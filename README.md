# UniLink — Study Partner Finding System

A Java application for university students to find study partners, exchange messages, and form study groups. Runs as either a **command-line app** or a **web app** served at `http://localhost:8080` — both modes share the same backend code and data files, with zero external dependencies.

---

## Branches

| Branch | Purpose |
|---|---|
| `main` | Full application source code |
| `testing` | JUnit 5 test suite (69 tests covering all design patterns) |

---

## Features

| Feature | Description |
|---|---|
| Authentication | Register and login; credentials stored in CSV |
| Profile Management | Set Major, Strengths, Weaknesses, Date of Birth, Gender, Hobbies, and Introduction |
| Partner Matching | Two algorithms rank partners by compatibility score |
| Messaging | Real-time 1-to-1 chat; unread badge per contact; clickable notification toasts |
| Study Groups | Create, browse, and join groups; group chat available after joining |
| Group Chat | Live chat inside any group you are a member of; polls every 3 seconds |
| Report System | Report a user from partner cards or inside a conversation; admins review and dismiss |
| Admin Panel | Suspend/reinstate accounts; Users tab + Reports tab |
| Settings | Change password or delete account; edit profile fields |
| Help / Service Manual | In-app FAQ and how-to guide |
| Web GUI | Full browser-based SPA; each tab holds an independent session |
| Demo Accounts | Five seed users created on first run for demonstration |

---

## Requirements

- **Java 11 or higher** (uses the JDK built-in HTTP server — no external libraries needed)
- A modern web browser (Chrome, Firefox, Edge, Safari) for the web GUI

> Run `java -version` to check your version.

---

## How to Build and Run

### 1. Compile

From the **project root directory**:

```bash
javac -d out -sourcepath src $(find src -name "*.java" | tr '\n' ' ')
```

On Windows (Command Prompt):
```cmd
for /r src %f in (*.java) do @echo %f >> sources.txt
javac -d out @sources.txt
del sources.txt
```

---

### 2. Run — Web GUI Mode (recommended)

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

### 3. Run — CLI Mode

```bash
java -cp out Main
```

Launches the original interactive command-line interface.

---

## Web GUI Pages

| Page | How to access | What it does |
|---|---|---|
| Login / Register | Landing screen | Create an account or sign in |
| My Profile | Sidebar → My Profile | View and edit all profile fields including hobbies and introduction |
| Find Partners | Sidebar → Find Partners | Search partners by strategy; Message or Report any result |
| Messages | Sidebar → Messages | Real-time 1-to-1 chat with unread badge and 🚩 Report button |
| Study Groups | Sidebar → Study Groups | Browse all groups; join a group; open group chat via 💬 Chat button |
| Admin Panel | Sidebar → Admin Panel | (Admin only) Suspend/reinstate users; review reports |
| Settings | Sidebar → Settings | Change password, delete account, edit profile |
| Help / Manual | Sidebar → Help / Manual | FAQ and how-to guide |

---

## Design Patterns

### 1. State Pattern
**Files:** [src/state/](src/state/)

Manages student account lifecycle. `Student` delegates permission checks to its current `StudentState`:

| Capability | NormalState | SuspendedState |
|---|---|---|
| Send messages | Yes | No |
| Appear in searches | Yes | No |
| Update profile | Yes | Yes |

### 2. Observer Pattern
**Files:** [src/observer/](src/observer/), [src/manager/MessageManager.java](src/manager/MessageManager.java), [src/model/Student.java](src/model/Student.java)

`MessageManager` (Subject) holds registered `Student` Observers. When a message is sent, `notifyObservers()` enqueues a notification on the recipient's `Student` object.

- **CLI:** drained and printed at the top of each main menu loop
- **Web:** `GET /api/notifications/poll` drains the queue every 5 seconds; the browser shows a clickable toast popup

### 3. Strategy Pattern
**Files:** [src/strategy/](src/strategy/)

The matching algorithm is swapped at runtime:

| Strategy | Scoring |
|---|---|
| **Complementary** (default) | 20% base + 20% same major + 30% per complementary skill pair |
| **Same Major** | 20% base + 40% same major + 15% per shared strength + 10% per shared weakness |

Both scores are capped at 100%.

### 4. Singleton Pattern
**Files:** [src/manager/](src/manager/)

`UserManager`, `MessageManager`, `GroupManager`, `GroupMessageManager`, `ReportManager`, and `SessionManager` each expose a `getInstance()` method with a private constructor, guaranteeing exactly one instance manages each resource.

### 5. Command Pattern
**Files:** [src/command/](src/command/), [src/ui/MenuBuilder.java](src/ui/MenuBuilder.java)

Every CLI menu action is a `Command` object with `getLabel()` and `execute()`. `MenuBuilder` (the Invoker) renders menus and dispatches calls without knowing what any command does.

---

## Testing (branch: `testing`)

Switch to the `testing` branch to access the JUnit 5 test suite:

```bash
git checkout testing
```

**Test files:**

| File | Tests | Covers |
|---|---|---|
| `test/state/StatePatternTest.java` | 8 | NormalState / SuspendedState permissions |
| `test/model/StudentTest.java` | 15 | CSV round-trip, state transitions, Observer queue |
| `test/model/StudyGroupTest.java` | 8 | CSV round-trip, membership add/dedup |
| `test/model/MessageTest.java` | 7 | CSV round-trip, read flag, timestamp format |
| `test/strategy/MatchingStrategyTest.java` | 10 | Scoring formulas, 100% cap, strategy swap |
| `test/manager/UserManagerTest.java` | 11 | Singleton, register/login, suspend/reinstate |
| `test/manager/GroupManagerTest.java` | 10 | Singleton, create/join group, member filtering |

**To run the tests**, install [Maven](https://maven.apache.org) and run from the project root:

```bash
mvn test
```

---

## Project Structure

```
project root/
├── src/
│   ├── Main.java                          # Entry point (--web flag or CLI)
│   ├── model/
│   │   ├── Student.java                   # Core user model + Observer
│   │   ├── Message.java                   # 1-to-1 message value object
│   │   ├── GroupMessage.java              # Group chat message value object
│   │   ├── StudyGroup.java                # Study group value object
│   │   └── Report.java                    # Report value object
│   ├── state/
│   │   ├── StudentState.java              # State interface
│   │   ├── NormalState.java               # Full-access state
│   │   └── SuspendedState.java            # Restricted state
│   ├── observer/
│   │   ├── Observer.java                  # Observer interface
│   │   └── Subject.java                   # Subject interface
│   ├── strategy/
│   │   ├── MatchingStrategy.java          # Strategy interface
│   │   ├── ComplementaryMatchingStrategy.java
│   │   └── SameMajorMatchingStrategy.java
│   ├── command/                           # One class per CLI menu action
│   ├── manager/
│   │   ├── UserManager.java               # Singleton — users.csv
│   │   ├── MessageManager.java            # Singleton + Subject — messages.csv
│   │   ├── GroupManager.java              # Singleton — groups.csv
│   │   ├── GroupMessageManager.java       # Singleton — group_messages.csv
│   │   └── ReportManager.java             # Singleton — reports.csv
│   ├── ui/
│   │   ├── CLIHelper.java
│   │   └── MenuBuilder.java               # Command pattern Invoker
│   └── server/
│       ├── ApiServer.java                 # Registers all HTTP routes
│       ├── SessionManager.java            # Token → Student session map
│       ├── JsonUtil.java                  # JSON serialisation / parsing
│       ├── BaseHandler.java               # Auth, CORS, request helpers
│       ├── StaticFileHandler.java         # Serves web/ directory
│       ├── AuthHandler.java               # /api/auth/*
│       ├── ProfileHandler.java            # /api/profile
│       ├── MatchHandler.java              # /api/match
│       ├── MessageHandler.java            # /api/messages/*
│       ├── GroupHandler.java              # /api/groups/*
│       ├── GroupMessageHandler.java       # /api/groups/chat
│       ├── NotifyHandler.java             # /api/notifications/poll
│       ├── AdminHandler.java              # /api/admin/*
│       ├── ReportHandler.java             # /api/reports/*
│       └── SettingsHandler.java           # /api/settings/*
├── web/
│   ├── index.html                         # Single HTML shell
│   ├── style.css                          # Full UI styles
│   └── app.js                             # Routing, API calls, polling
├── data/                                  # Auto-created on first run
│   ├── users.csv
│   ├── messages.csv
│   ├── groups.csv
│   ├── group_messages.csv
│   └── reports.csv
└── out/                                   # Compiled .class files
```

---

## REST API Reference (Web Mode)

All endpoints except login/register require:
```
Authorization: Bearer <token>
```

| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/login` | Login; returns `{token, username, role, status}` |
| POST | `/api/auth/register` | Register `{username, password, major}` |
| POST | `/api/auth/logout` | Invalidate session |
| GET | `/api/profile` | Get current user's full profile |
| PUT | `/api/profile` | Update `{major, strengths, weaknesses, dateOfBirth, gender, hobbies, introduction}` |
| GET | `/api/match?strategy=` | `complementary` or `samemajor` — returns ranked list |
| GET | `/api/messages/contacts` | All users with unread count |
| GET | `/api/messages/unread-count` | `{count}` |
| GET | `/api/messages/conversation?with=` | Full chat history with a user |
| POST | `/api/messages/send` | Send `{to, content}` |
| GET | `/api/notifications/poll` | Drain Observer queue; returns `{notifications:[...]}` |
| GET | `/api/groups` | All groups with membership flag |
| GET | `/api/groups/mine` | Groups the current user belongs to |
| POST | `/api/groups/create` | Create `{name, topic}` |
| POST | `/api/groups/join` | Join `{groupId}` |
| GET | `/api/groups/chat?groupId=` | Get group chat messages (members only) |
| POST | `/api/groups/chat` | Send `{groupId, content}` (members only) |
| GET | `/api/admin/users` | All users (admin only) |
| POST | `/api/admin/suspend` | Suspend `{username}` (admin only) |
| POST | `/api/admin/reinstate` | Reinstate `{username}` (admin only) |
| POST | `/api/reports/submit` | Submit `{reported, reason}` |
| GET | `/api/reports` | All reports (admin only) |
| POST | `/api/reports/dismiss` | Dismiss `{id}` (admin only) |
| POST | `/api/settings/password` | Change password `{oldPassword, newPassword}` |
| POST | `/api/settings/delete` | Delete account `{password}` |

---

## Data File Formats

**data/users.csv**
```
username,password,major,strengths,weaknesses,role,status[,dateOfBirth,gender,hobbies,introduction]
admin,admin123,-,-,-,ADMIN,NORMAL
alice,alice123,CS,Java;Algorithms,Networks,USER,NORMAL,2000-05-15,Female,Reading;Hiking,Hi everyone
```

**data/messages.csv**
```
sender,receiver,timestamp,content,read
alice,bob,2026-04-12T10:30:00,Hello lets study together,false
```

**data/groups.csv**
```
groupId,groupName,creator,members,topic
1,CS Study Group,alice,"alice;bob",Algorithms
```

**data/group_messages.csv**
```
groupId,sender,timestamp,content
1,alice,2026-04-14T10:30:00,Hello everyone in the group!
```

**data/reports.csv**
```
id,reporter,reported,reason,timestamp,status
1,alice,bob,Inappropriate messages,2026-04-13T18:27:00,PENDING
```

---

## Troubleshooting

**Port 8080 already in use**
```bash
lsof -ti:8080 | xargs kill
```

**`javac` not found**
```bash
java -version    # should print 11 or higher
javac -version   # should match
```
Install JDK from [Adoptium (Temurin)](https://adoptium.net) — free, works on Windows/macOS/Linux.

**`find` command not available (Windows)**
Use the Windows compile command shown above, or compile via an IDE (IntelliJ IDEA, Eclipse, VS Code with Java Extension Pack).
