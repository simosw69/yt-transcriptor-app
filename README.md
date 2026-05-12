# YT Transcriptor

Trasforma qualsiasi video YouTube in testo in pochi secondi.  
**YT Transcriptor** estrae automaticamente i sottotitoli/transcript da video YouTube e li converte in testo leggibile, esportabile e pronto per essere utilizzato.

---

## 🚀 Features

- 🎥 Estrazione transcript da video YouTube
- 🌍 Supporto multi-lingua
- ⚡ Elaborazione veloce
- 📄 Export in `.txt`, `.json` o `.md`
- 🧠 Pulizia automatica del testo
- 🔍 Ricerca all’interno del transcript
- 📌 Timestamp opzionali

---

## 📦 Installazione

```bash
git clone https://github.com/tuo-user/yt-transcriptor.git

cd yt-transcriptor

npm install
```

---

## ▶️ Avvio del progetto

```bash
npm run dev
```

Oppure in produzione:

```bash
npm start
```

---

## 🛠️ Utilizzo

### Esempio CLI

```bash
node index.js "https://www.youtube.com/watch?v=VIDEO_ID"
```

### Output esempio

```txt
[00:00] Benvenuti nel video...
[00:12] Oggi parleremo di...
```

---

## 📁 Struttura del progetto

```bash
yt-transcriptor/
│
├── src/
│   ├── services/
│   ├── utils/
│   ├── parser/
│   └── index.js
│
├── output/
├── package.json
└── README.md
```

---

## 🔧 Tecnologie utilizzate

- Node.js
- Express
- YouTube Transcript API
- TypeScript / JavaScript

---

## 🌐 API Example

### Request

```http
GET /api/transcript?url=https://youtube.com/watch?v=VIDEO_ID
```

### Response

```json
{
  "title": "Video Title",
  "language": "en",
  "transcript": [
    {
      "text": "Hello everyone",
      "start": 0.0
    }
  ]
}
```

---

## 📌 Roadmap

- [ ] Supporto playlist YouTube
- [ ] Download PDF
- [ ] AI Summary
- [ ] Traduzione automatica
- [ ] Web UI responsive

---

## 🤝 Contributing

Le pull request sono benvenute.  
Per modifiche importanti, apri prima una issue per discutere cosa desideri cambiare.

---

## 📄 Licenza

Distribuito sotto licenza MIT.

---

## 👨‍💻 Autore

Creato con ❤️ da **Il Tuo Nome**
