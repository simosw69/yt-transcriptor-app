# YouTube Transcriptor Android App

**YouTube Transcriptor** è un'applicazione Android che permette di estrarre e visualizzare i sottotitoli (transcript) dei video di YouTube. Offre un'esperienza ottimizzata su due layout: in modalità verticale (portrait) per leggere l'intero transcript e in modalità orizzontale (landscape) per visualizzare il video affiancato ai sottotitoli sincronizzati, in stile "karaoke".

---

## 🚀 Funzionalità

- 🎥 **Sincronizzazione video e testo:** In modalità orizzontale, i sottotitoli scorrono in sincrono con il player YouTube.
- 📱 **Dual Layout:** Interfaccia dinamica per adattarsi all'orientamento del dispositivo (verticale per la lettura e orizzontale per la visualizzazione video/testo).
- ⚡ **Caching delle trascrizioni:** L'app salva le trascrizioni ottenute in cache (`SharedPreferences`) per velocizzare i caricamenti futuri ed evitare chiamate API inutili.
- 🔒 **Gestione sicura API Key:** Le credenziali API sensibili sono archiviate in modo sicuro tramite un file `.env` a livello di progetto.

---

## 🛠️ Tecnologie Utilizzate

- **Linguaggio:** Java / Android SDK
- **Player Video:** YouTube Player
- **API Dati:** RapidAPI "YouTube Transcriptor"
- **Sicurezza:** Variabili d'ambiente tramite file `.env`

---

## 📦 Installazione e Configurazione

Segui questi passaggi per clonare, configurare ed eseguire l'applicazione sul tuo ambiente locale.

### 1. Clona il repository

```bash
git clone https://github.com/simosw69/yt-transcriptor-app.git
cd yt-transcriptor-app
```

### 2. Configura le credenziali API

Per far funzionare l'estrazione dei sottotitoli, è necessario ottenere una chiave API dal servizio "YouTube Transcriptor" su [RapidAPI](https://rapidapi.com/).

1. Crea un file chiamato `.env` nella cartella **radice** del progetto (allo stesso livello di `settings.gradle.kts`).
2. Aggiungi la tua chiave API all'interno del file in questo formato:

```env
RAPID_API_KEY=inserisci_qui_la_tua_api_key_di_rapidapi
```

*(Nota: il file `.env` è inserito nel file `.gitignore` per evitare che la chiave API venga esposta pubblicamente su GitHub).*

### 3. Apri il progetto in Android Studio

1. Apri **Android Studio**.
2. Seleziona **File > Open...** (o "Open" dalla schermata di benvenuto).
3. Naviga fino alla cartella `yt-transcriptor-app` e selezionala.
4. Attendi che Gradle abbia finito la sincronizzazione del progetto (Android Studio scaricherà automaticamente le dipendenze necessarie).

### 4. Avvia l'applicazione

- Puoi eseguire l'app su un emulatore integrato in Android Studio (AVD) o su un dispositivo Android fisico (assicurati di avere abilitato il "Debug USB").
- Clicca sul pulsante **Run (▶)** nella toolbar in alto (oppure premi `Shift + F10`).

---

## 👨‍💻 Contributi

Le pull request sono le benvenute. Per modifiche importanti, ti invitiamo ad aprire prima una issue per discutere le funzionalità che vorresti implementare o i bug che hai riscontrato.

---

## 📄 Licenza

Questo progetto è distribuito con licenza MIT.
