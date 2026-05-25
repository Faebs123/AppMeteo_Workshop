1. Obiettivo del Progetto

L'obiettivo è rilasciare un'applicazione desktop leggera, intuitiva e cross-platform (Windows, macOS, Linux) scritta in Java. L'utente inserisce il nome di una località e il sistema recupera e mostra in tempo reale le informazioni meteorologiche correnti e le previsioni a breve termine, utilizzando un'API esterna.
2. Requisiti del Sistema
Requisiti Funzionali (Cosa fa l'app)

    Ricerca Località: Un campo di testo in cui l'utente può digitare una città (es. "Milano" o "Tokyo").

    Visualizzazione Meteo Corrente: Mostra i dati essenziali:

        Temperatura attuale (in °C o °F)

        Condizioni meteo (es. Nuvoloso, Pioggia, Sereno) con relativa icona grafica

        Umidità, velocità del vento e pressione atmosferica

    Previsioni: Un pannello secondario che mostra le previsioni per le successive 24 ore o per i 3-5 giorni successivi.

    Gestione Errori: Notifiche chiare in caso di città non trovata, assenza di connessione internet o superamento dei limiti dell'API.

    Preferiti/Cronologia (Opzionale): Possibilità di salvare le città cercate di frequente per un accesso rapido.

Requisiti Non Funzionali (Come lo fa)

    Interfaccia Grafica (GUI): Pulita, responsiva e moderna.

    Performance: Il recupero dei dati tramite rete non deve bloccare l'interfaccia utente (uso di thread separati).

    Portabilità: Sfruttare la natura "Write Once, Run Anywhere" di Java.

3. Architettura Software e Stack Tecnologico

L'applicazione seguirà il pattern architetturale MVC (Model-View-Controller) per garantire la separazione delle responsabilità e la manutenibilità del codice.

+-------------------------------------------------------+
|                      VIEW (GUI)                       |
|          (JavaFX / Input Utente e Grafica)            |
+---------------------------+---------------------------+
                            |
            Invia Input     |     Aggiorna la View
            dell'Utente     |     con i nuovi dati
                            v
+-------------------------------------------------------+
|                      CONTROLLER                       |
|         (Gestore Eventi e Logica di Flusso)           |
+---------------------------+---------------------------+
                            |
            Richiede Dati   |     Ritorna i dati
            Meteo           |     Elaborati (Oggetti Java)
                            v
+-------------------------------------------------------+
|                        MODEL                          |
|    (Logica di Business, Client HTTP, Parsing JSON)    |
+-------------------------------------------------------+
                            |
             Richiesta HTTP |     Risposta JSON
             all'API        |
                            v
               +-------------------------+
               |   API Meteo Esterna     |
               |  (es. OpenWeatherMap)   |
               +-------------------------+

Stack Tecnologico Consigliato

    Linguaggio: Java Core (versione 17 LTS o successiva per sfruttare le feature moderne).

    Interfaccia Grafica: JavaFX. È la libreria standard de facto moderna per le GUI in Java (superiore alla vecchia Swing), ideale per gestire CSS, transizioni e interfacce scalabili.

    Gestione Network & JSON: * HttpClient (nativo di Java) per le richieste asincrone all'API.

        Jackson o Gson per il parsing dei dati JSON ricevuti in oggetti Java.

    Build Tool: Maven o Gradle per la gestione delle dipendenze esterne.

4. Integrazione con API Esterne

Il cuore informativo dell'applicazione si basa su un servizio di terze parti. La scelta più comune e affidabile è OpenWeatherMap (o in alternativa WeatherAPI o Meteo-Standard).
Flusso dei Dati

    L'utente digita "Roma" e clicca "Cerca".

    Il Model effettua una chiamata HTTP GET:
    https://api.openweathermap.org/data/2.5/weather?q=Roma&appid=TUA_API_KEY&units=metric&lang=it

    L'API risponde con un payload JSON contenente le coordinate, la temperatura, il codice dell'icona e la descrizione del meteo.

    Il Model deserializza il JSON in un oggetto di business (es. WeatherData) e lo passa al Controller, che aggiorna la View.

5. Principali Sfide Tecniche e Soluzioni

    Blocco della GUI (Freeze): Se la chiamata di rete viene eseguita sul thread principale della grafica, l'app si bloccherà durante il caricamento.

        Soluzione: Utilizzare la classe Task o Service di JavaFX per eseguire la richiesta HTTP in un thread in background, aggiornando la GUI solo a operazione completata.

    Gestione delle icone meteo: Scaricare le icone in tempo reale da internet può rallentare l'app.

        Soluzione: Integrare un set di icone vettoriali o PNG direttamente all'interno delle risorse locali del progetto (src/main/resources), mappandole in base al codice meteo restituito dall'API (es. codice 01d -> sole.png).

    Sicurezza della API Key: Non caricare mai la chiave dell'API in chiaro su repository pubblici come GitHub.

        Soluzione: Caricare la chiave a runtime da un file di configurazione esterno (.properties o .env) escluso dal controllo di versione.