# Contributing

A few rules so we can all work on this without stepping on each other.

## Workflow

1. Pull `main` before you start.
2. Make a branch for your task:
   - `feature/booking-controller`
   - `fix/seat-double-booking`
   - `docs/sequence-diagrams`
   - `chore/...` for build/tooling stuff
3. Commit small and often - one change per commit.
4. Run `mvn verify` before pushing. It runs Checkstyle and the tests.
5. Open a PR into `main` and get one teammate to review it.
6. Don't push straight to `main`. CI has to pass before merging.

## Commit messages

Short, present tense, say what changed:

- `Add Reservation entity`
- `Fix seat count after cancelling`
- `Update class diagram with Airline`

Not `stuff`, `changes`, or `final final v2`.

## Code layout

Each layer has its own package under `ca.ucalgary.chinookair`:

| Package | What goes in it | Name it like |
|---|---|---|
| `presentation` | Swing screens | `FlightSearchView` |
| `business.control` | one controller per use case | `BookingController` |
| `business.entity` | domain classes | `Reservation`, `Flight` |
| `data` | DB connection and DAOs | `FlightDao`, `SqliteFlightDao` |

Rules between layers:

- Views only call controllers. No SQL or DAO calls in a view.
- Entities don't know about Swing or the database.
- Controllers talk to the data layer through the DAO interfaces, not the
  SQLite classes directly.

If you add a class, make sure it shows up in the class diagram too
(and in a use-case scenario). We lose marks for classes that can't be traced.

## Naming

- Use **Reservation** everywhere, not booking or ticket, so the code
  matches the design document.
- Classes `PascalCase`, methods and variables `camelCase`,
  constants `UPPER_SNAKE_CASE`.

## Style

- 4 spaces, no tabs, max 120 chars per line. `.editorconfig` sets this up
  in most IDEs automatically.
- Always use braces, even for one-line `if`s.
- No wildcard imports.
- Comments should explain *why*, not repeat what the code says.

Checkstyle (`checkstyle.xml`) enforces most of this. If it fails, the
console tells you the file and line.

## Tests

Anything with logic (controllers, entities, DAOs) gets a JUnit test in
`src/test/java`, in the same package as the class. Screens don't need tests.
