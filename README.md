# Onboarding Chain Workflow with Spring AI

This project demonstrates the **Chain Workflow pattern** using Spring AI, implementing an intelligent onboarding assistant that processes user queries through a series of specialized steps.

## What is the Chain Workflow Pattern?

The Chain Workflow pattern is an agentic AI pattern that breaks down complex AI tasks into a sequence of specialized, composable steps. Each step has a single responsibility and can be independently tested, monitored, and optimized. The pattern promotes:

- **Modularity**: Each step is a separate component with a clear interface
- **Observability**: Individual step performance can be monitored
- **Resilience**: Steps can have independent error handling and retry logic
- **Flexibility**: Steps can be easily reordered, added, or removed
- **Testability**: Each step can be unit tested in isolation

For more details on agentic patterns, see the [Spring AI documentation](https://spring.io/blog/2025/01/21/spring-ai-agentic-patterns).

## Architecture Overview

This implementation creates an onboarding assistant that processes user queries through a 5-step chain:

```
User Query → Intent Classification → Retrieval → Answer Drafting → Validation → Persistence
```

### Step 1: Intent Classification (`IntentStep`)
- Classifies user input into predefined categories (ONBOARDING_IT, ONBOARDING_HR, BENEFITS, etc.)
- Uses LLM with a structured prompt to ensure consistent labeling
- Enables routing to appropriate knowledge domains

### Step 2: Retrieval (`RetrieveStep`)
- Searches vector database for relevant passages using the classified intent and user query
- Uses pgvector with similarity search to find top-k relevant documents
- Provides grounding context for answer generation

### Step 3: Answer Drafting (`DraftAnswerStep`)
- Generates a grounded answer using ONLY the retrieved passages
- Prevents hallucinations by constraining the LLM to provided context
- Includes structured formatting with references

### Step 4: Validation (`ValidateStep`)
- Validates the generated answer for compliance and structure
- Ensures required elements are present (references, format, etc.)
- Placeholder implementation for extensibility

### Step 5: Persistence (`PersistStep`)
- Stores the interaction for auditing and analytics
- Uses reactive database operations with R2DBC
- Enables conversation history and performance tracking

## Key Components

### Core Chain Infrastructure

- **`Step` Interface**: Defines the contract for all chain steps
- **`Ctx` (Context)**: Immutable data structure that flows through the chain
- **`ChainWorkflowOrchestratorService` Interface**: Defines the contract for the orchestrator service
- **`OnboardingChainOrchestratorService`**: Orchestrator service implementation that coordinates the execution of all steps

### Context Flow

The `Ctx` record carries data through the chain:

```java
public record Ctx(
    String userText,           // Original user input
    String intent,             // Classified intent (added by IntentStep)
    List<String> passages,     // Retrieved passages (added by RetrieveStep)
    String draftAnswer,        // Generated answer (added by DraftAnswerStep)
    Map<String,Object> meta    // Metadata for tracing, costs, etc.
) {}
```

### Reactive Design

The entire chain is built using Project Reactor for:
- **Non-blocking I/O**: Efficient resource utilization
- **Backpressure handling**: Prevents overwhelming downstream systems
- **Error propagation**: Graceful failure handling across the chain
- **Timeout management**: Prevents hanging operations

## Technology Stack

- **Spring Boot 3.5.5** with WebFlux (reactive)
- **Spring AI 1.0.1** for LLM integration and vector search
- **PostgreSQL** with pgvector extension for vector storage
- **R2DBC** for reactive database access
- **Flyway** for database migrations
- **Docker Compose** for local development

## Getting Started

### Prerequisites

- Java 24+
- Docker and Docker Compose
- OpenAI API key

### Setup

1. **Clone and navigate to the project**:
   ```bash
   cd onboarding-chain-workflow-spring-ai
   ```

2. **Start the database**:
   ```bash
   docker-compose up -d
   ```

3. **Configure your OpenAI API key** in `application.properties`:
   ```properties
   spring.ai.openai.api-key=your-api-key-here
   ```

4. **Run the application**:
   ```bash
   ./mvnw spring-boot:run
   ```

### API Endpoints

#### Main Onboarding Chain
```bash
POST /onboarding/ask
Content-Type: application/json

{
  "text": "How do I set up my laptop for work?"
}
```

Response:
```json
{
  "intent": "ONBOARDING_IT",
  "answer": "• Contact IT support at ext. 1234\n• Provide your employee ID\n• Schedule a setup appointment\n\nReferences:\n[1] IT Setup Process\n[2] Employee Onboarding Guide"
}
```

#### Testing Endpoints
```bash
# Test intent classification
GET /test/intent?text=How do I set up my laptop?

# Test retrieval
GET /test/retrieve?text=How do I set up my laptop?

# Health check
GET /test/health
```

## Chain Workflow Benefits

### 1. **Separation of Concerns**
Each step has a single responsibility, making the code easier to understand and maintain.

### 2. **Independent Scaling**
Steps can be scaled independently based on their resource requirements.

### 3. **Enhanced Observability**
Each step can be monitored separately, providing detailed insights into performance bottlenecks.

### 4. **Flexible Error Handling**
Different steps can have different retry strategies and error recovery mechanisms.

### 5. **Easy Testing**
Each step can be unit tested in isolation with mocked dependencies.

### 6. **A/B Testing Support**
Different implementations of steps can be easily swapped for experimentation.

## Extending the Chain

### Adding New Steps

1. Implement the `Step` interface:
   ```java
   @Component
   public class NewStep implements Step {
       @Override
       public Mono<Ctx> apply(Ctx ctx) {
           // Your step logic here
           return Mono.just(ctx.withNewField(value));
       }
   }
   ```

2. Add the step to the orchestrator:
   ```java
   this.steps = List.of(s1, s2, s3, newStep, s4, s5);
   ```

### Modifying Context

Add new fields to the `Ctx` record and corresponding helper methods:
```java
public record Ctx(
    String userText,
    String intent,
    List<String> passages,
    String draftAnswer,
    String newField,  // New field
    Map<String,Object> meta
) {
    public Ctx withNewField(String value) {
        return new Ctx(userText, intent, passages, draftAnswer, value, meta);
    }
}
```

## Production Considerations

### Monitoring
- Add metrics for each step (latency, success rate, error rates)
- Implement distributed tracing with tools like Jaeger
- Monitor vector search performance and relevance scores

### Security
- Validate and sanitize all user inputs
- Implement rate limiting
- Add authentication and authorization
- Audit all AI interactions

### Performance
- Cache frequently accessed vector embeddings
- Implement connection pooling for database operations
- Consider async processing for non-critical steps

### Reliability
- Add circuit breakers for external service calls
- Implement dead letter queues for failed operations
- Add health checks for all dependencies

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.
