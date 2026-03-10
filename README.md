# Exercise 4: BDI Agents and Agents on the Web

This repository contains a [JaCaMo](https://github.com/jacamo-lang/jacamo) project where BDI agents are programmed in Jason/AgentSpeak to interact with their environment, including smart room devices via [W3C Web of Things (WoT) Thing Descriptions](https://www.w3.org/TR/wot-thing-description11/).

For the exercise instructions, see [EXERCISE.md](EXERCISE.md).

## Project Structure
```
├── wot-servient
│   ├── server.js               // WoT servient simulating smart room devices
│   ├── dashboard.html           // Dashboard for observing device states
│   └── tds/                     // Thing Descriptions (TTL files)
├── src
│   ├── agt
│   │   ├── warmup
│   │   │   └── simple_agent.asl           // Agent program for Warm-up
│   │   ├── task1
│   │   │   └── illuminance_controller.asl // Agent program for Task 1
│   │   ├── task2
│   │   │   ├── train_operator.asl         // Agent program for Task 2
│   │   │   ├── train_driver.asl           // Agent program for Task 2
│   │   │   └── train_controller.asl       // Agent program for Task 2
│   │   └── task3
│   │       ├── personal_assistant.asl     // Agent program for Task 3
│   │       └── barista.asl                // Agent program for Task 3 (Bonus)
│   └── env
│       ├── task1
│       │   ├── IlluminanceSensor.java     // Artifact: illuminance sensor
│       │   ├── LightBulb.java             // Artifact: light bulb
│       │   ├── Blinds.java                // Artifact: blinds
│       │   └── WeatherStation.java        // Artifact: weather station
│       └── task2
│           └── SchedulingArtifact.java    // Artifact: beat-based scheduler
├── warmup.jcm              // JaCaMo configuration for Warm-up
├── task1.jcm               // JaCaMo configuration for Task 1
├── task2.jcm               // JaCaMo configuration for Task 2
├── task3.jcm               // JaCaMo configuration for Task 3
└── build.gradle
```

## How to Run

All options require [Docker](https://www.docker.com/). The available Gradle tasks are `warmup`, `task1`, `task2`, and `task3`.

- **WoT servient dashboard** (Task 3): `http://localhost:5000`
- **JaCaMo GUI**: `http://localhost:6080` (via noVNC)
- **Jason Mind Inspector**: `http://localhost:3272` — inspect agent beliefs, plans, and intentions
- **Moise Organisation Inspector**: `http://localhost:3271` — inspect the organisation state
- **CArtAgO Inspector**: `http://localhost:3273` — inspect workspaces and artifacts

### Docker Compose (recommended)

```shell
# Build the containers (first time only)
docker compose build

# Run Warm-up (default)
docker compose up

# Run Task 1
TASK=task1 docker compose up

# Run Task 2
TASK=task2 docker compose up

# Run Task 3
TASK=task3 docker compose up
```

The `src/` directory and `.jcm` files are mounted as volumes, so you can edit them locally and restart:
```shell
docker compose down
docker compose up
```

### VS Code Dev Container

1. Install the [Dev Containers](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers) extension
2. Open this project and click "Reopen in Container"
3. Run a task from the terminal: `./run.sh task1`

### GitHub Codespaces

1. Click `Code` > `Codespaces` > `Create codespace on main`
2. Run a task from the terminal: `./run.sh task1`

### Running Locally (without Docker)

Requires **Java 21**. For Task 3, start the WoT servient first:

```shell
# With Docker
cd wot-servient && docker build -t wot-servient . && docker run -d --rm --name wot-servient -p 1180:1180 -p 5000:5000 wot-servient

# Or with Node.js
cd wot-servient && npm install && npm start
```

Then run the JaCaMo application:
```shell
./gradlew task1    # macOS/Linux
gradlew.bat task1  # Windows
```

## Documentation

- [Bordini et al. (2007). *Programming multi-agent systems in AgentSpeak using Jason.*](https://www.wiley.com/en-gb/Programming+Multi+Agent+Systems+in+AgentSpeak+using+Jason-p-9780470029008); Chapters 1–3
- [Boissier et al. (2020). *Multi-agent oriented programming.*](https://mitpress.mit.edu/9780262044578/); Chapters 3–7
- [CArtAgO by Exercises](https://github.com/cake-lier/cartago-by-exercises)
- [Default internal actions of Jason](https://www.emse.fr/~boissier/enseignement/maop12/doc/jason-api/jason/stdlib/package-summary.html)
