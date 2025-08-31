package com.hrpd.onboarding.config;

import com.hrpd.onboarding.chain.Step;
import com.hrpd.onboarding.chain.orchestrator.ChainWorkflowOrchestratorService;
import com.hrpd.onboarding.chain.orchestrator.OnboardingChainOrchestratorService;
import com.hrpd.onboarding.chain.steps.*;
import com.hrpd.onboarding.persistence.TicketRepository.TicketRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.core.DatabaseClient;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class ChainWorkflowConfig {

    @Bean
    public ChainWorkflowOrchestratorService chainOrchestratorService(
                        IntentStep intentStep,
                        RetrieveStep retrieveStep,
                        DraftAnswerStep draftAnswerStep,
                        ValidateStep validateStep,
                        PersistStep persistStep) {
        return new OnboardingChainOrchestratorService(
                intentStep,
                retrieveStep,
                draftAnswerStep,
                validateStep,
                persistStep);
    }

    @Bean
    public IntentStep intentStep(ChatModel chatModel) {
        return new IntentStep(chatModel);
    }

    @Bean
    public RetrieveStep retrieveStep(VectorStore vectorStore) {
        return new RetrieveStep(vectorStore);
    }

    @Bean
    public DraftAnswerStep draftAnswerStep(ChatModel chatModel) {
        return new DraftAnswerStep(chatModel);
    }

    @Bean
    public ValidateStep validateStep() {
        return new ValidateStep();
    }

    @Bean
    public PersistStep persistStep(TicketRepository ticketRepository) {
        return new PersistStep(ticketRepository);
    }

    @Bean
    public TicketRepository ticketRepository(DatabaseClient databaseClient, ObjectMapper objectMapper) {
        return new TicketRepository(databaseClient, objectMapper);
    }
}
