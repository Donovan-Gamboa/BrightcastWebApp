# 🔮 Brightcast Web

**A real-time, multiplayer strategic card game built with Angular and Spring Boot.**

> *Compete against friends in this arcane duel of wits. Summon Spellcasters, unleash Monsters, and interrupt your opponent's plans in a race to assemble the ultimate council of magic.*

## 📖 About the Game
**Brightcast** is a digital implementation of a strategic tabletop card game. Two players face off in a 1v1 duel where the goal is to collect **5 unique Spellcasters** or **5 Spellcasters of the same type**.

Players must balance their hand resources, manage a tactical board state, and use **Instant-Speed Interrupts** (like the Wizard's counter-spell) to disrupt their opponent's victory condition.

## ✨ Key Features
* **⚔️ 1v1 Multiplayer:** Real-time gameplay powered by WebSockets.
* **⚡ Reactive "Interrupt" System:** A unique mechanic where players can pause the game to counter an opponent's move instantly.
* **🃏 Full Rules Engine:** Enforces turn phases (Draw -> Play), hand limits, and specific card interactions automatically.
* **🎨 Arcane Aesthetic:** Features a custom "Comic Fantasy" visual theme with dynamic CSS animations and original card art.
* **🔒 Robust State Management:** Server-authoritative logic ensures fair play and prevents desyncs.

## 🛠️ Tech Stack
This project is a full-stack application designed for seamless deployment:

* **Frontend:** ![Angular](https://img.shields.io/badge/Angular-DD0031?style=flat&logo=angular&logoColor=white) **Angular 17+** (TypeScript, RxJS for reactive state)
* **Backend:** ![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=flat&logo=spring-boot&logoColor=white) **Java Spring Boot** (WebSocket/STOMP protocol)
* **Communication:** Real-time bi-directional events via **SockJS** & **STOMP**.
* **Styling:** Pure CSS3 with CSS Variables for theming (No heavy UI libraries).

## 🚀 How to Run (Local Dev)

### 1. Clone the repo
```bash
git clone [https://github.com/yourusername/brightcast-web.git](https://github.com/yourusername/brightcast-web.git)
```
### 2. Start the Backend
Open the `Brightcast` folder in IntelliJ IDEA and run `BrightcastApplication.java`.
* **Server starts on:** `http://localhost:8080`

### 3. Start the Frontend
Open the `brightcast-web` folder in a terminal:
```bash
npm install
ng serve
```
* **Client runs on:** `http://localhost:4200`

### 4. Play!
Open two browser tabs to `http://localhost:4200` to simulate a match!

## 📜 License
This project is a digital adaptation created for educational purposes. Original game concepts and art by **Brightcast Games LLC**.
