package com.jp.flowpay.API.service;

import com.jp.flowpay.API.enums.TeamEnum;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.regex.Pattern;

@Service
public class TeamRoutingService {

    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    public TeamEnum determineTeam(String subject) {
        if (subject == null || subject.isBlank()) {
            return TeamEnum.OTHERS;
        }

        String normalizedSubject = normalizeText(subject);

        if (normalizedSubject.contains("cartao")) {
            return TeamEnum.CREDIT_CARDS;
        }

        if (normalizedSubject.contains("emprestimo")) {
            return TeamEnum.LOANS;
        }

        return TeamEnum.OTHERS;
    }

    private String normalizeText(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        String textWithoutAccents = DIACRITICS_PATTERN.matcher(normalized).replaceAll("");
        return textWithoutAccents.toLowerCase().trim();
    }
}
