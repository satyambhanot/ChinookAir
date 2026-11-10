# Glossary

One vocabulary for the diagrams, the report, the tests and the code. If a name
here changes, change it everywhere in the same pull request.

Use **Reservation**, never "ticket" or "booking", for the thing a customer
holds. "Book" is only a verb: the Book Flight use case, `BookingController`
and `BookingView`.

| Term | Meaning |
|---|---|
| Reservation | A customer's hold on one seat on one flight. It has a confirmation number, a price and a state (`CREATED`, `PENDING_PAYMENT`, `CONFIRMED`, `CANCELLED`, `COMPLETED`). |
| Flight | One scheduled trip of an aircraft on a route, with a departure and arrival time. |
| Route | An origin airport and a destination airport that flights are scheduled on. |
| Airline | The company that operates flights. |
| Airport | A place flights leave from or arrive at, identified by its code (for example `YYC`). |
| Aircraft | A plane of a given model with a fixed seat layout. A flight uses one aircraft. |
| Seat | A numbered place on an aircraft (for example `12A`), Economy or Business. |
| Payment | A simulated charge for a reservation, with a state (`PENDING`, `APPROVED`, `DECLINED`, `REFUNDED`). Only the last 4 card digits are stored. |
| Promotion | A discount picked at random on the first launch of a calendar month and shown once that month. |
| Customer | A user who searches for flights and manages their own reservations. |
| Flight Agent | A user who manages customers and reservations on their behalf. |
| Administrator | A user who manages flights, routes, aircraft and schedules. |
| Confirmation number | The code that identifies a reservation on its confirmation. |

## Decisions that go with these terms

- One passenger per reservation: the customer who owns it.
- Cancel is allowed until the flight departs, with a full refund.
- A flight with active reservations cannot be removed.
- Business is the first two rows and costs 1.5 times Economy.
- A card ending `0000` is declined; every other card is approved.

The full table, with the guideline section behind each decision, is under
"Decisions" in [action-plan.md](action-plan.md).

## Checking the vocabulary

```bash
grep -rniE "\b(ticket|booking)\b" docs src
```

Every hit should be Book Flight, `BookingController`, `BookingView`, a
section title quoted from the guideline, or a phrase like "double booking"
that describes the failure and not the entity. Anything else should say
"reservation".
