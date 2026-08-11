package com.jp.flowpay.API.unit;

import com.jp.flowpay.API.enums.TeamEnum;
import com.jp.flowpay.API.service.TeamRoutingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TeamRoutingServiceTest {

    private TeamRoutingService teamRoutingService;

    @BeforeEach
    void setUp() {
        teamRoutingService = new TeamRoutingService();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Problema com meu Cartão de Crédito",
            "Dúvida sobre cartao",
            "CARTAO bloqueado"
    })
    @DisplayName("Deve rotear para Cartões quando o assunto contém palavra-chave de cartão")
    void shouldRouteToCreditCards(String subject) {
        assertEquals(TeamEnum.CREDIT_CARDS, teamRoutingService.determineTeam(subject));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Dúvida sobre empréstimo pessoal",
            "Solicitar emprestimo",
            "EMPRESTIMO atrasado"
    })
    @DisplayName("Deve rotear para Empréstimos quando o assunto contém palavra-chave de empréstimo")
    void shouldRouteToLoans(String subject) {
        assertEquals(TeamEnum.LOANS, teamRoutingService.determineTeam(subject));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Dúvida genérica",
            "Problema com extrato",
            "Outros assuntos diversos"
    })
    @DisplayName("Deve rotear para Outros Assuntos quando não houver palavra-chave")
    void shouldRouteToOthers(String subject) {
        assertEquals(TeamEnum.OTHERS, teamRoutingService.determineTeam(subject));
    }

    @Test
    @DisplayName("Deve rotear para Outros Assuntos quando subject for nulo")
    void shouldRouteToOthersWhenNull() {
        assertEquals(TeamEnum.OTHERS, teamRoutingService.determineTeam(null));
    }

    @Test
    @DisplayName("Deve rotear para Outros Assuntos quando subject for em branco")
    void shouldRouteToOthersWhenBlank() {
        assertEquals(TeamEnum.OTHERS, teamRoutingService.determineTeam("   "));
    }
}
