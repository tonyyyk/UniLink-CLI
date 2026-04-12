# UniLink — Study Partner Finding System

A command-line Java application for CityU Software Design course. UniLink helps university students find study partners based on complementary skills, exchange messages, and form study groups.

---

## Features

| Feature | Description |
|---|---|
| Authentication | Register and login with credentials saved to CSV |
| Profile Management | Set your Major, Strengths, and Weaknesses |
| Partner Matching | Algorithm ranks potential partners by compatibility score |
| Asynchronous Chat | Send and receive messages; unread alerts on login |
| Study Groups | Create, browse, and join study groups |
| Admin Panel | Admins can suspend or reinstate user accounts |

---

## Design Patterns

### 1. State Pattern
**Files:** [src/state/StudentState.java](src/state/StudentState.java), [src/state/NormalState.java](src/state/NormalState.java), [src/state/SuspendedState.java](src/state/SuspendedState.java)

Manages student account lifecycle. A `Student` delegates behavior to its current `StudentState`:

| Capability | NormalState | SuspendedState |
|---|---|---|
| Send messages | Yes | No |
| Appear in searches | Yes | No |
| Update profile | Yes | Yes |

### 2. Observer Pattern
**Files:** [src/observer/Observer.java](src/observer/Observer.java), [src/observer/Subject.java](src/observer/Subject.java), [src/manager/MessageManager.java](src/manager/MessageManager.java), [src/model/Student.java](src/model/Student.java)

`MessageManager` (Subject) holds a map of registered `Student` Observers. When a message is sent, it calls `notifyObservers()` which enqueues a notification on the recipient's `Student` object. The alert `[!] You have N unread message(s).` is displayed the next time the student reaches the main menu.

### 3. Strategy Pattern
**Files:** [src/strategy/MatchingStrategy.java](src/strategy/MatchingStrategy.java), [src/strategy/ComplementaryMatchingStrategy.java](src/strategy/ComplementaryMatchingStrategy.java), [src/strategy/SameMajorMatchingStrategy.java](src/strategy/SameMajorMatchingStrategy.java)

The matching algorithm can be swapped at runtime without changing the caller:

- **Complementary Matching** (default): 20% base + 20% same major + 30% per complementary skill pair
- **Same Major Matching**: 20% base + 40% same major + 15% per shared strength + 10% per shared weakness

### 4. Singleton Pattern
**Files:** [src/manager/UserManager.java](src/manager/UserManager.java), [src/manager/MessageManager.java](src/manager/MessageManager.java), [src/manager/GroupManager.java](src/manager/GroupManager.java)

Each manager has a private constructor and a static `getInstance()` method, ensuring exactly one instance manages each CSV file throughout the application lifetime.

### 5. Command Pattern
**Files:** [src/command/Command.java](src/command/Command.java), all files in [src/command/](src/command/), [src/ui/MenuBuilder.java](src/ui/MenuBuilder.java)

Each menu action is encapsulated as a `Command` object with `getLabel()` and `execute()`. `MenuBuilder` (the Invoker) only knows the `Command` interface — it renders the menu and dispatches calls without knowing what any command does. New features can be added without modifying `MenuBuilder`.

---

## Matching Algorithm

**Complementary Matching Strategy** (default):

```
Score = 20% (base)
      + 20% (if same major)
      + 30% × (number of complementary skill pairs)
      capped at 100%
```

A complementary pair means:
- Your weakness is in the partner's strengths, AND
- Their weakness is in your strengths

---

## Project Structure

```
Software Design GP/
├── src/
│   ├── Main.java                        # Entry point
│   ├── model/
│   │   ├── Student.java                 # Core user model + Observer
│   │   ├── Message.java                 # Chat message value object
│   │   └── StudyGroup.java              # Study group value object
│   ├── state/
│   │   ├── StudentState.java            # State Pattern interface
│   │   ├── NormalState.java             # Full access state
│   │   └── SuspendedState.java          # Restricted access state
│   ├── observer/
│   │   ├── Observer.java                # Observer interface
│   │   └── Subject.java                 # Subject interface
│   ├── strategy/
│   │   ├── MatchingStrategy.java        # Strategy interface
│   │   ├── ComplementaryMatchingStrategy.java
│   │   └── SameMajorMatchingStrategy.java
│   ├── command/
│   │   ├── Command.java                 # Command interface
│   │   ├── RegisterCommand.java
│   │   ├── LoginCommand.java
│   │   ├── ViewProfileCommand.java
│   │   ├── EditProfileCommand.java
│   │   ├── FindPartnersCommand.java
│   │   ├── SendMessageCommand.java
│   │   ├── ReadMessagesCommand.java
│   │   ├── CreateGroupCommand.java
│   │   ├── AdminPanelCommand.java
│   │   └── LogoutCommand.java
│   ├── manager/
│   │   ├── UserManager.java             # Singleton — manages users.csv
│   │   ├── MessageManager.java          # Singleton + Subject — manages messages.csv
│   │   └── GroupManager.java            # Singleton — manages groups.csv
│   └── ui/
│       ├── CLIHelper.java               # Input helpers and display utilities
│       └── MenuBuilder.java             # Command Pattern Invoker
├── data/                                # Auto-created at runtime
│   ├── users.csv
│   ├── messages.csv
│   └── groups.csv
└── out/                                 # Compiled .class files
```

---

## CSV File Formats

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

---

## How to Build and Run

### Compile
```bash
# From the project root
find src -name "*.java" | xargs javac -d out
```

### Run
```bash
java -cp out Main
```

### Default Admin Account
```
Username: admin
Password: admin123
```

---

## Requirements

- Java 11 or higher
- No external libraries or databases required — pure Java standard library only
