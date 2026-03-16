# Decommission Runbook: Payment Instrument (PI) — Reward Calculator

Version: 1.0
Last updated: 2026-03-04

## Purpose
This document describes the plan to remove Payment Instrument (PI) related logic from the Reward Calculator service. It is written to be both human-readable and directly executable by an AI agent (step-by-step shell commands, search patterns, and patch actions) to automate the discovery, refactor, test, and rollout.

## Scope
- Remove all business logic, DTOs, connectors, and configuration specific to the Payment Instrument service from the Reward Calculator codebase.
- Update AsyncAPI and docs if Reward Calculator published or consumed PI-specific channels.
- Update tests, CI, and deployment manifests where PI integration is configured or mocked.

## Non-Goals
- Removing the PI microservice itself (out of scope).
- Changing unrelated reward calculation logic.

## High-level Strategy
1. Discover: find all references and usages of PI artifacts (class names, packages, config keys, asyncapi channels).  
2. Analyze: classify occurrences (compile-time types, runtime configuration, tests, docs).  
3. Isolate: add feature-flag or compile-time guards if needed for staged rollout.  
4. Remove: refactor code to remove PI-specific paths and replace with either noop or updated flows.  
5. Test: unit, integration, contract (AsyncAPI), and consumer-driven tests.  
6. Deploy: staged rollout, monitoring, rollback plan.  

## Risks & Mitigations
- Risk: removed logic breaks runtime flows — Mitigation: keep feature flag, run contract tests, smoke tests.  
- Risk: hidden references in tests/it infra — Mitigation: exhaustive repo scan and run full test suite.  

## Inventory & Discovery (Agent-executable)
Run these commands from repository root to locate PI usages. The AI agent should run them and collect results into a single file `pi-usage.txt`.

Commands:

```bash
# find Java classes and resources referencing "PaymentInstrument" or "payment-instrument"
rg --hidden --no-ignore -n "PaymentInstrument|payment-instrument|paymentinstrument|payment_instrument" || true > pi-usage.txt

# also search for PI client/connector artifactId or package fragments (adjust keywords if your code uses different naming)
rg --hidden --no-ignore -n "payment.instrument|paymentinstrument|pi-client|piConnector|PaymentInstrumentClient" || true >> pi-usage.txt

# Search AsyncAPI spec for PI channels
rg --hidden --no-ignore -n "PaymentInstrument|payment-instrument|pi-" specs/ || true >> pi-usage.txt

# Count unique files
cut -d: -f1 pi-usage.txt | sort -u | wc -l > pi-usage-count.txt
```

Save the results and review. The AI agent should summarize the top 20 files by occurrence and total hits.

## Classification Guide
For each discovered file, the agent should classify occurrences into one of:
- Compile-time artifact: DTO, interface, enum, repository, or package.  
- Runtime config: `application.yml`, feature flags, environment variables, bean definitions.  
- Integration: Kafka consumer/producer configs or AsyncAPI channels.  
- Test-only: mocks, test fixtures.  
- Docs/specs: `specs/asyncapi.yaml`, README, Helm values.

## Concrete Change Plan (step-by-step)
These steps are written so an AI agent can attempt them sequentially and open PRs with focused patches.

Prerequisites: create a working branch, e.g., `remove/pi-from-reward-calculator`.

1) PRE-FLIGHT: run full search and tests

```bash
git checkout -b remove/pi-from-reward-calculator
rg "PaymentInstrument|payment-instrument|PaymentInstrumentClient|pi-" -n > pi-usage.txt || true
./mvnw -q -DskipTests=false test || true    # run tests to see current baseline failures
```

2) FEATURE FLAG (optional, for staged rollout)
- If the code contains runtime toggles, add a temporary feature flag `feature.pi_integration_enabled` set to `false` by default. Wrap removal in the flag if a big-mid-rollout is required.

3) REMOVE / REFACTOR: follow the ordered categories below.

- DTOs & Domain Types
  - Find classes whose sole purpose is carrying PI data (names like `PaymentInstrument`, `Card`, `Instrument`) and remove or replace them with minimal, generic DTOs used elsewhere.
  - Example patch approach (agent): generate an apply_patch that deletes the class and updates imports in referencing files. Use the repo search results to enumerate dependent files first.

- Connector / Client
  - If a `PaymentInstrumentClient` or `payment-instrument` connector exists: remove the class, configuration beans, and autowired injections. Replace calls with the new default flow (e.g., no-op or use updated service if exists).

- Business Logic
  - In `Reward Calculator` service packages, remove code paths that branch on presence of PI or call PI-specific methods. Replace with default handling (e.g., treat instrument as absent). Maintain API compatibility where external contracts expect older payloads.

- Event Consumers/Producers
  - Update Kafka consumer configs. If Reward Calculator consumed PI-specific topics, disable those consumers and remove handler code. Update `specs/asyncapi.yaml` to remove PI channels.

- Tests
  - Remove or update unit/integration tests that mock PI. Replace with mocks of the new flow or generic mocks.

- Docs & Config
  - Update `specs/asyncapi.yaml`, Helm charts (`helm/values*.yaml`), and `application.yml` where PI configs appear.

4) EXAMPLE AGENT-PATCH WORKFLOW
 - Agent should prepare small, reviewable patches. Example sequence per file:

  - For each domain class to delete:
    - Create patch removing the file and updating all imports.
  - For each client/connector removal:
    - Remove bean definitions and constructor parameters consuming it; replace with an explicit empty implementation if interface is used elsewhere.

5) TESTING TO RUN AFTER EACH PATCH

 - Unit tests:
```bash
./mvnw -q -DskipITs=true -DskipTests=false test
```

 - Integration tests (if available, run with Testcontainers):
```bash
./mvnw -q verify -Pit || true
```

 - Contract tests / AsyncAPI validation:
```bash
# basic YAML lint + check for removed channels
rg "payment-instrument|PaymentInstrument|pi-" specs/ || true
yamllint specs/asyncapi.yaml || true
```

6) CI / Build
 - Update CI configs to remove PI environment variables or mocked services. Make sure the CI pipeline can run without starting PI.

7) Deployment & Rollout
 - Staged Canary: deploy to dev -> staging -> canary -> prod. After each step run health checks and smoke tests.
 - Health checks:
   - Application starts normally.
   - No consumer errors or retries related to missing PI topics.

8) Monitoring & Alerts
 - Add alerts for:
   - Error rates in Reward Calculator after change.  
   - Consumer lag or missing topic subscriptions.  
   - Unexpected exceptions in reward-calculation flows.

9) Rollback
 - Revert the PR(s) and redeploy previous release. Keep an explicit changelog entry of files modified to speed reverts.

## AsyncAPI & External Contracts
 - Search and update `specs/asyncapi.yaml`. If Reward Calculator previously consumed/produced PI-related channels, remove those sections and bump version.

Agent Commands:

```bash
rg "payment-instrument|PaymentInstrument|pi-" -n specs/ || true
# If channels exist, open and remove channel blocks referencing PI, then validate yaml
yamllint specs/asyncapi.yaml || true
```

## Example PR Checklist (to include in each PR description)
 - **Discovery file**: attach `pi-usage.txt` with top files affected.
 - **Small commits**: one logical change per commit.
 - **Tests**: relevant unit and integration tests pass.
 - **Docs**: `specs/asyncapi.yaml` updated and `helm/` values cleaned.
 - **CI**: pipeline green.
 - **Owner**: name of service owner and reviewers.

## Owner, Stakeholders, Timeline
 - Owner: Reward Calculator service lead (add GitHub handle).  
 - Stakeholders: PI service owner, platform-kafka, QA, DevOps.  
 - Suggested timeline: 1–2 sprints depending on breadth discovered.

## Agent Playbook (executable sequence)
1. Run discovery commands above and upload `pi-usage.txt` to PR.  
2. For each unique Java class file found, classify and propose a minimal apply_patch.  
3. Open a draft PR with a small first change: remove a single DTO or a single unused connector. Run unit tests.  
4. Iterate until all PI artifacts are removed.  
5. Update `specs/asyncapi.yaml` and Helm values, then run full CI.  

## Verification Commands (post-merge)

```bash
# run full test suite
./mvnw -q -DskipTests=false test

# sanity build
./mvnw -q -DskipTests=true package

# check AsyncAPI does not reference PI
rg "payment-instrument|PaymentInstrument|pi-" -n || true
```

## Appendix — Sample grep patterns to discover likely names
 - PaymentInstrument
 - payment-instrument
 - PaymentInstrumentClient
 - paymentInstrumentConnector
 - pi-client
 - pi-service

---
If you want, I can: run the discovery commands and produce `pi-usage.txt`, or create the first small PR removing one DTO/connector and run the test suite. Which should I do next?
