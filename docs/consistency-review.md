# Consistency review of the design report (P4-04)

One pass over the diagrams and the text of the report, done before the design
is frozen (P4-05). Every check was run against the sources in
`docs/diagrams/src` and `docs/report/sections`, not against screenshots.

## Checklist

| # | Check | How | Result |
|---|---|---|---|
| 1 | Every operation sent in a sequence diagram exists on the receiving class | Script: each numbered message of diagrams 30 to 33 is matched to the lifeline it points at and to the operations of diagrams 10 to 13 | 0 mismatches after fix 3 |
| 2 | Every lifeline is a class of the class diagrams | Same script | 0 missing |
| 3 | State names equal the enumeration values | Diagrams 41 and 42 against `PaymentStatus` and `ReservationStatus` in diagram 10 | fixed (see below) |
| 4 | State events are operations of the entity | `authorize`, `approve`, `decline`, `refund`, `assignSeat`, `confirmPayment`, `changeFlight`, `cancel`, `complete` against Payment, Reservation and PaymentMethod | match |
| 5 | Use-case names agree between table, diagram, scenarios and sequence titles | Table of section B against diagram 01 | match (Select Seat is the only extra ellipse: it is the included use case of Book Flight and has no ID) |
| 6 | Every UC-nn and FR-n in the text exists | Regex over all sections | UC-01 to UC-17 and FR-1 to FR-14 only, all used |
| 7 | Every class of the package diagram is in a class diagram, and the reverse | Script on the raw XML | 0 missing, 0 extra |
| 8 | Traceability tables cover everything | 14 FRs, 17 UCs once, 50 first-cut classes | complete |
| 9 | Names written in the text are known | Every CamelCase word in the sections is a class, an operation, a state, an exception or a parameter | 0 unknown after fix 4 |
| 10 | Terminology | `grep -rniE "\b(ticket\|booking)\b" docs/report` | only Book Flight and BookingController after fix 5 |
| 11 | Multiplicities of diagram 10 against the text and diagram 12 | Read all 10 associations | match (Flight to Seat is 1 to 0..* in both) |
| 12 | The report builds | `make pdf`: no undefined reference, no "??" | clean |
| 13 | Diagrams are readable | Landscape pages viewed at 100% zoom | text 5 to 6 pt on the landscape pages after fix 6, larger on portrait pages |

## Mismatches found and fixed

1. `Reservation.complete(now)` in diagram 10 against `complete(clock)` in
   diagrams 12 and in the code plan. Diagram 10 now says `complete(clock)`.
2. The payment states in diagram 41 and in section E were `Pending`,
   `Approved`, `Declined`, `Refunded`. They are now `PENDING`, `APPROVED`,
   `DECLINED`, `REFUNDED`, like the enumeration and the reservation states.
3. The sequence diagrams send 20 messages to views (`showError`, `pickSeat`,
   ...) that were on no class. Section F now lists them for the four views.
4. Section C named a `showResults(offers)` view operation. The sequence
   diagram calls it `showTable(offers)`; the text now says so.
5. A "booking controller" in the reliability row of the requirements table
   now reads `BookingController`.
6. Landscape pages had 2.5 cm margins and printed the diagrams at about 4 pt.
   They now use 1.2 cm margins and a taller figure.

## Not changed

- The use-case table has 17 rows and the diagram has 18 ellipses. Select Seat
  is an included use case of Book Flight, which is how FR-14 is met, and it
  does not get its own control class or ID.
- UC-12 (View Flight Schedules) has no scenario step. Its operation is in the
  table of section C and its row in the traceability table says so.
