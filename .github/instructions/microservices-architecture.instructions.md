---
description: "Code review di microservizi: Database-per-Service, ownership e isolamento dei dati, Anemic Domain Model e invarianti di dominio."
applyTo: "**"
excludeAgent: "cloud-agent"
---

# Code review: architettura dei microservizi

Applica [la policy](microservices-architecture.md), in italiano, insieme a quella di retrocompatibilita. Non modificare codice.

## Metodo e segnalazioni

- In PR esamina diff e commit rispetto ai parent; segnala regressioni introdotte/aggravate. Il preesistente e contesto, salvo review completa richiesta. Dichiara il perimetro.
- Traccia il codice modificato fino a query, repository, migrazioni, config, permessi, chiamanti, mutazioni e test. Cerca ADR/ownership; non dedurre confini da nomi, package o numero di DB.
- Per Database-per-Service usa `data-stores.json` (stessa cartella): collezioni MongoDB e tabelle PostgreSQL per repository del progetto. Confronta i dati del diff con la voce di questo repository: dati solo di altri indicano accesso trasversale; condivisi vanno verificati; mancanti sono `NON-VERIFICABILE`. Indica uso, non ownership.
- Titolo `[MS-ID-REGOLA][STATO]`, es. `[MS-DB-ACCESS][VIOLAZIONE]`. ID validi: `[MS-DB-OWNERSHIP]`, `[MS-DB-ACCESS]`, `[MS-DB-INTEGRATION]`, `[MS-DOMAIN-MODEL]`, `[MS-DOMAIN-INVARIANTS]`, `[MS-EVIDENCE]`.
- `VIOLAZIONE`: evidenza concreta nel codice del diff e impatto dimostrato. `NON-VERIFICABILE`: rischio plausibile non provato; indica cosa manca. `PROVA-MANCANTE`: prova pre-rilascio richiesta ma assente. Le ultime due non sono violazioni.
- Indica gravita motivata, file/riga, percorso, evidenza, impatto e correzione minima. Ordina per gravita, senza duplicati; lo stile non e bug.
- Per lacune chiedi verifica circoscritta, responsabile per ruolo e azione pre-rilascio. Non inventare ownership, permessi, comportamenti o test. Un'eccezione documentata non e conformita.

## MS-DB-OWNERSHIP: proprietario unico

Verifica un proprietario unico per ogni tabella/collezione e relativo schema. Segnala scritture o migrazioni di servizi diversi sugli stessi dati, ownership ambigua introdotta dal diff e modelli ORM condivisi che vincolano schema e rilascio. Versioni/repliche dello stesso servizio non sono proprietari distinti.

## MS-DB-ACCESS: isolamento effettivo

Segnala accessi diretti, anche read-only, ai dati privati di altri servizi: SQL, ORM, viste, procedure, trigger, batch, credenziali. Verifica privilegi e ruolo di runtime; uno schema separato non basta con accesso trasversale. Cluster condiviso e identificativi uguali non provano violazioni. Non esporre segreti.

## MS-DB-INTEGRATION: contratti tra servizi

Segnala join, foreign key, transazioni e dipendenze da schemi privati tra servizi. Usa API/eventi del proprietario, composizione o proiezioni locali con contratto e consistenza espliciti. Distingui una proiezione autonoma da letture del DB altrui; valuta CDC, reporting e data platform secondo la policy. Non prescrivere saga/outbox senza requisito concreto.

## MS-DOMAIN-MODEL: comportamento nel dominio

Per domini con regole reali segui un caso d'uso fino a entita, value object, aggregati e policy. Segnala Anemic Domain Model solo con una regola concreta implementata altrove e oggetti ridotti a contenitori mutabili. Getter/setter, DTO, record, ORM, CQRS e CRUD semplici non bastano. Domain service stateless e Transaction Script motivati sono legittimi.

## MS-DOMAIN-INVARIANTS: mutazioni controllate

Verifica che creazione e transizioni proteggano le invarianti da ogni ingresso: consumer, batch, aggiornamenti diretti. Segnala setter/bypass che consentono stati invalidi o regole duplicate divergenti. L'application service orchestra I/O e transazioni; il dominio decide le regole, anche via policy/domain service. Non spostare I/O o coordinamento remoto nelle entita.

## MS-EVIDENCE: verifiche proporzionate

Richiedi, secondo il rischio, ownership/ADR, permessi e test di isolamento; per regole di dominio, test di stati validi/invalidi e percorsi alternativi. Distingui prove lette, eseguite e proposte; valuta coesistenza e rollout se cambiano confini o contratti. Non dichiarare conformita globale o test mancanti solo perche assenti dal diff.