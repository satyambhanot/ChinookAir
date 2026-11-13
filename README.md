# ChinookAir

Flight reservation system for the ENSF 614 term project (Group 15).

A standalone desktop app in Java (Swing + SQLite). You log in as a customer,
flight agent or admin and get the screens for that role.

## Build and run

Needs JDK 17+ and Maven.

```
mvn clean package
java -jar target/ChinookAir.jar
```

## Demo accounts

The first start creates a database in `~/.chinookair/chinookair.db` with demo
flights for the next 60 days, 12 promotions and three accounts. Move the file
with `-Dchinookair.db=/path/to/file.db`.

| Role | Username | Password |
|---|---|---|
| Customer | `customer1` | `customer123` |
| Flight agent | `agent1` | `agent123` |
| Administrator | `admin` | `admin123` |

Passwords are stored as PBKDF2 hashes, never as text.

## Project layout

Three layers, one package each:

```
src/main/java/ca/ucalgary/chinookair/
├── ChinookAir.java          entry point
├── presentation/            Swing screens (boundary)
├── business/
│   ├── control/             controllers, one per use case
│   └── entity/              domain classes
└── data/                    SQLite connection + DAOs
docs/                        design document and UML diagrams
```

Screens call controllers, controllers use entities and DAOs.
Nothing in `presentation` should touch the database directly.

## Working on it

Read [CONTRIBUTING.md](CONTRIBUTING.md) before your first PR - it covers
branches, commit messages and the style rules Checkstyle checks.
