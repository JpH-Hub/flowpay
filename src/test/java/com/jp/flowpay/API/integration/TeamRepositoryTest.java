package com.jp.flowpay.API.integration;

import com.jp.flowpay.API.entity.Team;
import com.jp.flowpay.API.repository.TeamRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;


import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest
@ActiveProfiles("test")
public class TeamRepositoryTest {

    @Autowired
    private TeamRepository teamRepository;

    @Test
    @DisplayName("Deve encontrar o time Cartões ignorando maiúsculas e minúsculas")
    public void testFindByNameIgnoreCase() {
        Optional<Team> team = teamRepository.findByNameIgnoreCase("cArTõEs");

        assertTrue(team.isPresent(), "Deveria encontrar o time no banco de dados");
        assertEquals("Cartões", team.get().getName(), "O nome do time deve bater");
    }
}
