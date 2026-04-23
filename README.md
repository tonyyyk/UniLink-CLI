# UniLink — Study Partner Finding System

A Java application for university students to find study partners, exchange messages, and form study groups. Runs as either a **command-line app** or a **web app** served at `http://localhost:8080` — both modes share the same backend code and data files, with zero external dependencies.

---

## Requirements

- **Java 24 or higher** (uses the JDK built-in HTTP server — no external libraries needed)
- A modern web browser (Chrome, Firefox, Edge, Safari) for the web GUI

> Run `java -version` to check your version.

---

## How to Run

### 1. Web GUI Mode

```bash
java -cp out Main --web
```

Open your browser to: **http://localhost:8080**

Press `Ctrl+C` to stop the server.

**Default admin account:**
```
Username: admin
Password: admin123
```

**Demo accounts (seeded on first run):**
```
alice / alice123  — CS,           Strengths: Java, Algorithms
bob   / bob123    — Mathematics,  Strengths: Calculus, Statistics
carol / carol123  — Physics,      Strengths: Mathematics, Lab Work
dave  / dave123   — CS,           Strengths: Networking, Databases
emma  / emma123   — Data Science, Strengths: Python, Statistics, Machine Learning
```

> **Multi-account testing:** Open multiple browser tabs — each tab keeps its own independent session.

---
