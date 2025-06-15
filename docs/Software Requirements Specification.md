>[!info] Legenda
>Categorie requisiti:
>- **IF**: Funzionalità individuali
>- **DF**: Dati e formato dei dati
>- **UI**: Interfaccia utente
>- **NF**: Requisiti non funzionali
>- **PC**: Vincoli di progetto
>
>Livelli di priorità (Business value/Rischio tecnico):
>- **H**: Must-have / alto
>- **M**: Should-have / medio
>- **L**: Nice-to-have / basso

| ID  | Nome                                | Descrizione breve                                                                                                                                                                       | Categoria | Business value | Rischio tecnico |
| --- | ----------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------- | -------------- | --------------- |
| 1   | Menu principale                     | Dopo l'autenticazione, viene mostrato un menu principale che permette di navigare verso le varie funzionalità dell'applicazione.                                                        | UI        | H              | L               |
| 2   | Fase di lettura                     | Uno o più documenti testuali (precedentemente forniti) vengono mostrati all'utente per un tempo limitato.                                                                               | IF        | H              | H               |
| 3   | Fase quiz                           | Dopo la fase di lettura, è generata una successione di domande a risposta multipla con 4 opzioni ciascuna.                                                                              | IF        | H              | H               |
| 4   | Selezione del livello di difficoltà | All'avvio di una partita, al giocatore è richiesto di scegliere tra tre livelli di difficoltà.                                                                                          | IF        | H              | L               |
| 5   | Varietà delle domande               | Le domande del quiz devono appartenere a un set di tipologie distinte per garantirne la varietà.                                                                                        | NF        | H              | M               |
| 6   | Autenticazione utenti               | Il sistema è multiutente, e permette la registrazione e l'autenticazione tramite nickname e password.                                                                                   | IF        | H              | M               |
| 7   | Persistenza dati utente             | Per ogni account registrato è memorizzato, in maniera persistente su database relazionale, uno storico dei punteggi delle partite effettuate.                                           | DF        | H              | M               |
| 8   | Stop words                          | E' prevista dal gioco una lista di *stop words*, ossia parole che verranno escluse dalle parole candidate all'analisi del quiz.                                                         | IF        | H              | L               |
| 9   | Privilegi di amministratore         | Un utente può essere promosso ad amministratore. Questi utenti hanno privilegi speciali che consentono di personalizzare il gioco.<br>                                                  | IF        | H              | L               |
| 10  | Leaderboard                         | Il gioco mette a disposizione una classifica globale di tutti gli utenti e una divisa per difficoltà delle partite giocate.                                                             | IF        | M              | M               |
| 11  | Pannello utente                     | E' possibile visualizzare una sezione che fornisce informazioni sull'utente e sulle partite giocate.                                                                                    | UI        | M              | M               |
| 12  | Statistiche post-partita            | Al termine di una partita, è possibile visualizzare informazioni dettagliate sul quiz appena concluso.                                                                                  | IF        | M              | L               |
| 13  | Supporto multilingua                | Un amministratore può caricare documenti (e *stop words*) in diverse lingue e l'utente, al momento della sessione di gioco, può decidere in che lingua giocare.                         | IF        | L              | H               |
| 14  | Gestione sessioni interrotte        | Se l'utente interrompe una sessione di gioco prima che questa sia terminata, il sistema provvede a salvare lo stato corrente della partita e lo ripropone al riavvio dell'applicazione. | IF        | L              | M               |
| 15  | Styling CSS                         | L'interfaccia è abbellita mediante l'utilizzo di fogli di stile CSS.                                                                                                                    | NF        | M              | -               |
| 16  | Implementazione in Java+JavaFX      | L'applicazione è realizzata utilizzando il JDK 1.8, e JavaFX è utilizzato per la generazione dell'interfaccia grafica.                                                                  | PC        | H              | -               |
| 17  | Logging                             | Al verificarsi di un'eccezione o di un errore, queste vengono memorizzate in un file di logging testuale consultabile in qualunque momento dal file system.                             | IF        | L              | M               |
##### Autenticazione utenti
Il programma supporta un sistema di gestione degli account, permettendo quindi di effettuare sessioni di gioco come utenti diversi. All'avvio, all'utente è richiesto di autenticarsi o registrarsi. Una volta eseguito il sign in/up, l'account viene identificato come attivo, ossia agli avvii successivi del programma l'utente sarà automaticamente loggato con quell'account, finché non esegue manualmente il logout (ad esempio per cambiare utente). In seguito alla fase di autenticazione (che può anche corrispondere alla confermata presenza di un account attivo implicita al sistema ), l'utente viene indirizzato al menu principale.
##### Fase di lettura
Costituisce la prima fase di gioco vera e propria. Il sistema seleziona, in base alla difficoltà scelta e ai documenti a disposizione, le opzioni di gioco:
- quali documenti
- quanti documenti
- il valore del timer
Una volta istanziato il contesto della partita, il timer si avvia e vengono mostrati a schermo i documenti, in un'interfaccia agevola la navigazione tra le pagine e consente di terminare la lettura prima dello scadere del timer.
##### Fase quiz
Entro il termine della fase di lettura, il sistema genera le domande da porre nel quiz che costituisce la fase successiva del gioco.
Viene dunque mostrato un elenco di domande a risposta multipla (4 risposte possibili), con domande relative alla presenza e frequenza di parole all'interno dei documenti precedentemente visualizzati, un timer e un pulsante ***Fine***. Allo scadere del timer o alla pressione del pulsante, la partita termina e vengono calcolati e visualizzati i risultati.
>[!note] Nota
>La scelta di generare le domande prima della fase del quiz consente di minimizzare l'overhead nel caricamento del quiz stesso al termine della lettura, poiché durante quest'ultima al programma sono richieste poche risorse, e può quindi "portarsi avanti" eseguendo operazioni di preparazione alla fase successiva.
##### Gestione sessioni interrotte
Quando l'utente chiude il programma durante il corso di una sessione di gioco, quest'ultimo registra il contesto della partita e, all'avvio seguente, propone all'utente tramite un popup di riprendere la sessione interrotta. In caso di risposta affermativa, verrà caricato il contesto salvato dell'ultima partita, altrimenti si verrà reindirizzati al menu principale.
##### Leaderboard
Dal menu principale è possibile accedere ad una pagina del menu contenente la leaderboard del gioco. Questa contiene:
- classifica globale, relativa a tutte le partite giocate sul dispositivo da ogni utente
- classifica per difficoltà, che permette di filtrare i risultati in base alle partite giocate dagli utenti in una determinata difficoltà.
##### Logging
Come strumento di diagnostica al fine di aumentare la manutenibilità del software, è possibile, dalle impostazioni del programma, abilitare la funzione di logging, che prevede la memorizzazione di eccezioni ed errori verificatisi a runtime in un file testuale consultabile dalla cartella contenente i file di gioco.
##### Menu principale
In seguito alla (eventuale) schermata di autenticazione, viene visualizzato a schermo il menu principale, da cui è possibile:
- iniziare una nuova partita
- visualizzare la pagina della leaderboard
- visualizzare il pannello utente (admin)
- uscire dall'applicazione
##### Pannello utente
Dal menu principale è possibile accedere al pannello utente, che permette di:
- visualizzare le informazioni di autenticazione dell'utente
- consultare statistiche sulle partite precedentemente giocate (punteggio medio, miglior punteggio, ultime partite
- modificare la password di accesso
- effettuare il logout
- cancellare l'account
##### Persistenza dati utente
Il sistema mantiene un database locale su file (SQLite) per garantire la persistenza delle informazioni relative agli utenti registrati, sia quelle di autenticazione, sia statistiche relative alle partite giocate in precedenza.
##### Privilegi di amministratore
Ogni utente può essere o meno categorizzato come amministratore. Un amministratore possiede dei privilegi speciali che gli permettono, tramite una sezione speciale del suo pannello utente, di:
- aggiornare la lista di documenti di testo usati nelle sessioni di gioco
- aggiornare la lista di *stop words*
- conferire o revocare il privilegio di amministratore ad altri utenti
Alla registrazione del primo utente del programma, questo verrà automaticamente promosso al ruolo di amministratore.
##### Selezione del livello di difficoltà
All'avvio di una partita, all'utente è richiesto di scegliere fra 3 livelli di difficoltà: ***facile***, ***medio*** e ***difficile***. La scelta della difficoltà impatterà sulla lunghezza e il numero di documenti, ed eventualmente sul valore del timer.
##### Statistiche post-partita
Al termine di una partita, prima di ritornare al menu principale, l'utente può consultare informazioni dettagliate sulla partita terminata, sia relative alle risposte del quiz (con un confronto tra le risposte scelte e quelle corrette), sia statistiche sulle parole presenti nei documenti
##### Stop words
Il motore di gioco prevede la gestione di una lista di stop words: le parole inserite in questa lista saranno ignorate dall'algoritmo di generazione delle domande.
E' utile aggiungere a questa lista parole come articoli o preposizioni, che non sono di interesse al dominio descritto dai documenti, e di cui risulta inutilmente difficile memorizzarne la frequenza.
##### Supporto multilingua
Il gioco supporta la possibilità per gli amministratori di fornire materiale di gioco (documenti, stop words) per lingue differenti, consentendo agli utenti di avviare partite in lingue diverse (in presenza di lingue multiple, dal menu di selezione della difficoltà sarà anche possibile selezionare la lingua dei documenti da utilizzare per la sessione di gioco).
##### Varietà delle domande
Le domande proposte dal quiz dovrebbero essere di varie categorie al fine di aggiungere allo stesso un grado di imprevedibilità.
Le tipologie di domande possibili dovrebbero includere almeno:
- Frequenza assoluta (quante volte appare una parola). 
- Confronto tra frequenze di più parole.
- Associazione di parole a documenti specifici (solo se ne è stato presentato più d'uno).
- Individuazione di parole mai comparse.