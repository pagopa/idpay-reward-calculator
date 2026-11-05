# Linee guida per Database-per-Service e modello di dominio

## Principi e ambito

Questa policy guida la review dell'architettura di applicazioni a microservizi, con due obiettivi: preservare la proprieta esclusiva dei dati e collocare le regole di business nel modello di dominio quando la complessita lo richiede.

**DEVE**, **NON DEVE** e **DEVONO** indicano obblighi; **PUO** indica una scelta. Le regole riguardano comportamento e dipendenze reali, non nomi delle classi, annotazioni, framework o numero di database. Si applicano anche a database documentali, stored procedure, processi schedulati e componenti di integrazione.

Database-per-Service richiede dati persistenti privati del servizio proprietario, accessibili dagli altri servizi tramite contratti espliciti. Non richiede necessariamente un server fisico per servizio. Un Rich Domain Model associa stato e comportamento di business, proteggendo invarianti e transizioni; non richiede che ogni operazione risieda in un'entita.

In PR la review **DEVE** segnalare solo problemi introdotti o aggravati dal diff. Il codice preesistente serve come contesto, non come elenco di debito tecnico da imputare alla PR. In una review completa richiesta esplicitamente **SI POSSONO** segnalare problemi preesistenti nel perimetro concordato. I commit disponibili **DEVONO** essere confrontati con i rispettivi parent; **NON SI DEVONO** attestare commit, repository o ambienti non esaminati.

Questa policy affianca quelle di retrocompatibilita, sicurezza e operativita. Una correzione architetturale **NON DEVE** introdurre una migrazione incompatibile senza un piano esplicito.

## Inventario dei data store

Il file `data-stores.json`, nella stessa cartella di questa policy, elenca per ogni repository del progetto di appartenenza le collezioni MongoDB (`mongodb_collections`, formato `database.collezione`) e le tabelle PostgreSQL (`postgresql_tables`, formato `database.schema.tabella`) che vi sono adoperate. E sincronizzato dal repository centrale delle policy e **NON DEVE** essere modificato localmente.

La verifica del pattern Database-per-Service (`MS-DB-OWNERSHIP`, `MS-DB-ACCESS`, `MS-DB-INTEGRATION`) **DEVE** usare l'inventario come fonte primaria per individuare i dati usati dagli altri servizi:

- Individuare la voce del repository in esame tramite il campo `repository` (`owner/nome`) e confrontare con essa le collezioni e le tabelle lette, scritte, migrate o referenziate dal codice modificato.
- Un dato toccato dal diff che l'inventario attribuisce soltanto ad altri repository e un indizio concreto di accesso ai dati privati di un altro servizio (`MS-DB-ACCESS`) o di integrazione tramite schema condiviso (`MS-DB-INTEGRATION`), da confermare con il percorso nel codice.
- Un dato attribuito a piu repository indica una condivisione da verificare: la review **DEVE** stabilire il proprietario e se gli altri repository ne sono versioni, worker o migrator dello stesso servizio (`MS-DB-OWNERSHIP`). Una condivisione preesistente non va imputata alla PR, salvo che il diff la introduca o la aggravi, ad esempio aggiungendo scritture o migrazioni.
- Un dato toccato dal diff assente dalla voce del repository, o un repository assente dall'inventario, **DEVE** essere segnalato come `[NON-VERIFICABILE]`, chiedendo di confermarne il proprietario e di aggiornare l'inventario nel repository centrale.

L'inventario descrive l'uso dichiarato dei dati, non la loro proprieta, i privilegi effettivi o la modalita di accesso. Da solo **NON DEVE** fondare una `[VIOLAZIONE]`, che richiede comunque l'evidenza nel codice; un inventario incompleto o non aggiornato non prova l'assenza di accessi trasversali. Se il file manca o non e leggibile, la review **DEVE** dichiararlo come limite e procedere con le altre evidenze.

## MS-DB-OWNERSHIP: proprieta esclusiva dei dati

Ogni insieme di dati persistenti di business (tabelle, collezioni e relativi oggetti di schema) **DEVE** avere un solo microservizio proprietario, responsabile di significato, scritture, regole di accesso e migrazioni. Il confine **DEVE** essere ricostruibile da codice e configurazione e, per confini nuovi o modificati, essere documentato.

- Servizi diversi **NON DEVONO** aggiornare gli stessi dati autoritativi o gestirne indipendentemente le migrazioni, neppure se sviluppati dallo stesso team.
- Un servizio **NON DEVE** utilizzare entity ORM o repository di un altro servizio per accedere al suo schema privato. Una libreria condivisa di contratti API/eventi o value object generici non costituisce, da sola, una violazione.
- Repliche, versioni coesistenti, worker e migrator appartenenti allo stesso servizio **POSSONO** condividere i suoi dati. La loro appartenenza **DEVE** essere verificata tramite responsabilita e ciclo di vita, non desunta solo dal nome del deployment.
- Identificativi del proprietario **POSSONO** essere memorizzati da altri servizi come riferimenti logici, senza accesso al suo database o vincoli referenziali verso il suo schema.

Evidenze pertinenti: inventario `data-stores.json`, operazioni di scrittura, repository, mapping, changelog, job, manifest e documenti di ownership. Due connection string simili o una entity con lo stesso nome non provano da sole una proprieta condivisa.

## MS-DB-ACCESS: isolamento logico e autorizzativo

I dati privati del proprietario **NON DEVONO** essere letti o modificati direttamente da altri servizi, nemmeno per sole letture. Essere read-only riduce il rischio di corruzione, ma non elimina il vincolo sullo schema privato.

- Un server, cluster o motore condiviso **PUO** ospitare database, schemi o collezioni privati, purche ownership, privilegi e gestione delle migrazioni garantiscano l'isolamento.
- Ogni servizio **DEVE** usare un'identita di runtime con privilegi limitati ai propri dati. Credenziali condivise o ruoli con accesso trasversale ai dati di business violano l'isolamento quando il privilegio e dimostrato; un riferimento a un secret non ne prova il contenuto o i permessi.
- Query native, ORM, viste, procedure, trigger, database link e job **NON DEVONO** aggirare il confine, anche quando l'accesso e nascosto dietro un repository locale o una vista apparentemente privata.
- Identita amministrative, backup e migrator **POSSONO** avere privilegi distinti per finalita operative, con responsabilita, restrizioni e audit documentati. Il servizio applicativo **NON DEVE** ereditarli.
- La verifica **DEVE** includere, quando interessati, configurazioni dei diversi ambienti, grant e policy infrastrutturali. Database separati non provano isolamento se i servizi possono comunque accedere ai dati altrui.

Esempio di violazione: il servizio ordini esegue una query sulla tabella privata dei clienti per validare una richiesta. Correzione minima: ottenere il dato dal contratto del servizio clienti o da una proiezione locale alimentata tramite un contratto supportato.

## MS-DB-INTEGRATION: integrazione senza schema condiviso

L'interazione tra proprietari **DEVE** avvenire tramite contratti espliciti, ad esempio API o eventi. Gli altri servizi **NON DEVONO** dipendere dalla struttura interna del database del proprietario.

- Join, foreign key, viste e transazioni che attraversano dati privati di servizi diversi **NON DEVONO** essere usati come integrazione applicativa. Una transazione locale su piu tabelle dello stesso proprietario e legittima.
- Per interrogazioni trasversali **SI POSSONO** usare composizione di API o proiezioni locali. Le proiezioni **DEVONO** avere un proprietario, non scrivere sulla fonte e dichiarare consistenza, ritardo tollerabile e recupero previsti dal caso d'uso.
- Regole che richiedono coordinamento tra servizi **DEVONO** avere garanzie esplicite di consistenza e gestione dei fallimenti. Saga, outbox e deduplicazione **DEVONO** essere valutati quando richiesti da tali garanzie, non prescritti automaticamente per ogni chiamata.
- CDC **PUO** alimentare una proiezione se gestito dal proprietario o da una piattaforma delegata, con contratto pubblicato, controllo delle modifiche e accessi limitati. Leggere direttamente un change log privato senza accordo resta accoppiamento allo storage.
- Reporting, ETL e data platform **POSSONO** usare interfacce di esportazione dedicate o repliche analitiche autorizzate: devono essere non autoritative, senza write-back, con contratto, permessi e responsabilita espliciti. Non autorizzano letture trasversali del database operativo da parte dei microservizi applicativi.
- Uno schema realmente condiviso tra servizi, anche se documentato o temporaneamente approvato, resta una deviazione dal pattern. La review **DEVE** indicare deroga e rischio residuo, senza presentarlo come Database-per-Service.

Una migrazione verso dati privati **DEVE** definire fonte autoritativa, transizione dei lettori/scrittori, eventuale sincronizzazione temporanea, gestione dei fallimenti e dismissione degli accessi precedenti. Non basta sostituire una query con una chiamata remota ignorando timeout o consistenza.

## MS-DOMAIN-MODEL: comportamento e modello di dominio

Per un dominio con invarianti, calcoli, decisioni o transizioni di stato significative, il modello **DEVE** esprimere tali regole tramite entita, value object, aggregati e, dove appropriato, domain service o policy.

Anemic Domain Model e la combinazione di oggetti che rappresentano concetti di dominio ma contengono quasi solo dati mutabili, e regole di business collocate sistematicamente altrove. L'assenza di metodi in una singola classe non e una prova sufficiente.

- Controller, consumer e application service **NON DEVONO** diventare la sede delle regole intrinseche degli oggetti, limitandosi a manipolarne i campi. Una regola concreta e il percorso che la esegue **DEVONO** essere identificati prima di segnalare anemia.
- L'application service **DEVE** orchestrare il caso d'uso: caricamento, invocazione del dominio, persistenza, transazioni e integrazioni. Un metodo lungo o una classe chiamata `Service` non dimostrano da soli la violazione.
- Un domain service stateless **PUO** implementare una regola che non appartiene naturalmente a una singola entita, ad esempio una policy che coinvolge piu concetti. Non deve essere segnalato solo perche esterno all'entita.
- DTO, request/response, eventi, read model CQRS, proiezioni, record immutabili e modelli di persistenza separati **POSSONO** contenere solo dati. La review **DEVE** cercare il modello comportamentale effettivo e i suoi chiamanti prima di trarre conclusioni.
- Servizi CRUD, integrazioni o cataloghi con regole semplici **POSSONO** usare Transaction Script o modelli data-centric. La scelta **DEVE** essere coerente con le regole presenti; un ADR non giustifica bypass o duplicazione di invarianti introdotte successivamente.
- Un modello funzionale con dati immutabili e funzioni/policy di dominio **PUO** essere legittimo se confini e invarianti sono protetti. Non e obbligatoria la co-locazione fisica di stato e metodi.

Esempio di violazione: un application service calcola penali e decide le transizioni di un ordine, poi modifica campi tramite setter; altri ingressi possono modificare gli stessi campi senza quelle regole. Correzione minima: esprimere la transizione nell'aggregato o in una policy di dominio appropriata, lasciando I/O e coordinamento all'application service.

## MS-DOMAIN-INVARIANTS: stato valido e transizioni controllate

Le invarianti **DEVONO** essere rispettate alla creazione e dopo ogni operazione di business. Costruttori/factory, metodi di dominio e confini di aggregato **DEVONO** impedire mutazioni applicative che lasciano stati non validi.

- Setter pubblici, collection modificabili esposte e campi pubblici **DEVONO** essere valutati rispetto alle invarianti concrete. Non sono vietati indistintamente; sono un problema quando permettono di aggirare le regole necessarie.
- Validazione sintattica del trasporto, autorizzazione e regole di dominio **DEVONO** essere distinte. Una validazione solo nel controller non protegge consumer, batch o altri casi d'uso che mutano lo stesso concetto.
- Aggiornamenti bulk, SQL diretti, mapping, deserializzazione e consumer **NON DEVONO** introdurre percorsi di business che bypassano invarianti. L'idratazione ORM, da sola, non prova un bypass applicativo: occorre identificare un percorso realmente utilizzabile.
- Una regola di dominio **NON DEVE** essere duplicata in ingressi diversi con rischio concreto di divergenza. Vincoli DB possono rafforzarla, ma non sostituiscono la decisione di business quando serve al caso d'uso.
- Se la regola dipende da stato concorrente, la review **DEVE** valutare i meccanismi di persistenza pertinenti (versionamento ottimistico, lock o aggiornamento condizionale). Un metodo di dominio non garantisce da solo la consistenza tra richieste concorrenti.
- Entita e value object **NON DEVONO** assumere responsabilita di HTTP, broker, repository o coordinamento remoto per risultare "ricchi". Le dipendenze necessarie **DEVONO** restare nei confini appropriati.

Esempio di prova mirata: una transizione vietata viene rifiutata sia dal caso d'uso HTTP sia dal consumer; una modifica della collection esposta non permette di eludere il limite dell'aggregato. Il test deve verificare la regola reale, non solo la presenza di un metodo.

## MS-EVIDENCE: evidenze e formato della review

Per i confini o le regole interessati dalla modifica **DEVONO** essere disponibili evidenze proporzionate al rischio:

- Ownership dei dati e decisioni architetturali, con motivazioni delle eventuali deroghe e responsabile della loro revisione.
- Mapping tra servizi, identita di runtime, storage, migrazioni e privilegi effettivi, a partire dall'inventario `data-stores.json`; test o verifiche che un servizio non possa accedere ai dati privati di un altro.
- Test di contratto e integrazione per API/eventi/proiezioni coinvolti; verifiche di consistenza, fallimenti e rollout quando cambiano i confini.
- Test delle invarianti e delle transizioni valide/non valide, con percorsi alternativi e concorrenza quando pertinenti.

Le prove **DEVONO** distinguere risultati eseguiti, test presenti ma non eseguiti e test solo proposti. La sola assenza di test o ADR nel diff non prova che manchino: cercare le evidenze pertinenti accessibili e dichiarare i limiti. Non richiedere automaticamente migrazioni, test end-to-end o un modello DDD completo per modifiche che non li riguardano.

Ogni segnalazione **DEVE** avere un titolo nel formato `[MS-ID-REGOLA][STATO]`, usando l'ID esatto della regola e uno stato tra `[VIOLAZIONE]`, `[NON-VERIFICABILE]` e `[PROVA-MANCANTE]`. Per esempio: `[MS-DB-ACCESS][VIOLAZIONE]`. **VIOLAZIONE** **DEVE** essere usato solo con evidenza concreta del diff e impatto dimostrato; rischi non verificabili e prove mancanti **NON DEVONO** essere presentati come violazioni accertate. Per le prove trasversali usare `[MS-EVIDENCE][PROVA-MANCANTE]`; quando la lacuna riguarda una regola specifica si **PUO** usare l'ID di quella regola.

Ogni segnalazione deve inoltre riportare:

1. Gravita motivata dall'impatto e stato: **violazione dimostrata**, **aspetto non verificabile** o **prova obbligatoria mancante**.
2. File e riga pertinente; in PR un punto modificato che introduce/aggrava il problema, con riferimenti alle implementazioni collegate accessibili.
3. Evidenza concreta: proprietario e accesso dimostrati, oppure regola di business e percorso di mutazione. Per lacune, indicare esattamente cosa manca senza affermare una violazione non provata.
4. Impatto specifico, ad esempio accoppiamento di schema/rilascio, scritture non autorizzate, stato invalido o divergenza delle regole.
5. Correzione minima; per lacune, responsabile per ruolo e verifica richiesta prima del rilascio, senza inventare persone o scadenze.

Le segnalazioni **DEVONO** essere ordinate per gravita e prive di duplicati. Usare la regola piu specifica; `[MS-EVIDENCE]` serve per lacune trasversali non gia coperte. Una violazione architetturale non e automaticamente critica: motivare la priorita rispetto a impatto, raggiungibilita e probabilita. Debito preesistente e suggerimenti fuori perimetro **NON DEVONO** essere presentati come regressioni.

In assenza di evidenze sufficienti, formulare una richiesta precisa, ad esempio verificare con il responsabile della piattaforma i grant del ruolo di runtime. **NON SI DEVONO** esporre credenziali o valori dei secret nei commenti.

GitHub Copilot supporta la review, ma non sostituisce test, analisi dei responsabili e controlli di rilascio. L'assenza di segnalazioni non certifica l'architettura. Le istruzioni orientano il modello e non garantiscono controlli deterministici.