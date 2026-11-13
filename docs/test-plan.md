# Manual test plan

One or more cases for every use case (UC-01 to UC-17) and for the main alternative flows of the
scenarios: a seat taken first, a declined card, and a flight that cannot be removed.

## How to run it

1. Build the jar: `mvn -B clean verify` (the jar is `target/ChinookAir.jar`).
2. Start from a fresh database: `rm -rf ~/.chinookair`.
3. Start the program: `java -jar target/ChinookAir.jar`. The first start creates the demo data.
4. Demo accounts (also in the README): `customer1` / `customer123`, `agent1` / `agent123`,
   `admin` / `admin123`.
5. Flights are seeded from tomorrow on, so "tomorrow" below means the day after the start date.
   Fix the date with `java -Dchinookair.today=2026-12-01 -jar target/ChinookAir.jar` to follow the
   examples exactly.
6. Follow a row, write `pass` or `fail` and the date in the last two columns. For each `fail`,
   open an issue with the label `bug` and link it in the row, then run the row again after the fix.

**Clicks** counts mouse clicks from the dashboard of the role: a button, a table row, and one for each
choice you make in a list. Typing is not counted, and neither are the defaults (the first airports,
today's date).

**Automated check** names the test class that checks the same behaviour without a window. That is a
safety net, not a replacement for walking the screen once.

## Cases

| ID | UC | Role | Steps from the dashboard | Clicks | Expected result | Automated check | Result | Date |
|----|----|------|--------------------------|--------|-----------------|-----------------|--------|------|
| T01 | UC-01 Login | any | On the login screen type the username and password, press Enter or Log in | 1 | The dashboard of the role opens and shows "Logged in as <name>" | `LoginViewTest` | | |
| T02 | UC-01 Login | any | Type `customer1` and a wrong password, Log in | 1 | `Invalid username or password` stays on the login screen, the password field is cleared, nobody is logged in | `LoginViewTest` | | |
| T03 | UC-01 Login | any | Press Log out on a dashboard, then try to go back | 1 | The login screen opens; the screens that need a login are not shown | `DashboardTest`, `MainFrameSmokeTest` | | |
| T04 | UC-02 Register | customer | On the login screen press Register, fill username `newflyer`, password `secret123`, name, email, press Register | 2 | The login screen opens with the username filled in and a notice | `RegisterViewTest` | | |
| T05 | UC-02 Register | customer | Register again with the same username | 2 | `Username already taken`, nothing is created | `RegisterViewTest` | | |
| T06 | UC-02 Register | customer | Register with password `abc` | 2 | A message that the password must be at least 8 characters, with a digit | `RegisterViewTest` | | |
| T07 | UC-03 Manage Customer Profile | customer | My profile, change the phone, Save | 2 | `Saved`; leaving and coming back shows the new phone; the username cannot be edited | `ProfileViewTest` | | |
| T08 | UC-03 Manage Customer Profile | customer | My profile, set the email to a bad value such as `a@b`, Save | 2 | A message about a valid email, nothing saved | `ProfileViewTest` | | |
| T09 | UC-03 Manage Customer Profile | agent | Customers, type part of a name, Search, choose the row, change the phone, Save | 4 | `Saved`; searching again shows the new phone | `CustomerManagementViewTest` | | |
| T10 | UC-03 Manage Customer Profile | agent | Customers, New customer, fill the form with a starting password, Save | 3 | The customer is saved and selected; they can log in with the starting password | `CustomerManagementViewTest` | | |
| T11 | UC-03 Manage Customer Profile | agent | Customers, choose a row, set the email to one that another customer has, Save | 3 | `Email already registered`, nothing saved | `CustomerManagementViewTest` | | |
| T12 | UC-04 Find Available Flights | customer | Search flights, From `YYC`, To `YVR`, date tomorrow, Search | 4 | A table of flights ordered by time, with seats left and the lowest price | `FlightSearchViewTest` | | |
| T13 | UC-04 Find Available Flights | customer | Same, with the airline set to `AC - Air Canada` | 5 | Only Air Canada rows | `FlightSearchViewTest` | | |
| T14 | UC-04 Find Available Flights | customer | From and To both `YYC`, Search | 4 | `Origin and destination must be different`, the table is unchanged | `FlightSearchViewTest` | | |
| T15 | UC-04 Find Available Flights | customer | Date `2026-13-40`, Search | 2 | A message that the date must look like yyyy-MM-dd | `FlightSearchViewTest` | | |
| T16 | UC-04 Find Available Flights | customer | A date far in the future with no flights, Search | 4 | `No flights found. Try another date or airline.` | `FlightSearchViewTest` | | |
| T17 | UC-05 View Flight Details | customer | Search flights, Search | 4 | Each row shows the flight number, airline, route, departure and arrival, seats left and the price from | `FlightTableModelTest`, `FlightSearchViewTest` | | |
| T18 | UC-06 Book Flight | customer | Search flights, Search, choose a row, Select flight, choose seat `1A` | 7 | `Seat 1A (Business) - $<business price>` is shown; `3C` shows the economy price | `SeatSelectionViewTest` | | |
| T19 | UC-06 Book Flight | customer | Continue to a seat, press Continue to payment | 8 | The payment screen opens with the price as the amount | `SeatSelectionViewTest` | | |
| T20 | UC-06 Book Flight | customer | With the seat screen open, take the same seat from another session (see below), then press Continue | 8 | `That seat was just taken. Please pick another.`, the seat turns grey, you stay on the screen | `SeatSelectionViewTest` | | |
| T21 | UC-06 Book Flight | customer | On the seat screen press Back | 7 | The search screen opens; My reservations shows no leftover reservation | `SeatSelectionViewTest` | | |
| T22 | UC-07 Make Payment | customer | On the payment screen: name `Jo Smith`, card `4111 1111 1111 1111`, expiry `12/30`, Pay | 9 | The confirmation shows a number like `CA7K2P9X` and `Saved to: <path>`; the file exists and holds the same number | `PaymentViewTest`, `ConfirmationViewTest` | | |
| T23 | UC-07 Make Payment | customer | Card `4111 1111 1111 0000`, Pay | 9 | `Card declined`, you stay on the payment screen, the card number field is empty; a good card then works | `PaymentViewTest` | | |
| T24 | UC-07 Make Payment | customer | Card `1234` | 9 | A message with `16 digits` | `PaymentViewTest` | | |
| T25 | UC-07 Make Payment | customer | Expiry `01/20` | 9 | A message that the card has expired | `PaymentViewTest` | | |
| T26 | UC-07 Make Payment | customer | An empty name | 9 | A message about the name | `PaymentViewTest` | | |
| T27 | UC-07 Make Payment | customer | After a paid booking press Back to dashboard, then My reservations, choose the row | 3 | The reservation is `CONFIRMED`; Pay now is disabled, so it cannot be paid twice | `MyReservationsViewTest`, `PaymentViewTest` | | |
| T28 | UC-08 View Payment Records | agent | Payment records, type the customer's name, Search, choose the row | 3 | The payments of that customer, newest first, with the masked card | `PaymentRecordsViewTest` | | |
| T29 | UC-09 Cancel Reservation | customer | My reservations, choose a `CONFIRMED` row, Cancel reservation, Yes | 4 | The question names the refund; the row becomes `CANCELLED`; the seat is free again on the seat screen | `MyReservationsViewTest` | | |
| T30 | UC-09 Cancel Reservation | customer | The same, answer No | 4 | Nothing changes | `MyReservationsViewTest` | | |
| T31 | UC-09 Cancel Reservation | customer | Choose a `CANCELLED` row | 2 | Cancel reservation is disabled | `MyReservationsViewTest` | | |
| T32 | UC-10 Modify Reservation | agent | Customers, Search, choose the customer, Modify a reservation, choose the `CONFIRMED` row, Modify, search a flight of the same price, Select, choose a seat, Confirm change | 13 | The reservation stays `CONFIRMED` with the new flight and seat | `SeatSelectionModifyTest`, `ModifyReservationViewTest` | | |
| T33 | UC-10 Modify Reservation | agent | The same to a flight with a higher price | 13 | A notice that the old payment was refunded; the payment screen opens for the new amount; paying makes it `CONFIRMED` | `SeatSelectionModifyTest` | | |
| T34 | UC-10 Modify Reservation | agent | The same, with a seat taken in the meantime | 13 | `That seat was just taken. Please pick another.`, nothing changed | `SeatSelectionModifyTest` | | |
| T35 | UC-10 Modify Reservation | agent | Choose a `CANCELLED` row | 5 | Modify is disabled | `ModifyReservationViewTest` | | |
| T36 | UC-11 View Reservation History | customer | My reservations | 1 | All reservations, newest first, with confirmation number, flight, route, departure, seat, status and price; a new customer sees "No reservations yet." | `MyReservationsViewTest` | | |
| T37 | UC-12 View Flight Schedules | agent | Schedule, set the period, Show | 2 | Flights of the period ordered by departure; an empty period shows `No flights in this period.` | `ScheduleViewTest` | | |
| T38 | UC-13 Update Schedules | admin | Schedule, Show, choose a flight, set the new departure and arrival, Reschedule | 4 | The table shows the new times | `ScheduleViewTest` | | |
| T39 | UC-13 Update Schedules | admin | Reschedule onto the time of another flight of the same aircraft, or into the past | 4 | `Aircraft is already assigned to a flight at that time` or `Departure must be in the future` | `ScheduleViewTest` | | |
| T40 | UC-14 Manage Flights | admin | Flights, fill `AC9001`, route, airline, a free aircraft, future times, price `199.99`, Add | 2 | `Added AC9001`; it shows in the table; a customer search for that day finds it | `FlightManagementViewTest` | | |
| T41 | UC-14 Manage Flights | admin | Add `AC9001` again | 2 | `Flight number already exists` | `FlightManagementViewTest` | | |
| T42 | UC-14 Manage Flights | admin | Arrival before departure; departure in the past; the same aircraft at an overlapping time | 2 | A message about the arrival; `Departure must be in the future`; `Aircraft is already assigned to a flight at that time` | `FlightManagementViewTest` | | |
| T43 | UC-14 Manage Flights | admin | Choose a flight that has a confirmed reservation, Remove, Yes | 4 | `Cannot remove a flight with active reservations`, the flight stays | `FlightManagementViewTest` | | |
| T44 | UC-14 Manage Flights | admin | Choose `AC9001` (no reservations), Remove, Yes | 4 | The flight disappears from the table | `FlightManagementViewTest` | | |
| T45 | UC-15 Manage Routes | admin | Routes, choose two airports that have no route, Add route | 4 | The route appears in the table | `RouteManagementViewTest` | | |
| T46 | UC-15 Manage Routes | admin | Add the same route again | 4 | `Route already exists` | `RouteManagementViewTest` | | |
| T47 | UC-16 Manage Aircraft | admin | Aircraft, model `E190`, rows `25`, seats per row `4`, Add aircraft | 2 | The table shows 100 seats | `AircraftManagementViewTest` | | |
| T48 | UC-16 Manage Aircraft | admin | Rows `0` | 2 | A message that contains `Rows` | `AircraftManagementViewTest` | | |
| T49 | UC-17 View Monthly Promotion | any | Start with `-Dchinookair.today=2027-02-01` on a fresh database | 0 | A dialog with the promotion of February; closing it opens the login screen | `MainFrameSmokeTest`, `PromotionControllerTest` | | |
| T50 | UC-17 View Monthly Promotion | any | Close the program and start it again the same month | 0 | No dialog the second time; with `-Dchinookair.today=2027-03-01` the March promotion shows | `MainFrameSmokeTest`, `PromotionControllerTest` | | |
| T51 | Roles | customer, agent | Log in as `customer1` and look for the administrator screens; log in as `admin` and look for the booking screens | 0 | A customer sees only Search flights, My reservations, My profile; an agent sees Customers, Reservations, Payment records, Schedule; the administrator sees Flights, Routes, Aircraft, Schedule | `DashboardTest` | | |

### Taking a seat from another session (T20)

While the seat screen is open, in a terminal run

```
sqlite3 ~/.chinookair/chinookair.db
```

and insert a `CONFIRMED` reservation for seat `1A` of that flight (copy the columns of an existing
`reservations` row), or simply book the same seat as a second customer in a second copy of the program.
Then press Continue in the first window.

## Use-case coverage

Every use case has at least one row: UC-01 T01 to T03, UC-02 T04 to T06, UC-03 T07 to T11,
UC-04 T12 to T16, UC-05 T17, UC-06 T18 to T21, UC-07 T22 to T27, UC-08 T28, UC-09 T29 to T31,
UC-10 T32 to T35, UC-11 T36, UC-12 T37, UC-13 T38 and T39, UC-14 T40 to T44, UC-15 T45 and T46,
UC-16 T47 and T48, UC-17 T49 and T50.

## Clicks from the dashboard

The usability requirement is at most 4 clicks from the dashboard. Counted on the screens as built:

| Use case | Clicks | Within 4? |
|----------|--------|-----------|
| UC-01, UC-02, UC-03 (customer), UC-11, UC-12, UC-14 (add), UC-16, UC-17 | 0 to 2 | yes |
| UC-03 (agent), UC-04, UC-05, UC-08, UC-09, UC-13, UC-14 (remove), UC-15 | 3 to 4 | yes |
| UC-06 Book Flight (to the payment screen) | 8 | no |
| UC-07 Make Payment (booking and paying) | 9 | no |
| UC-10 Modify Reservation | 13 | no |

Booking, paying and modifying are chains of screens (search, seat, payment), so they need more
clicks than a single screen. This is a known difference from the requirement and should be decided
before the submission: either the requirement is read as "per screen", or the flow is shortened,
for example by opening the seat screen with a double click on a search result and by starting from
the airports of the last search.

## Status

The cases are written and each one has an automated check that passes in `mvn verify`. The columns
`Result` and `Date` are filled in when the case is walked on the screen with the built jar; a case
that fails there gets a `bug` issue first.
