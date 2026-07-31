package com.jp.flowpay.API.integration;

import com.jp.flowpay.API.entity.Agent;
import com.jp.flowpay.API.repository.AgentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
public class AgentRepositoryTest {

    @Autowired
    private AgentRepository agentRepository;

    @Test
    @DisplayName("Deve buscar atendente por ID corretamente")
    public void testFindById() {

        Optional<Agent> agent = agentRepository.findById(1L);

        assertTrue(agent.isPresent(), "O atendente de ID 1 deveria existir no banco");
        assertEquals("Cartões", agent.get().getName(), "O nome do atendente deve ser Cartões");
        assertEquals(1L, agent.get().getTeamId(), "O team_id deve ser 1");
    }
}