# YT Transcriptor App 🎥📝

YT Transcriptor è un'applicazione Android nativa scritta in **Java** che permette di estrarre e leggere i sottotitoli (transcript) di qualsiasi video YouTube. L'applicazione utilizza un'interfaccia utente semplice ed elegante in Dark Mode e memorizza in cache l'ultima trascrizione caricata per garantire un'esperienza utente fluida.

---

## ✨ Funzionalità Principali

- 🔗 **Supporto a Link e ID:** Puoi incollare l'URL completo del video YouTube (inclusi link `youtu.be`, `shorts` ecc.) o semplicemente l'ID di 11 caratteri.
- ⚡ **API Veloce e Sicura:** Utilizza il servizio [YouTube Transcriptor su RapidAPI](https://rapidapi.com/) per il recupero dei dati in tempo reale.
- 💾 **Caching Locale:** Salva automaticamente l'ultima trascrizione in locale (`transcript_cache.json`). Al riavvio dell'app, non sarà necessario rifare la chiamata API se vuoi rivedere lo stesso video.
- 🛡️ **Gestione degli Errori Robusta:** Gestisce esplicitamente i limiti di rate (HTTP 429), chiavi API non valide (HTTP 401/403) e video inesistenti o senza sottotitoli (HTTP 404).
- 🔐 **Sicurezza API Key:** Utilizza un file `.env` e `BuildConfig` per assicurare che la chiave API non venga mai salvata nel repository o esposta nel codice sorgente.

---

## 🛠️ Stack Tecnologico

- **Linguaggio:** Java
- **UI:** XML Layouts, RecyclerView (con Adapter personalizzato)
- **Networking:** `HttpURLConnection`, `ExecutorService` (per task asincroni in background)
- **JSON Parsing:** libreria nativa `org.json`
- **Build System:** Gradle (Kotlin DSL)

---

## 📦 Installazione e Configurazione

Per clonare, configurare ed eseguire il progetto localmente, segui questi passaggi:

### 1. Clonare il repository

```bash
git clone https://github.com/simosw69/yt-transcriptor-app.git
cd yt-transcriptor-app
```

### 2. Configurare l'API Key

L'applicazione comunica con RapidAPI per funzionare correttamente. Devi fornire la tua chiave privata:

1. Registrati su [RapidAPI](https://rapidapi.com/) e iscriviti all'API "YouTube Transcriptor".
2. Nella **cartella radice** del progetto (allo stesso livello di `settings.gradle.kts`), crea un file di testo e chiamalo **`.env`**.
3. All'interno del file, inserisci la tua API Key seguendo questo formato (assicurati di includere le virgolette se richiesto dal parsing, altrimenti metti il valore pulito, per via del file gradle verrà avvolto come stringa):

```env
RAPIDAPI_KEY="la_tua_api_key_qui"
```

*(Il file `.env` è già ignorato da Git tramite `.gitignore` in modo da non essere committato per sbaglio).*

### 3. Aprire il progetto su Android Studio

1. Avvia **Android Studio**.
2. Fai clic su **File > Open** e seleziona la cartella `yt-transcriptor-app`.
3. Attendi la sincronizzazione di Gradle (Gradle leggerà automaticamente il file `.env` e inietterà la chiave nel codice come `BuildConfig.RAPIDAPI_KEY`).

### 4. Compilazione ed Esecuzione

- Assicurati di avere un emulatore configurato o un dispositivo fisico collegato con il debug USB attivo.
- Clicca sul pulsante **Run 'app'** (il triangolo verde ▶️ in alto) o premi `Shift + F10`.

---

## 📂 Struttura del Codice

- `MainActivity.java`: Cuore pulsante dell'app. Gestisce UI, click dei bottoni, validazione dell'input con Regex, chiamate di rete in multithreading e la lettura/scrittura della cache JSON.
- `Subtitle.java`: Il modello dati (POJO) per rappresentare ogni singola riga di sottotitolo (testo, inizio, durata).
- `SubtitleAdapter.java`: Adapter per legare i dati della lista al componente grafico `RecyclerView`.
- `build.gradle.kts`: Contiene la logica per estrarre la `RAPIDAPI_KEY` dal file `.env`.

---

## 👨‍💻 Contributi

Sentiti libero di aprire **Issues** o inviare **Pull Requests** per suggerire miglioramenti (es. supporto per scaricare il testo in formato txt, implementazione di una dark/light mode dinamica, o un player YouTube integrato).

---

## 📄 Licenza

Distribuito sotto licenza MIT.
