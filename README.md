
<p align="center">
  <img src="src/main/resources/it/unisa/diem/wordageddon_g16/assets/logo.png" alt="Wordageddon Logo" width="200"/>
</p>

# Wordageddon

>**Wordageddon** è un'applicazione desktop ludico-educativa sviluppata in JavaFX, progettata per allenare la memoria dell'utente attraverso quiz basati sulle parole più frequenti presenti in documenti testuali.

## 🎯 Obiettivi del progetto

L'applicazione sfida l'utente nella memorizzazione e riconoscimento delle parole più frequenti in uno o più documenti mostrati per un tempo limitato. Al termine della lettura, l'utente affronta una serie di domande a risposta multipla.

## Funzionalità principali

- **Autenticazione utenti** con gestione automatica dell’account attivo  
- **Lettura temporizzata** di documenti testuali  
- **Quiz generati automaticamente** con domande a risposta multipla  
- **Livelli di difficoltà**: facile, medio, difficile  
- **Classifica (leaderboard)** e statistiche post-partita  
- **Salvataggio dati** su database locale (SQLite)  
- **Ripresa sessioni interrotte**  

[![Java](https://img.shields.io/badge/Java-24-red?logo=java&logoColor=white)](https://www.oracle.com/java/)
[![JavaFX](https://img.shields.io/badge/JavaFX-%2318B6F2.svg?logo=java&logoColor=white)](https://openjfx.io/)
[![SQLite](https://img.shields.io/badge/SQLite-blue?logo=sqlite&logoColor=white)](https://www.sqlite.org/)
[![CSS3](https://img.shields.io/badge/CSS-blue?logo=css3&logoColor=white)](https://developer.mozilla.org/en-US/docs/Web/CSS)

## 🚀 Documentazione & Risorse
- 📄 [Documentazione Progettuale](docs/Documentazione_Wordageddon_G16.pdf), presente anche su [Overleaf](https://www.overleaf.com/read/rwwgkkkbzkdj#33f35f)
- 📚 [Documentazione Tecnica - Javadoc](https://bg735.github.io/Wordageddon-Gruppo\_16)
- 📺 [Presentazione](docs/Presentazione_Wordageddon_G16.pdf) 
- 🎨 [Mockup Figma](https://www.figma.com/design/bqGUZqN27MYtyQel39LUrE/Wordageddon?node-id=0-1&p=f&t=tQsawDH1bQAf32sZ-0)

## 🔒 Credenziali
**Admin:**
- Username: *admin*
- Password: *admin*

**User**
- Username: *demo*
- Password: *demo*

## Esecuzione tramite `.jar`
Per una rapida esecuzione dell’applicazione è stato fornito il file `Wordageddon.jar`, il quale deve essere eseguito con la JDK 24 o superiore. L'eseguibile non contiene le dipendenze `JavaFX` per cui é necessario  scaricare l’sdk dal sito ufficiale e specificare a runtime i
moduli necessari: 

```
java --module-path $PATH_TO_FX --add-modules javafx.controls,javafx.fxml -jar Wordageddon.jar
```
Dove `$PATH_TO_FX` rappresenta il percorso alla sdk precedentemente scaricata. Si tiene presente che
il file .jar deve essere eseguito dalla stessa cartella (root) in cui é presente il database (db.sqlite)
