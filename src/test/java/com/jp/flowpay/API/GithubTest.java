package com.jp.flowpay.API;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GithubTest {
    @Test
    @DisplayName("Teste de validação do ambiente do GitHub Actions")
    void testGitHubActionsPipeline() {
        // Valida uma operação simples
        int resultado = 2 + 2;
        assertEquals(4, resultado, "A soma básica deve funcionar no CI");

        // Valida se o teste está rodando corretamente
        assertTrue(true, "O ambiente de teste deve responder como verdadeiro");
    }

}
