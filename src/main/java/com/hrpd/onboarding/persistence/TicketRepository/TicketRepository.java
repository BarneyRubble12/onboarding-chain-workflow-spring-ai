package com.hrpd.onboarding.persistence.TicketRepository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public class TicketRepository {

    private final DatabaseClient dbClient;
    private final ObjectMapper objectMapper;

    public TicketRepository(DatabaseClient dbClient, ObjectMapper objectMapper) {
        this.dbClient = dbClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Persists a single draft entry (question, intent, draft answer, passages).
     *
     * @param userText   original query
     * @param intent     classified intent
     * @param draft      grounded draft answer
     * @param passages   retrieved passages (top-k)
     * @return Mono that completes when the row has been inserted
     */
    public Mono<Void> saveDraft(String userText, String intent, String draft, List<String> passages) {
        var passagesJson = "";

        try {
            passagesJson = objectMapper.writeValueAsString(passages);
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }

        return dbClient
                .sql("""
                INSERT INTO drafts (user_text, intent, draft_answer, passages)
                VALUES ($1, $2, $3, CAST($4 AS JSONB))
                """)
                .bind("$1", userText)
                .bind("$2", intent)
                .bind("$3", draft)
                .bind("$4", passagesJson)
                .fetch()
                .rowsUpdated()
                .then();
    }
}
