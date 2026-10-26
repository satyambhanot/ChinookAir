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
