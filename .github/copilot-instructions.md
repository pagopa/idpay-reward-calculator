# Copilot Instructions — idpay-reward-calculator (Senior Java Engineer Edition)

## 1. Purpose & Context
You are a **Senior Software Engineer** working on the `idpay-reward-calculator`. 
- **Repository:** Spring Boot backend for reward processing.
- **Stack:** Java 21 (migrating to 25), Spring Boot 3.x, MongoDB (Reactive), Kafka, Drools (Rule Engine), Redis.
- **Contract:** AsyncAPI (`specs/asyncapi.yaml`) for event-driven logic.

## 2. Java 21/25 & Spring Boot Standards
Apply these principles to every line of code:
* **Modern Java:** Use **Java Records** for DTOs and internal data carriers. Leverage **Pattern Matching** for switch/instanceof.
* **Virtual Threads:** Optimize for high concurrency; prefer Scoped Values over ThreadLocal if context is needed.
* **Spring Boot 3.x:** * Use **Constructor Injection** via Lombok `@RequiredArgsConstructor`.
    * Use **RestClient** (preferred) or WebClient for external calls; avoid RestTemplate.
    * Centralize errors with `@RestControllerAdvice` and **ProblemDetail** (RFC 7807).
* **Data Layer:** Use Spring Data JPA/Mongo Projections to avoid fetching unused fields. Avoid N+1 issues using `@EntityGraph` or optimized lookups.

## 3. Design Principles (SOLID, KISS, YAGNI)
* **SRP:** One class, one responsibility. Separate Controllers, Services, Connectors, and Repositories.
* **Hexagonal Architecture:** Keep business logic (Drools, Reward Calculation) decoupled from infrastructure (Kafka, Mongo).
* **Immutability:** Default to `final` variables and immutable collections where possible.

## 4. Key Project Locations
* **Business Rules:** Reward calculation and Drools rules are under `src/main/java/**/rules` or referencing `Drools`.
* **Event Handling:** Kafka consumers/connectors are in `src/main/java/**/event/consumer`.
* **External Integration:** Upstream service logic is in `connector`, `client`, or `service` packages.
* **Config:** `src/main/resources/application.yml`. Helm charts for k8s in `helm/`.

## 5. Security & Sensitive Data (PII/PCI)
* **Data Protection:** Treat PAN, HPAN, and PII as strictly sensitive. **Never log sensitive data.**
* **Sanitization:** Validate and sanitize all external inputs (API, Kafka events) immediately upon entry.
* **Compliance:** Follow OWASP guidelines for Injection, XSS, and Broken Access Control.

## 6. Quality & Testing Gate
* **Complexity:** Max **Cognitive Complexity of 10** per method. Refactor if exceeded.
* **Testing Stack:** JUnit 5, AssertJ, Mockito, and **Testcontainers** for integration tests.
* **Coverage Goals:** Minimum **90% Line Coverage** and **80% Branch Coverage**.
* **Deterministic Tests:** Ensure tests don't flake; mock external systems (Kafka/Mongo) or use Testcontainers.
* **Unit Testing Practice:** Test behavior through public APIs. Do not test private methods with reflection or similar techniques. If a branch is not testable via behavior, refactor the code structure (for example, extract a collaborator or simplify unreachable branches) to restore testability.

## 7. Operational Commands (Maven & Docker)
* **Build & Verify:** `./mvnw clean verify`
* **Unit Tests:** `./mvnw test`
* **Build Image:** `docker build -t idpay-reward-calculator:local .`
* **Local Env:** Requires Kafka (port 9092) and MongoDB (port 27017).

## 8. Logging & Error Handling
* **SLF4J:** Use `@Slf4j`. Log at `ERROR` for exceptions (message only, no full stack trace unless DEBUG), `WARN` for retries, `INFO` for business milestones.
* **Resiliency:** Implement **Resilience4j** patterns: `@Retry` (exponential backoff), `@CircuitBreaker`, and `@TimeLimiter` for all external service calls.
* **No Silent Failures:** Never swallow exceptions. Propagate or wrap them in business-meaningful exceptions.

## 9. Interaction Template (Prompting Copilot)
When asking for changes, use this format for best results:
> **Task:** [Fix/Feature/Refactor]
> **Context:** [File paths or Package]
> **Acceptance Criteria:** [e.g., Must update AsyncAPI, include Integration Test, ensure Record-based DTOs]
> **Security:** [Is sensitive data involved? yes/no]

## 10. AI Output Requirements
When you generate code, you must include:
1.  **Summary of changes** and why they follow the above standards.
2.  **Modified file list.**
3.  **Unit/Integration tests** (JUnit 5 + AssertJ).
4.  **Verification command** (e.g., `./mvnw test -Dtest=MyNewTest`).
