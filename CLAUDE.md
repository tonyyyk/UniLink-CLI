# Role

Act as a Senior Java Software Engineer and Academic Tutor. I need you to help me build a complete, runnable Java application for my university Software Design course project called "UniLink" (a study partner finding system).

# STRICT TECHNICAL CONSTRAINTS (CRITICAL)

1. **Language:** Pure Java only.
2. **Interface:** Command Line Interface (CLI) ONLY. Do not use Swing, JavaFX, Spring Boot, or any Web frameworks. All interactions must use `Scanner` and `System.out.println`.
3. **Database:** Simple File I/O ONLY (e.g., CSV or TXT files). Do NOT use SQL, MongoDB, or any database servers. Use `FileWriter` and `BufferedReader` to persist data.

# Required Design Patterns to Implement

1. **State Pattern:** Implement this within the `Student` class using a `StudentState` interface with `NormalState` and `SuspendedState`. A suspended student cannot send messages or appear in partner searches, but they can update their profile.
2. **Observer Pattern:** Implement this for an asynchronous Chat Notification System. A `MessageManager` (handling File I/O) acts as the Subject. `Student` objects act as Observers. When a message is appended to the CSV, the manager notifies the receiving student so they see an alert (e.g., "[!] You have a new message") upon returning to the main CLI menu.

# Core Features to Implement

1. **Authentication:** CLI register and login, saving credentials to `users.csv`.
2. **Profile Management:** Users can input/update their Major, and lists of Strengths and Weaknesses.
3. **Matching Algorithm:** A CLI menu option to find partners. Base score is 20%. Add +20% for the same major. Add +30% for each complementary skill (User's weakness matches Partner's strength, and vice versa). Display a sorted list of matches.
4. **Asynchronous Chat:** Users can send a text message to a matched partner. This saves to `messages.csv` (Format: Sender,Receiver,Timestamp,Content). They can also read their chat history.

# Output Requirements

Please provide the complete, functional Java source code separated into logical classes (e.g., `Main.java`, `Student.java`, `MessageManager.java`, State interfaces, etc.). Include brief inline comments explaining the File I/O and Design Patterns so I can explain them during my presentation.
