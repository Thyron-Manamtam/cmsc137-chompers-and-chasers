# Chompers & Chasers
**CMSC 137 — Networked Game Project in Java**  
Second Semester AY 2025-2026 | Milestone 1 (Single-Player)

---

## Table of Contents
1. [Prerequisites](#1-prerequisites)
2. [VS Code Setup](#2-vs-code-setup)
3. [Project Setup](#3-project-setup)
4. [How to Run](#4-how-to-run)
5. [Controls](#5-controls)
6. [Gameplay](#6-gameplay)
7. [Project Structure](#7-project-structure)
8. [OOP Design](#8-oop-design)
9. [Scoring](#9-scoring)
10. [Milestone 2 Plan](#10-milestone-2-plan-networked-multiplayer)

---

## 1. Prerequisites

### Install Java Development Kit (JDK)

You need **JDK 11 or later** installed. Check if you already have it:

```bash
java -version
javac -version
```

If either command says "not found", install the JDK:

**Windows:**
- Download from https://adoptium.net (Temurin JDK 21 recommended)
- Run the `.msi` installer
- Make sure to check **"Set JAVA_HOME"** and **"Add to PATH"** during installation
- Restart your PC after installing

**macOS:**
```bash
# Using Homebrew (recommended)
brew install openjdk@21

# Then add to PATH (add this line to your ~/.zshrc or ~/.bash_profile)
export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt update
sudo apt install default-jdk
```

After installing, verify it works:
```bash
java -version
# Should print something like: openjdk version "21.0.x"

javac -version
# Should print: javac 21.0.x
```

---

## 2. VS Code Setup

### Required Extensions

Install these extensions in VS Code (Ctrl+Shift+X to open Extensions):

1. **Extension Pack for Java** — by Microsoft
   - Search: `vscjava.vscode-java-pack`
   - This installs 6 extensions in one click:
     - Language Support for Java (Red Hat)
     - Debugger for Java
     - Test Runner for Java
     - Maven for Java
     - Project Manager for Java
     - IntelliSense for Java

> **Tip:** After installing the extension pack and JDK, fully close and reopen VS Code. It needs to detect your Java installation fresh.

### Verify VS Code Sees Your Java

1. Open VS Code
2. Press `Ctrl+Shift+P` (or `Cmd+Shift+P` on Mac)
3. Type `Java: Configure Java Runtime`
4. You should see your JDK listed under **"Project JDKs"**

If it shows "No JDK found", your JDK is not on PATH. Fix:
- Windows: Search "Environment Variables" in Start Menu → Edit `Path` → Add your JDK's `bin` folder (e.g., `C:\Program Files\Eclipse Adoptium\jdk-21\bin`)
- Mac/Linux: Make sure the `export PATH=...` line is in your shell config file and you've restarted the terminal

---

## 3. Project Setup

### Folder Structure

After unzipping the source, your folder should look like this:

```
cmsc137-chompers-and-chasers/
├── src/
│   ├── main/
│   │   └── Main.java
│   ├── controller/
│   │   └── GameController.java
│   ├── model/
│   │   ├── Entity.java
│   │   ├── Movable.java
│   │   ├── Player.java
│   │   ├── Chaser.java
│   │   ├── Maze.java
│   │   └── Pellet.java
│   ├── view/
│   │   ├── GameWindow.java
│   │   ├── GamePanel.java
│   │   └── GameRenderer.java
│   ├── util/
│   │   ├── Direction.java
│   │   ├── GameState.java
│   │   ├── Role.java
│   │   └── GameConfig.java
│   └── network/
│       ├── GameServer.java       ← Milestone 2 stub
│       └── GameClient.java       ← Milestone 2 stub
└── README.md
```

### Opening in VS Code

1. Open VS Code
2. Go to **File → Open Folder**
3. Select the `cmsc137-chompers-and-chasers/` root folder (the one containing `src/`)
4. Wait a few seconds — VS Code will scan the project and the Java extension will initialize (you'll see a loading bar at the bottom)

### If VS Code shows red errors on import statements

This usually means:
- Java extension is still loading — wait 10–20 seconds
- JDK not detected — check Step 2 above
- Wrong folder opened — make sure you opened the **root folder**, not the `src/` subfolder

---

## 4. How to Run


### Compile and Run from VS Code

**Method 1: Run button (easiest in VS Code)**

1. Open `src/main/Main.java`
2. Click the **▷ Run** button that appears above the `main` method
3. The game window will open

**Method 2: Terminal (manual compile)**

Open a terminal inside VS Code (`Ctrl+\``) and run:

```bash
# Windows
mkdir out
javac -d out -sourcepath src src\main\Main.java
java -cp out main.Main

# Mac / Linux
mkdir out
javac -d out -sourcepath src src/main/Main.java
java -cp out main.Main
```

**Method 3: Rebuild the JAR yourself**

```bash
# Compile
mkdir out
javac -d out -sourcepath src $(find src -name "*.java")   # Mac/Linux
# OR on Windows PowerShell:
Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName } | Out-File sources.txt
javac -d out @sources.txt

# Package
echo Main-Class: main.Main > manifest.txt
jar cfm ChompersAndChasers.jar manifest.txt -C out .

# Run
java -jar ChompersAndChasers.jar
```

---

## 5. Controls

| Key | Action |
|-----|--------|
| `ENTER` | Start game from title screen |
| `↑ ↓ ← →` | Move the Chomper |
| `P` | Pause / Resume |
| `R` | Restart (works at any time — mid-game, game over, or win screen) |
| `ESC` | Quit |

---

## 6. Gameplay

The **Chomper** (yellow) is controlled by you.  
Four **AI Chasers** (red ghosts) hunt you down using intelligent BFS pathfinding.

**Goal:** Collect every pellet on the board without getting caught.

**Power Pellets** — the 4 large orange dots in the corners:
- Collecting one **frightens all Chasers** (they turn blue) for ~4 seconds
- While they're frightened, **walk into them to eat them** for bonus points
- Frightened Chasers flash white just before they recover — watch out!

**Lives:** You start with 3 lives. Getting caught costs one life and respawns all Chasers.

**Win condition:** Collect every pellet on the board.  
**Lose condition:** Run out of lives.

---

## 7. Project Structure

```
src/
├── main/
│   └── Main.java               Entry point — calls GameWindow.launch()
│
├── controller/
│   └── GameController.java     Owns the game loop (Swing Timer), state
│                               machine, and all logic. View reads from
│                               here; only controller mutates state.
│
├── model/                      Pure game logic — no Swing imports
│   ├── Entity.java             Abstract base class: row, col, spawnRow,
│   │                           spawnCol, respawn(), collidesWith()
│   ├── Movable.java            Interface: move(Maze), getRow(), getCol()
│   ├── Player.java             Extends Entity. Handles movement with
│   │                           buffered direction input, lives, score,
│   │                           pellet collection, and power state.
│   ├── Chaser.java             Extends Entity. AI ghost with three modes:
│   │                           CHASE (BFS), FRIGHTENED (random), EATEN.
│   ├── Maze.java               15×15 grid. Provides isWall(), pellet
│   │                           collection, and pellet count.
│   └── Pellet.java             NORMAL (10pts) or POWER (50pts) type.
│
├── view/
│   ├── GameWindow.java         JFrame wrapper. Centers window on screen.
│   ├── GamePanel.java          JPanel canvas + KeyListener. Delegates all
│   │                           drawing to GameRenderer.
│   └── GameRenderer.java       Stateless renderer. Draws maze, pellets,
│                               player (animated mouth), ghosts, HUD, and
│                               all screen overlays.
│
├── util/
│   ├── Direction.java          Enum: UP / DOWN / LEFT / RIGHT / NONE
│   │                           Each carries dRow, dCol deltas.
│   ├── GameState.java          Enum: START / PLAYING / PAUSED /
│   │                           DEAD / GAME_OVER / WIN
│   ├── Role.java               Enum: CHOMPER / CHASER
│   └── GameConfig.java         All constants in one place: tile size,
│                               tick speed, lives, scores, power duration,
│                               and Milestone 2 network constants.
│
└── network/
    ├── GameServer.java         [Milestone 2 stub — documented]
    └── GameClient.java         [Milestone 2 stub — documented]
```

---

## 8. OOP Design

| OOP Principle | Where Applied |
|---------------|--------------|
| **Inheritance** | `Player` and `Chaser` both extend abstract `Entity` — shared grid position and respawn logic |
| **Interface** | `Movable` defines the contract `move(Maze)`, `getRow()`, `getCol()` |
| **Encapsulation** | Model objects expose only getters to the view; all mutation goes through methods |
| **Abstraction** | `Entity` hides coordinate bookkeeping; subclasses only define their own behavior |
| **Separation of Concerns** | `GameController` (logic) · `GameRenderer` (drawing) · `GamePanel` (input) |
| **Enum Types** | `Direction`, `GameState`, `Role`, `Pellet.Type`, `Chaser.Mode` — no magic strings/ints |
| **Single Source of Truth** | `GameConfig` holds every constant; nothing is hardcoded elsewhere |

---

## 9. Scoring

| Event | Points |
|-------|--------|
| Collect normal pellet | 10 |
| Collect power pellet | 50 |
| Eat 1st Chaser (in one power) | 200 |
| Eat 2nd Chaser | 400 |
| Eat 3rd Chaser | 800 |
| Eat 4th Chaser | 1,600 |

---

## 10. Milestone 2 Plan (Networked Multiplayer)

The codebase is already structured for M2:

- **Model classes have zero Swing imports** — they can run as-is on a headless server
- `GameConfig` already contains `SERVER_PORT`, `MIN_PLAYERS`, `MAX_PLAYERS`
- `Role.java` is wired into `Player` — the server just needs to assign roles upon connection
- `network/GameServer.java` and `network/GameClient.java` are documented stubs

**M2 additions needed:**
- `GameServer` — `ServerSocket` loop, one `ClientHandler` thread per connected player, broadcasts `GameSnapshot` each tick
- `GameClient` — connects via TCP, shows lobby until 4 players join, sends `Direction` input, renders received state
- `GameSnapshot` — serializable object (JSON or custom binary) containing maze, all player positions, scores, game state
- Role assignment: server picks Chomper/Chaser distribution (1-3 or 2-2) and tells each client their role
- `GameController` logic moves to the server; client becomes a thin renderer only