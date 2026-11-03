---
applyTo: "**"
excludeAgent: "cloud-agent"
---

# Code review: retrocompatibilita

Review in italiano secondo [la policy](backward-compatibility.md): diff e commit rispetto ai parent. Non attestare commit non esaminati. Non modificare codice.

## Metodo e segnalazioni

- Leggi la policy; verifica codice, contratti, migrazioni, config, test e chiamanti. Header e flag non provano conformita.
- Segnala solo problemi introdotti/aggravati dal diff; refactoring a contratti invariati non richiedono versioni.
- Titolo nel formato `[BC-CATEGORIA][STATO]`, con categoria della policy: `[BC-REST][VIOLAZIONE]`, `[BC-SCOPE][NON-VERIFICABILE]` o `[BC-EVIDENCE][PROVA-MANCANTE]`.
- `VIOLAZIONE`: evidenza concreta nel diff e impatto su un contratto/consumatore supportato. `NON-VERIFICABILE`: rischio plausibile non dimostrabile; indica cosa manca. `PROVA-MANCANTE`: prova richiesta pre-rilascio assente, non incompatibilita accertata.
- Indica file, riga, evidenza, impatto e correzione minima; ordina per gravita senza duplicati.
- Non inventare esiti ne attestare conformita senza prove. Breaking change approvate restano incompatibili.

## BC-SCOPE: perimetro esterno

Verifica ingressi/uscite, webhook, messaggi, dati, file, batch, config, permessi e segnali operativi, incluse dipendenze e infrastruttura. Esplicita consumatori non identificati.

## BC-BEHAVIOR: semantica e operativita

Verifica significato/unita dei dati, default, NULL, validazioni, errori, ordine, paginazione, auth, idempotenza, transazioni, effetti e garanzie di messaggistica. Controlla timeout, obiettivi operativi e downstream non aggiornati.

## BC-REST: interfacce REST

Modifiche strutturali/semantiche richiedono un header HTTP di versione (es. `X-API-Version`) che selezioni la logica; path/query/body non bastano. Preserva versioni supportate e comportamento senza header; verifica versioni sconosciute, specifiche, gateway, proxy e cache.

## BC-DB: schema DB

Solo nuovi campi NULLABLE: non rimuovere/rinominare campi o cambiarne tipo; un default non giustifica NOT NULL. Verifica campi assenti, SQL posizionale, SELECT *, mapping, validatori, vincoli, viste, trigger, indici, permessi, lock, backfill e rollback.

## BC-MSG: Kafka e Azure Service Bus

Modifiche strutturali/semantiche richiedono versione nei metadati (es. `X-Msg-Version`), scritta dal producer e letta dal consumer. Verifica versioni precedenti, header assente/sconosciuto, backlog, retry, dead-letter, replay, compatibilita producer/consumer, routing, ordine, partizionamento, TTL, retention e duplicati.

## BC-FEATURES: logiche on-demand

Quando richiesto, verifica `X-Features` multi-valore in tutti gli ingressi REST/Kafka/Service Bus e la propagazione downstream. Controlla sintassi, valori, combinazioni, assenza, autorizzazione, confini di fiducia e supporto downstream; le varianti non devono compromettere sistemi esterni.

## BC-FLAGS: feature flag

Facoltativi e disattivi di default anche in config/fallback. Verifica varianti, combinazioni e attivazione coordinata. Disattivarli non annulla effetti; non sostituiscono versionamento o `X-Features`.

## BC-RELEASE: distribuzione e dismissione

Verifica versioni ammesse, combinazioni vietate e rollout/rollback. Dismissioni richiedono migrazione coordinata, consenso e criteri verificabili, inclusi backlog/replay. Ogni stato rilasciabile deve essere conforme.

## BC-EVIDENCE: prove di conformita

Verifica prove di regressione, contratti, integrazione, varianti, coesistenza, migrazione, rollback e limiti operativi. Distingui test eseguiti/proposti. Per lacune indica prova, responsabile e azione pre-rilascio. La review non sostituisce test o controlli di rilascio.