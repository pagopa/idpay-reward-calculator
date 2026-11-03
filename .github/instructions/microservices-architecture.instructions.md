---
description: "Code review di microservizi: Database-per-Service, ownership e isolamento dei dati, Anemic Domain Model e invarianti di dominio."
applyTo: "**"
excludeAgent: "cloud-agent"
---

# Code review: architettura dei microservizi

Applica [la policy](microservices-architecture.md), in italiano. Non modificare codice. Queste regole affiancano quelle di retrocompatibilita.

## Metodo e segnalazioni

- In PR esamina diff e commit disponibili rispetto ai parent; segnala regressioni introdotte/aggravate. Il preesistente e contesto, salvo review completa esplicitamente richiesta. Dichiara il perimetro.
- Traccia il codice modificato fino a query, repository, migrazioni, config, permessi, chiamanti, mutazioni e test pertinenti. Cerca ADR/ownership; non dedurre confini da nomi, package o numero di DB.
- Titolo nel formato `[MS-ID-REGOLA][STATO]`, es. `[MS-DB-ACCESS][VIOLAZIONE]`. ID validi: `[MS-DB-OWNERSHIP]`, `[MS-DB-ACCESS]`, `[MS-DB-INTEGRATION]`, `[MS-DOMAIN-MODEL]`, `[MS-DOMAIN-INVARIANTS]`, `[MS-EVIDENCE]`.
- `VIOLAZIONE`: evidenza concreta nel diff e impatto dimostrato. `NON-VERIFICABILE`: rischio plausibile non provato; indica cosa manca. `PROVA-MANCANTE`: prova pre-rilascio richiesta ma assente. Le ultime due non sono violazioni accertate.
- Indica gravita motivata, file/riga, percorso, evidenza, impatto e correzione minima. Ordina per gravita, senza duplicati; preferenze stilistiche non sono bug.
- Per lacune chiedi verifica circoscritta, responsabile per ruolo e azione pre-rilascio. Non inventare ownership, permessi, comportamenti o test. Un'eccezione documentata non equivale a conformita.

## MS-DB-OWNERSHIP: proprietario unico

Verifica un servizio proprietario per ogni tabella/collezione e relativo schema. Segnala scritture o migrazioni di servizi diversi sugli stessi dati, ownership ambigua introdotta dal diff e modelli ORM condivisi che vincolano schema e rilascio. Versioni/repliche dello stesso servizio non sono proprietari distinti.

## MS-DB-ACCESS: isolamento effettivo

Segnala accessi diretti, anche read-only, ai dati privati di altri servizi: SQL, ORM, viste, procedure, trigger, batch e credenziali. Verifica privilegi e ruolo di runtime; uno schema separato non basta con accesso trasversale. Server/cluster condiviso e identificativi uguali non provano una violazione. Non esporre segreti.

## MS-DB-INTEGRATION: contratti tra servizi

Segnala join, foreign key, transazioni e dipendenze da schemi privati che attraversano servizi. Usa API/eventi del proprietario, composizione o proiezioni locali con contratto e consistenza espliciti. Distingui una proiezione autonoma da letture del DB altrui; valuta CDC, reporting e data platform rispetto alle condizioni della policy. Non prescrivere saga/outbox senza un requisito concreto.

## MS-DOMAIN-MODEL: comportamento nel dominio

Per domini con regole reali segui un caso d'uso fino a entita, value object, aggregati e policy. Segnala Anemic Domain Model solo con una regola concreta implementata esternamente e oggetti ridotti a contenitori mutabili. Getter/setter, DTO, record, ORM, CQRS e servizi CRUD semplici non bastano. Domain service stateless e Transaction Script motivati possono essere legittimi.

## MS-DOMAIN-INVARIANTS: mutazioni controllate

Verifica che creazione e transizioni proteggano le invarianti da tutti gli ingressi pertinenti, inclusi consumer, batch e aggiornamenti diretti. Segnala setter/bypass che consentono stati invalidi o regole duplicate divergenti. L'application service orchestra I/O e transazioni; il dominio decide le regole, anche tramite policy/domain service. Non spostare I/O o coordinamento remoto nelle entita.

## MS-EVIDENCE: verifiche proporzionate

Richiedi, per il rischio introdotto, ownership/ADR, permessi e test di isolamento; per regole di dominio, test di stati validi/invalidi e percorsi alternativi. Distingui prove lette, eseguite e proposte; valuta coesistenza e rollout quando cambiano confini o contratti. Non dichiarare conformita globale o mancati test solo perche non presenti nel diff.