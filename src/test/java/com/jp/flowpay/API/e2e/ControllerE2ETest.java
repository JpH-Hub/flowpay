package com.jp.flowpay.API.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jp.flowpay.API.dto.ticketDTO.request.CreateTicketRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class ControllerE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateTicketSuccessfullyInRealEnvironment() throws Exception {
        CreateTicketRequestDTO request = new CreateTicketRequestDTO();
        request.setConversationRef("");
        request.setSubject("Problema com meu Cartão de Crédito");

        mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void shouldReturn400WhenPayloadIsInvalid() throws Exception {
        CreateTicketRequestDTO invalidRequest = new CreateTicketRequestDTO();

        mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldCloseTicketSuccessfully() throws Exception {
        CreateTicketRequestDTO request = new CreateTicketRequestDTO();
        request.setConversationRef("WHATS-CLOSE-1");
        request.setSubject("Dúvida de Extrato");

        String responseContent = mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long ticketId = objectMapper.readTree(responseContent).get("id").asLong();

        mockMvc.perform(patch("/tickets/" + ticketId + "/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void shouldReturn404WhenClosingNonExistingTicket() throws Exception {
        Long invalidTicketId = 999999L;

        mockMvc.perform(patch("/tickets/" + invalidTicketId + "/close"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn409WhenConversationRefIsDuplicated() throws Exception {
        CreateTicketRequestDTO request = new CreateTicketRequestDTO();
        request.setConversationRef("WHATS-DUP-001");
        request.setSubject("Dúvida genérica");

        mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("conversationRef already exists"));
    }

    @Test
    void shouldReturn400WhenClosingRejectedTicket() throws Exception {
        CreateTicketRequestDTO request = new CreateTicketRequestDTO();
        request.setConversationRef("WHATS-REJECT-CLOSE");
        request.setSubject("Dúvida genérica");

        for (int i = 0; i < 12; i++) {
            CreateTicketRequestDTO fillRequest = new CreateTicketRequestDTO();
            fillRequest.setConversationRef("WHATS-FILL-" + i);
            fillRequest.setSubject("Dúvida genérica " + i);

            mockMvc.perform(post("/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(fillRequest)))
                    .andExpect(status().isCreated());
        }

        String responseContent = mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andReturn().getResponse().getContentAsString();

        Long ticketId = objectMapper.readTree(responseContent).get("id").asLong();

        mockMvc.perform(patch("/tickets/" + ticketId + "/close"))
                .andExpect(status().isBadRequest());
    }
}
