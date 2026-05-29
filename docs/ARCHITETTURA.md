Contesto del Progetto:
Sto avviando un progetto in Java e voglio organizzarlo seguendo un approccio "Feature-First" (package per funzionalità) combinato con un'architettura Model-View-Controller (MVC) all'interno di ciascuna feature. 

[OPZIONALE: Inserisci qui una breve descrizione del tuo progetto. Es: "Il progetto è un sistema di gestione per una biblioteca" oppure "È un'applicazione desktop per la gestione di un inventario"].

Obiettivo:
Aiutami a pianificare e strutturare l'albero dei pacchetti (package tree) e a definire le classi principali necessarie per iniziare.

Linee guida richieste per la struttura:
1. Approccio Feature-First: La cartella principale del codice sorgente deve essere divisa in base alle macro-funzionalità del sistema (es. 'utente', 'catalogo', 'prenotazioni'), più un pacchetto 'shared' o 'core' per le utilità trasversali.
2. MVC dentro la Feature: All'interno di ogni singolo pacchetto di una feature, i componenti devono essere separati in 'model', 'view' e 'controller' (es. feature.utente.model, feature.utente.view, ecc.).
3. Disaccoppiamento: I controller devono fare da collante, le viste non devono conoscere la logica di business e i modelli devono essere indipendenti dalla rappresentazione grafica.

Cosa voglio che tu generi:
1. L'albero dei pacchetti (Package Tree) di esempio basato sul mio progetto (o un esempio standard se non ho specificato il dominio).
2. Una spiegazione rapida del flusso di dati tra Model, View e Controller all'interno di una singola feature.
3. Lo scheletro del codice Java (interfacce o classi base con i metodi principali vuoti) per una singola feature d'esempio, mostrando come il Controller istanzia o mette in comunicazione il Model e la View.
4. Eventuali consigli su come gestire le comunicazioni tra feature diverse (es. se la Feature A ha bisogno di dati dalla Feature B).

Rispondi in modo schematico, chiaro e orientato al codice pulito.