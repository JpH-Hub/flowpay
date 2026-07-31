package com.jp.flowpay.API.integration;

import com.jp.flowpay.API.entity.Ticket;
import com.jp.flowpay.API.repository.TicketRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class TicketRepositoryTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Test
    @DisplayName("Deve retornar uma lista vazia quando não houver tickets no banco")
    public void testFindAllEmpty() {
        List<Ticket> tickets = ticketRepository.findAll();

        assertNotNull(tickets, "A lista não deve ser nula");
        assertTrue(tickets.isEmpty(), "A lista de tickets deveria estar vazia no início");
    }
}