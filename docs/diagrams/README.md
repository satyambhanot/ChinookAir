# Diagrams

All UML for the design report is drawn in [draw.io](https://www.diagrams.net/)
(the desktop app or the web version). Sources go in `src/`, exports in
`export/`. The report uses the exports, so commit both.

```
docs/diagrams/
  src/      the .drawio files (the source of truth)
  export/   PNG exports, same file name as the source
```

Start every new diagram by copying `src/00-template.drawio`. It has a class
box, a lifeline and two swimlanes, so the style stays the same everywhere.

## File names

`NN-short-name.drawio`, lowercase, words joined with `-`. The tens digit says
what kind of diagram it is.

| Files | Diagram |
|---|---|
| `01-use-case` | Use-case diagram |
| `10-class-entities`, `11-class-control-boundary`, `12-class-refined`, `13-class-data` | Class diagram: first-cut entities, controls and boundaries, refined business layer, data layer |
| `20-activity-login`, `21-activity-browse-flight`, `22-activity-book-flight`, `23-activity-payment` | The four activity diagrams |
| `30-sequence-find-flights`, `31-sequence-book-flight`, `32-sequence-payment`, `33-sequence-manage-flights` | The four sequence diagrams |
| `40-state-system`, `41-state-payment`, `42-state-reservation` | The three state diagrams |
| `50-package` | Package diagram |

An export has the same name with `.png`, for example `export/01-use-case.png`.

## Style rules

- Font: Helvetica 12 everywhere. Do not shrink text to make a diagram fit;
  split the diagram instead.
- Stereotypes are written in lowercase between double angle brackets:
  `<<entity>>`, `<<control>>`, `<<boundary>>`, `<<interface>>`, `<<singleton>>`.
- Colours help reading but the stereotype carries the meaning, so the diagram
  must still read in black and white:
  - entity: fill `#dae8fc`, line `#6c8ebf`
  - control: fill `#d5e8d4`, line `#82b366`
  - boundary: fill `#fff2cc`, line `#d6b656`
- Multiplicity is written as `1`, `0..1`, `0..*` or `1..*`, on both ends of
  every association. Write `0..*`, not a bare `*`, as in the course slides.
- Visibility on class members: `+` public, `-` private, `#` protected, `~`
  package, `/` derived. Show types (`name: String`). Leave out getters,
  setters, constructors and library classes.
- Class diagrams: three compartments (name, attributes, operations); the class
  name starts with a capital letter. Abstract classes and abstract operations
  are in *italics* (`User`), static members are underlined
  (`DatabaseConnection.getInstance()`). Give an association a short verb label
  (`registers for`) and role names (nouns, next to the class they describe)
  where they add meaning; role names are required on a reflexive association.
  Add a navigability arrow only when one class must reach the other and not the
  reverse. An object is written `name: Class`, an anonymous one `:Class`.
- Use-case diagrams: stick-figure actors outside a rectangle that is the system
  boundary, labelled ovals inside it, a plain line between an actor and each
  use case it performs. `<<include>>` (from the base use case to the shared
  one) and `<<extend>>` (from the extension to the base use case) are dashed
  arrows with an open arrowhead. A use case name is short and describes a
  result of value to an actor, not just an action like "Search".
- Names match the [glossary](../glossary.md) and the code exactly: class names
  in `UpperCamelCase`, operations in `lowerCamelCase`, state names in
  `UPPER_SNAKE_CASE` (`PENDING_PAYMENT`). Use "Reservation", never "ticket" or
  "booking" (the Book Flight use case, `BookingController` and `BookingView`
  are the exceptions).
- Activity diagrams: swimlanes `User` and `System`, one start node (black dot),
  actions as rounded rectangles, decisions as diamonds with a guard on every
  arrow leaving them (`[valid]`, `[invalid]`), a thick bar for fork and join,
  and a final node (bull's-eye) at every end. Say in the title which use case
  the diagram documents (for example "UC-01 Login").
- Sequence diagrams: lifelines named `:ClassName`, the actor first, then
  boundary, control and entity classes left to right. One control class per
  diagram. Number the messages (`1 : searchFlights(criteria)`), use the
  operation names from the class diagram, draw activation bars, and show
  returns as dashed arrows (they may be left out only when the diagram is
  crowded). A message to the same object is a loop arrow. Creation is a dashed
  `<<create>>` arrow to the new object, deletion is `<<destroy>>` ending in an
  X.
- State diagrams: states as rounded rectangles, one initial node, a final node
  where the life cycle ends, transitions labelled `event [guard] / action`,
  states named as in the code.
- Package diagram (packages are drawn like folders): show the three layers (presentation, business, data), mark
  public classes with `+` and package-private ones with `~`.
- Arrows do not cross where it can be avoided. Keep one idea per diagram.

## Exporting

In draw.io: File > Export as > PNG, with these settings:

| Setting | Value |
|---|---|
| Zoom | 200% (2x) |
| Border width | 10 px |
| Background | white (untick "Transparent background") |
| Include a copy of my diagram | on |

From the command line with the desktop app installed (`brew install --cask drawio`; a harmless
"sandbox_extension" warning may print):

```bash
drawio -x -f png -s 2 -b 10 -o docs/diagrams/export/01-use-case.png docs/diagrams/src/01-use-case.drawio
```

The report is A4, so an export should be at least 1600 px wide and its text
readable when scaled to the page width. If it is not, the diagram has too
much on it.
