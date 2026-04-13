# UniLink — Study Partner Finding System

A Java application for university students to find study partners, exchange messages, and form study groups. Runs as either a **command-line app** or a **web app** served at `http://localhost:8080` — both modes share the same backend code and data files, with zero external dependencies.

---

## Features

| Feature | Description |
|---|---|
| Authentication | Register and login; credentials stored in CSV |
| Profile Management | Set your Major, Strengths, and Weaknesses |
| Partner Matching | Two algorithms rank partners by compatibility score |
| Messaging | Real-time chat; unread badge per contact; notification toasts are clickable to open the conversation |
| Study Groups | Create, browse, and join study groups |
| Report System | Report a user from partner cards or from inside a conversation; admins review and dismiss |
| Admin Panel | Suspend/reinstate accounts; Users tab + Reports tab |
| Settings | Change password or delete account (Account Security); edit profile (Manage Profile) |
| Help / Service Manual | In-app FAQ and how-to guide accessible from the sidebar |
| Web GUI | Full browser-based SPA — same features, no CLI required |

---

## Requirements

- **Java 17 or higher** (uses JDK built-in HTTP server — no external libraries)
- A modern web browser (Chrome, Firefox, Edge, Safari) for the web GUI
- No Maven, Gradle, npm, or any build tools needed

---

## How to Build and Run

### 1. Compile

From the **project root directory**, run:

```bash
javac -d out -sourcepath src $(find src -name "*.java" | tr '\n' ' ')
```

On Windows (Command Prompt):
```cmd
for /r src %f in (*.java) do @echo %f >> sources.txt
javac -d out @sources.txt
del sources.txt
```

Compiled `.class` files will be placed in the `out/` directory (created automatically).

---

### 2. Run — Web GUI Mode (recommended)

```bash
java -cp out Main --web
```

Then open your browser to: **http://localhost:8080**

The server runs until you press `Ctrl+C`.

**Default admin account:**
```
Username: admin
Password: admin123
```

---

### 3. Run — CLI Mode (original)

```bash
java -cp out Main
```

Launches the original interactive command-line interface. Identical features to the web GUI.

---

## Web GUI Pages

| Page | How to access | What it does |
|---|---|---|
| Login / Register | Landing screen | Create an account or sign in |
| My Profile | Sidebar → My Profile | View and edit your major, strengths, weaknesses |
| Find Partners | Sidebar → Find Partners | Search partners; Message or Report any result |
| Messages | Sidebar → Messages | Real-time chat; unread badge per contact; 🚩 Report button in conversation header |
| Study Groups | Sidebar → Study Groups | Browse all groups, see groups you joined, create a new group |
| Admin Panel | Sidebar → Admin Panel | (Admin only) Users tab: suspend/reinstate; Reports tab: review and dismiss |
| Settings | Sidebar → Settings | **Manage Profile** tab (major/strengths/weaknesses) + **Account Security** tab (change password, delete account) |
| Help / Manual | Sidebar → Help / Manual | FAQ, how-to guide, and instructions for contacting admin |

Live notifications (e.g. "You have a new message") appear as **clickable** toast popups — clicking opens the conversation directly. The browser polls every 5 seconds using the Observer pattern.

---

## Design Patterns

### 1. State Pattern
**Files:** [src/state/](src/state/)

Manages student account lifecycle. A `Student` delegates permission checks to its current `StudentState`:

| Capability | NormalState | SuspendedState |
|---|---|---|
| Send messages | Yes | No |
| Appear in searches | Yes | No |
| Update profile | Yes | Yes |

### 2. Observer Pattern
**Files:** [src/observer/](src/observer/), [src/manager/MessageManager.java](src/manager/MessageManager.java), [src/model/Student.java](src/model/Student.java)

`MessageManager` (Subject) holds registered `Student` Observers. When a message is sent, `notifyObservers()` enqueues a notification on the recipient's `Student` object.

- **CLI:** notifications are drained and printed at the top of each main menu loop
- **Web:** `GET /api/notifications/poll` drains the same queue every 5 seconds and returns JSON; the browser shows a toast popup

### 3. Strategy Pattern
**Files:** [src/strategy/](src/strategy/)

The matching algorithm is swapped at runtime by selecting a strategy:

| Strategy | Scoring |
|---|---|
| **Complementary** (default) | 20% base + 20% same major + 30% per complementary skill pair |
| **Same Major** | 20% base + 40% same major + 15% per shared strength + 10% per shared weakness |

### 4. Singleton Pattern
**Files:** [src/manager/](src/manager/)

`UserManager`, `MessageManager`, `GroupManager`, `ReportManager`, and `SessionManager` each expose a `getInstance()` method and use a private constructor, guaranteeing exactly one instance manages each resource.

### 5. Command Pattern
**Files:** [src/command/](src/command/), [src/ui/MenuBuilder.java](src/ui/MenuBuilder.java)

Every CLI menu action is encapsulated as a `Command` object with `getLabel()` and `execute()`. `MenuBuilder` (the Invoker) renders menus and dispatches calls without knowing what any command does.

---

## Project Structure

```
project root/
├── src/
│   ├── Main.java                          # Entry point (--web flag or CLI)
│   ├── model/
│   │   ├── Student.java                   # Core user model + Observer
│   │   ├── Message.java                   # Message value object
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
│   │   ├── Command.java
│   │   ├── LoginCommand.java
│   │   ├── RegisterCommand.java
│   │   ├── ViewProfileCommand.java
│   │   ├── EditProfileCommand.java
│   │   ├── FindPartnersCommand.java
│   │   ├── SendMessageCommand.java
│   │   ├── ReadMessagesCommand.java
│   │   ├── CreateGroupCommand.java
│   │   ├── AdminPanelCommand.java
│   │   └── LogoutCommand.java
│   ├── manager/
│   │   ├── UserManager.java               # Singleton — users.csv
│   │   ├── MessageManager.java            # Singleton + Subject — messages.csv
│   │   ├── GroupManager.java              # Singleton — groups.csv
│   │   └── ReportManager.java             # Singleton — reports.csv
│   ├── ui/
│   │   ├── CLIHelper.java                 # Input helpers and display utilities
│   │   └── MenuBuilder.java               # Command pattern Invoker
│   └── server/                            # Web GUI backend (new)
│       ├── ApiServer.java                 # Creates HttpServer, registers all routes
│       ├── SessionManager.java            # Token → Student session map (Singleton)
│       ├── JsonUtil.java                  # Manual JSON serialisation / parsing
│       ├── BaseHandler.java               # Shared: auth, CORS, request/response helpers
│       ├── StaticFileHandler.java         # Serves web/ directory
│       ├── AuthHandler.java               # /api/auth/login, register, logout
│       ├── ProfileHandler.java            # GET/PUT /api/profile
│       ├── MatchHandler.java              # GET /api/match?strategy=
│       ├── MessageHandler.java            # /api/messages/* endpoints
│       ├── NotifyHandler.java             # GET /api/notifications/poll
│       ├── GroupHandler.java              # /api/groups/* endpoints
│       ├── AdminHandler.java              # /api/admin/* endpoints
│       ├── ReportHandler.java             # /api/reports/* endpoints
│       └── SettingsHandler.java           # /api/settings/password, /api/settings/delete
├── web/                                   # Browser SPA (served by StaticFileHandler)
│   ├── index.html                         # Single HTML shell — all pages as divs
│   ├── style.css                          # Full UI styles (sidebar, chat, cards)
│   └── app.js                             # All JS: routing, API calls, polling
├── data/                                  # Auto-created on first run
│   ├── users.csv
│   ├── messages.csv
│   ├── groups.csv
│   └── reports.csv
└── out/                                   # Compiled .class files (after javac)
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
| GET | `/api/profile` | Get current user's profile |
| PUT | `/api/profile` | Update `{major, strengths, weaknesses}` |
| GET | `/api/match?strategy=` | `complementary` or `samemajor` — returns ranked list |
| GET | `/api/messages/contacts` | List of all users to message |
| GET | `/api/messages/unread-count` | `{count}` |
| GET | `/api/messages/conversation?with=` | Full chat history with a user |
| POST | `/api/messages/send` | Send `{to, content}` |
| GET | `/api/notifications/poll` | Drain Observer queue, returns `{notifications:[...]}` |
| GET | `/api/groups` | All groups with membership flag |
| GET | `/api/groups/mine` | Groups the current user belongs to |
| POST | `/api/groups/create` | Create `{name, topic}` |
| POST | `/api/groups/join` | Join `{groupId}` |
| GET | `/api/admin/users` | All users (admin only) |
| POST | `/api/admin/suspend` | Suspend `{username}` (admin only) |
| POST | `/api/admin/reinstate` | Reinstate `{username}` (admin only) |
| POST | `/api/reports/submit` | Submit `{reported, reason}` — any logged-in user |
| GET | `/api/reports` | All reports (admin only) |
| POST | `/api/reports/dismiss` | Dismiss `{id}` (admin only) |
| POST | `/api/settings/password` | Change own password `{oldPassword, newPassword}`; invalidates session |
| POST | `/api/settings/delete` | Delete own account `{password}`; invalidates session |

---

## Data File Formats

**data/users.csv**
```
username,password,major,strengths,weaknesses,role,status
admin,admin123,-,-,-,ADMIN,NORMAL
alice,pass123,CS,Java;Math,Networks,USER,NORMAL
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

**data/reports.csv**
```
id,reporter,reported,reason,timestamp,status
1,alice,bob,Inappropriate messages,2026-04-13T18:27:00,PENDING
```

Data files are created automatically with seed data on first run.

---

## Troubleshooting

**Port 8080 already in use**
```bash
# macOS / Linux — find and kill the process using port 8080
lsof -ti:8080 | xargs kill
```

**`javac` not found**
Make sure Java JDK (not just JRE) is installed and `JAVA_HOME` / `PATH` is set correctly.
```bash
java -version    # should print 17 or higher
javac -version   # should match
```

**`find` command not available (Windows)**
Use the Windows compile command shown in the Build section above, or use an IDE (IntelliJ IDEA, Eclipse, VS Code with Java Extension Pack) to compile and run.
# UniLink-GUI
