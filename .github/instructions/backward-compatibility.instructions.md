---
applyTo: "**"
excludeAgent: "cloud-agent"
---

# Code review: retrocompatibilita

Review in italiano secondo [la policy](backward-compatibility.md): diff PR e commit disponibili rispetto ai parent. Non attestare commit non esaminati. Non modificare codice.

## Metodo e segnalazioni

- Leggi la policy; verifica codice, contratti, migrazioni, configurazioni, test e implementazioni collegate. Header e flag non provano conformita.
- Segnala solo problemi introdotti/aggravati dal diff; refactoring a contratti invariati non richiedono versioni.
- Titolo con etichetta esatta della policy OBBLIGATORIO per ogni segnalazione, es. `[BC-REST]`, `[BC-DB]`, `[BC-MSG]`, anche per aspetti non verificabili e prove mancanti.
- Indica file, riga pertinente, evidenza, impatto e correzione minima. Ordina per gravita senza duplicati.
- Distingui violazioni, aspetti non verificabili e prove obbligatorie mancanti. Non inventare esiti/comportamenti ne attestare conformita senza prove. Breaking change approvate restano incompatibili.

## BC-SCOPE: perimetro esterno

Verifica inventario e impatto su ingressi/uscite, webhook, messaggi, dati, file, batch, configurazioni, permessi e segnali operativi. Considera contratti impliciti, dipendenze e infrastruttura. Esplicita consumatori non identificati.

## BC-BEHAVIOR: semantica e operativita

Verifica significato e unita dei dati, default, NULL, validazioni, errori, ordine, paginazione, auth, idempotenza, transazioni, effetti collaterali e garanzie di messaggistica. Controlla timeout e obiettivi operativi concordati e compatibilita con downstream non aggiornati.

## BC-REST: interfacce REST

Modifiche strutturali/semantiche richiedono versione via header HTTP (es. `X-API-Version`) che selezioni la logica. Path/query/body non bastano. Preserva versioni supportate e comportamento senza header; verifica versioni sconosciute, specifiche, gateway, proxy e cache. Header di versione esemplificativi.

## BC-DB: schema DB

Retrocompatibilita: solo nuovi campi NULLABLE, non rimozioni, rinomine o cambi di tipo; un default non giustifica NOT NULL. Verifica campi assenti, SQL posizionale, SELECT *, mapping, validatori, vincoli, viste, trigger, indici e permessi. Controlla lock, backfill e leggibilita/scrivibilita dei dati per versioni coesistenti e di rollback.

## BC-MSG: Kafka e Azure Service Bus

Modifiche strutturali/semantiche richiedono versione nei metadati (es. `X-Msg-Version`), scritta dal producer e letta dal consumer. Verifica messaggi precedenti, header assente/sconosciuto, backlog, retry, dead-letter e replay. Producer nuovi devono restare compatibili con consumer vecchi: verifica formato/routing e ordine di aggiornamento. Controlla partizionamento, TTL, retention e duplicazione degli effetti.

## BC-FEATURES: logiche on-demand

Quando richiesto dalla policy, verifica `X-Features` multi-valore in tutti gli ingressi REST/Kafka/Service Bus e propagazione downstream HTTP/messaggi. Controlla sintassi, valori e combinazioni, assenza dell'header, autorizzazione, confini di fiducia e supporto downstream. Le varianti e i test in produzione non devono compromettere sistemi esterni.

## BC-FLAGS: feature flag

Facoltativi, disattivi di default anche in configurazioni e fallback. Verifica varianti, combinazioni e attivazione coordinata. Disattivarli non annulla dati o messaggi prodotti. Non sostituiscono versionamento o X-Features obbligatori.

## BC-RELEASE: distribuzione e dismissione

Verifica versioni ammesse, vincoli contro combinazioni vietate e rollout/rollback. Dismissioni richiedono migrazione coordinata, consenso e criteri verificabili, inclusi backlog/replay. Ogni stato rilasciabile deve essere conforme.

## BC-EVIDENCE: prove di conformita

Verifica impatto PR e prove: regressione, contratti vecchi/nuovi, integrazione, varianti, coesistenza, migrazioni, rollback e limiti operativi. Distingui test eseguiti/proposti. Per lacune indica prova mancante, responsabile e azione pre-rilascio. Review non sostitutiva di test e controlli di rilascio.