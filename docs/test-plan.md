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

**Clicks** counts mouse clicks from the screen that opens after the login (a customer's Home, an agent's
Customers, the administrator's Flights) or from the tab you are on: a tab, a button, a table row (a double click on a flight counts as one), and one for each choice
you make in a list. Typing is not counted, and neither are the defaults: the Find flights screen
already holds the first route that has flights and shows them, so booking that route needs no search.

**Automated check** names the test class that checks the same behaviour without a window. That is a
safety net, not a replacement for walking the screen once.

## Cases

| ID | UC | Role | Steps from Home | Clicks | Expected result | Automated check | Result | Date |
|----|----|------|-----------------|--------|-----------------|-----------------|--------|------|
| T01 | UC-01 Login | any | On the login screen type the username and password, press Enter or Log in | 1 | A customer lands on Home with "Hello, <first name>"; an agent lands on Customers; the administrator lands on Flights; the navy bar shows the name (a customer by full name, the staff by username), the role and the tabs of that role | `LoginViewTest`, `AppBarTest` | | |
| T02 | UC-01 Login | any | Type `customer1` and a wrong password, Log in | 1 | A red strip says `Invalid username or password`, the password field is empty with the cursor in it, the username stays | `LoginViewTest` | | |
| T03 | UC-01 Login | any | Press Log out in the top right of any screen, then try to go back | 1 | The login screen opens; the screens that need a login are not shown | `AppBarTest`, `MainFrameSmokeTest` | | |
| T04 | UC-02 Register | customer | On the login screen press Create an account, fill username `newflyer`, password `secret123`, name, email, press Create account | 2 | The login screen opens with the username filled in and a green strip | `RegisterViewTest` | | |
| T05 | UC-02 Register | customer | Register again with the same username | 2 | `Username already taken` under the Username field, nothing is created | `RegisterViewTest` | | |
| T06 | UC-02 Register | customer | Register with password `abc` | 2 | A message under the Password field that it needs at least 8 characters and a digit | `RegisterViewTest` | | |
| T07 | UC-03 Manage Customer Profile | customer | My profile tab, change the phone, Save changes | 2 | Save changes is off until something differs; then a green `Saved`; leaving and coming back shows the new phone; the username cannot be edited | `ProfileViewTest` | | |
| T08 | UC-03 Manage Customer Profile | customer | My profile tab, set the email to a bad value such as `a@b`, Save changes | 2 | A red strip about a valid email, nothing saved | `ProfileViewTest` | | |
| T09 | UC-03 Manage Customer Profile | agent | Customers tab, type part of a name (the list follows the typing), choose the row, change the phone, Save | 3 | The card is headed by the name with the username and the number of reservations; a green `Saved <name>`; searching again shows the new phone | `CustomerManagementViewTest` | | |
| T10 | UC-03 Manage Customer Profile | agent | Customers tab, New customer, fill the form with a starting password, Save | 3 | The customer is saved and selected; they can log in with the starting password | `CustomerManagementViewTest` | | |
| T11 | UC-03 Manage Customer Profile | agent | Customers tab, choose a row, set the email to one that another customer has, Save | 3 | `Email already registered`, nothing saved | `CustomerManagementViewTest` | | |
| T12 | UC-04 Find Available Flights | customer | Find flights tab, From `YYC`, To `YVR`, date tomorrow, Search | 4 | One card per flight, earliest first: times and airports, duration, airline, flight and plane, seats left and the lowest price; above them the chosen day and the three days either side with their lowest prices | `FlightSearchViewTest` | | |
| T13 | UC-04 Find Available Flights | customer | Same, with the airline set to `AC - Air Canada` | 5 | Only Air Canada cards | `FlightSearchViewTest` | | |
| T14 | UC-04 Find Available Flights | customer | From and To both `YYC`, Search | 4 | `Origin and destination must be different`, the cards are unchanged | `FlightSearchViewTest` | | |
| T15 | UC-04 Find Available Flights | customer | Type the date `2026-13-40`, Search | 2 | A message that the date must look like yyyy-MM-dd | `FlightSearchViewTest` | | |
| T16 | UC-04 Find Available Flights | customer | A date far in the future with no flights, Search | 4 | The grey hint `No flights found. Try another date or airline.` and `No flights match this search.` where the cards would be | `FlightSearchViewTest` | | |
| T17 | UC-05 View Flight Details | customer | Find flights tab | 1 | The flights of the first route with flights are already listed as cards, each showing the times, airports, duration, airline, flight number, plane, seats left and the price from | `FlightTableModelTest`, `FlightSearchViewTest` | | |
| T18 | UC-06 Book Flight | customer | Find flights tab, double-click a flight, choose seat `1A` | 3 | The map has a heading over each cabin and a legend with small swatches; hovering a seat shows its class and price; the "Your seat" card shows the passenger, `1A (Business)` and the business price, with Back and Continue right under it; `3C` shows the economy price; the step strip shows step 2 of 4 | `SeatSelectionViewTest` | | |
| T19 | UC-06 Book Flight | customer | Continue with Continue to payment | 4 | The payment screen opens with an order summary and the button `Pay $<amount>`; the step strip shows step 3 | `SeatSelectionViewTest` | | |
| T20 | UC-06 Book Flight | customer | With the seat screen open, take the same seat from another session (see below), then press Continue to payment | 4 | `That seat was just taken. Please pick another.`, the seat shows a cross and is grey, you stay on the screen | `SeatSelectionViewTest` | | |
| T21 | UC-06 Book Flight | customer | On the seat screen press Back | 3 | The search screen opens; My reservations shows no leftover reservation | `SeatSelectionViewTest` | | |
| T22 | UC-07 Make Payment | customer | On the payment screen: name `Jo Smith`, card `4111 1111 1111 1111`, expiry `12/30`, Pay | 5 | "Booking confirmed" with a number like `CA7K2P9X` that Copy puts on the clipboard, the trip card, and `A copy of this confirmation was saved: <file>`; Open folder shows the file, which holds the same number | `PaymentViewTest`, `ConfirmationViewTest` | | |
| T23 | UC-07 Make Payment | customer | Card `4111 1111 1111 0000`, Pay | 5 | `Card declined` in a red strip, you stay on the payment screen, the card number field is empty; a good card then works | `PaymentViewTest` | | |
| T24 | UC-07 Make Payment | customer | Card `1234` | 5 | A red outline and a message with `16 digits` under Card number | `PaymentViewTest` | | |
| T25 | UC-07 Make Payment | customer | Expiry `01/20` | 5 | A message under Expiry date that the card has expired | `PaymentViewTest` | | |
| T26 | UC-07 Make Payment | customer | An empty name | 5 | A message about the name under Name on card | `PaymentViewTest` | | |
| T27 | UC-07 Make Payment | customer | After a paid booking press Done, then the My reservations tab | 3 | One card under Upcoming with the badge `Confirmed`; it offers Cancel reservation and no Pay now | `MyReservationsViewTest`, `PaymentViewTest` | | |
| T28 | UC-08 View Payment Records | agent | Payment records tab, type the customer's name, choose the row | 2 | The customer list is on the left and, on the right, "Payments of <name>" with a line `Paid $… · Refunded $… · N payments`, the payments newest first, with the masked card and a badge such as `Paid` | `PaymentRecordsViewTest` | | |
| T29 | UC-09 Cancel Reservation | customer | My reservations tab, Cancel reservation on a `Confirmed` card, Cancel reservation in the red dialog | 3 | The dialog names the refund; the card moves under Cancelled with the badge `Cancelled`; the seat is free again on the seat screen | `MyReservationsViewTest`, `ConfirmDialogTest` | | |
| T30 | UC-09 Cancel Reservation | customer | The same, press Cancel in the dialog (or Escape) | 4 | Nothing changes | `MyReservationsViewTest`, `ConfirmDialogTest` | | |
| T31 | UC-09 Cancel Reservation | customer | Look at a `Cancelled` card | 1 | It has no Cancel reservation button | `MyReservationsViewTest` | | |
| T32 | UC-10 Modify Reservation | agent | Customers tab, choose the customer, Modify a reservation, choose the `Confirmed` row, Modify, From and To of a flight of the same price, Search, double-click the flight, choose a seat, Confirm change | 11 | The reservation stays `Confirmed` with the new flight and seat; the step strip shows the three steps | `SeatSelectionModifyTest`, `ModifyReservationViewTest` | | |
| T33 | UC-10 Modify Reservation | agent | The same to a flight with a higher price | 11 | A notice that the old payment was refunded; the payment screen opens for the new amount; paying makes it `Confirmed` | `SeatSelectionModifyTest` | | |
| T34 | UC-10 Modify Reservation | agent | The same, with a seat taken in the meantime | 11 | `That seat was just taken. Please pick another.`, nothing changed | `SeatSelectionModifyTest` | | |
| T35 | UC-10 Modify Reservation | agent | Choose a `Cancelled` row | 4 | Modify is disabled and its tip says why | `ModifyReservationViewTest` | | |
| T36 | UC-11 View Reservation History | customer | My reservations tab | 1 | All reservations, newest first, with confirmation number, flight, route, departure, seat, a status badge and price; a new customer sees "No reservations yet" | `MyReservationsViewTest` | | |
| T37 | UC-12 View Flight Schedules | agent | Schedule tab, Next 7 days (or pick two dates in the calendars) | 2 | Flights of the period ordered by departure; an empty period shows `No flights in these dates.` | `ScheduleViewTest` | | |
| T38 | UC-13 Update Schedules | admin | Schedule tab, choose a flight, set the new departure and arrival in the card that appears, Reschedule | 3 | A green `Rescheduled <flight>`; the table shows the new times | `ScheduleViewTest` | | |
| T39 | UC-13 Update Schedules | admin | Reschedule onto the time of another flight of the same aircraft, or into the past | 3 | `Aircraft is already assigned to a flight at that time` or `Departure must be in the future` | `ScheduleViewTest` | | |
| T40 | UC-14 Manage Flights | admin | Flights tab, Add flight, fill `AC9001`, price `199.99`, route, airline, a free aircraft, future times, Add | 3 | A green `Added AC9001`; typing `AC9001` in the filter box shows it; a customer search for that day finds it | `FlightManagementViewTest` | | |
| T41 | UC-14 Manage Flights | admin | Add `AC9001` again | 3 | `Flight number already exists` under Flight number | `FlightManagementViewTest` | | |
| T42 | UC-14 Manage Flights | admin | Arrival before departure; departure in the past; the same aircraft at an overlapping time | 3 | A message about the arrival; `Departure must be in the future`; `Aircraft is already assigned to a flight at that time` under Aircraft | `FlightManagementViewTest` | | |
| T43 | UC-14 Manage Flights | admin | Choose a flight that has a confirmed reservation, Remove, Remove flight in the red dialog | 4 | `Cannot remove a flight with active reservations`, the flight stays | `FlightManagementViewTest`, `ConfirmDialogTest` | | |
| T44 | UC-14 Manage Flights | admin | Choose `AC9001` (no reservations), Remove, Remove flight in the red dialog | 4 | The flight disappears from the table | `FlightManagementViewTest` | | |
| T45 | UC-15 Manage Routes | admin | Routes tab, choose two airports that have no route, Add route | 4 | A green strip and the route in the table | `RouteManagementViewTest` | | |
| T46 | UC-15 Manage Routes | admin | Add the same route again | 4 | `Route already exists` in a red strip | `RouteManagementViewTest` | | |
| T47 | UC-16 Manage Aircraft | admin | Aircraft tab, model `E190`, rows `25`, seats per row `4`, Add aircraft | 2 | While typing the card says `100 seats in total`; the table shows 100 seats | `AircraftManagementViewTest` | | |
| T48 | UC-16 Manage Aircraft | admin | Rows `0` | 2 | A message under Rows that contains `Rows` | `AircraftManagementViewTest` | | |
| T49 | UC-17 View Monthly Promotion | any | Start with `-Dchinookair.today=2027-02-01` on a fresh database | 0 | A dialog with the logo, the promotion of February and a gold line; OK, Enter or Escape closes it and the login screen is ready | `MainFrameSmokeTest`, `PromotionControllerTest`, `PromotionPanelTest` | | |
| T50 | UC-17 View Monthly Promotion | any | Close the program and start it again the same month | 0 | No dialog the second time; with `-Dchinookair.today=2027-03-01` the March promotion shows | `MainFrameSmokeTest`, `PromotionControllerTest` | | |
| T51 | Roles | customer, agent | Log in as `customer1` and look for the administrator screens; log in as `admin` and look for the booking screens | 0 | The tabs are Find flights, My reservations, My profile for a customer; Customers, Payment records, Schedule for an agent; Flights, Routes, Aircraft, Schedule for the administrator; a role cannot open another role's screens | `AppBarTest`, `NavModelTest`, `DashboardTest` | | |
| T52 | Keyboard | any | On the login screen press Tab, type, press Enter; after login press Cmd+1, Cmd+2 (Ctrl on Windows and Linux); on the payment screen Tab through the fields | 0 | Enter logs in; the shortcuts open the tabs in order and each tab's tip names its key; Tab on the payment screen goes Name, Number, Type, Expiry; every control shows a visible ring when it has the focus | `ScreenDefaultsTest`, `AppBarTest`, `PaymentViewTest` | | |
| T53 | Window sizes | any | Drag the window to the smallest size (it stops at 960 x 640) and to a large size; open Register, Payment, Confirmation, Customers and Flights | 0 | Nothing is cut off: a small window gets a scroll bar on the page or the form, and the buttons stay reachable | `MainFrameSmokeTest` | | |
| T54 | Start-up | any | Delete `~/.chinookair`, start the program | 0 | A small window with the logo and a moving bar shows at once, then the login screen replaces it; the window is never frozen | `SplashWindowTest` | | |
| T55 | UC-04 Find Available Flights | customer | On Home look at "Your next trip", the Find a flight box and "Recent bookings"; with a paid booking in the future, then with none; press Search flights | 1 | The next trip is the confirmed booking that leaves first (route, day and time, flight, seat, confirmation number); with none it says "No trip booked yet"; "Recent bookings" shows at most three, newest first, each with its status; the boxes start on a route that has flights; Search flights opens the Find flights screen on those choices | `CustomerHomeTest` | | |
| T56 | UC-10 Modify Reservation | agent | Customers tab, choose a customer, Reservations; look at the path above the title; click Customers in it | 3 | The path reads `Customers › <name> › Reservations`, the title says whose reservations they are, and clicking Customers returns to the list; the same path (ending in Modify reservation) shows on the modify screen | `MyReservationsViewTest`, `ModifyReservationViewTest`, `BreadcrumbTest` | | |
| T57 | UC-04 Find Available Flights | customer | On the results press another day in the strip, then Cheapest, Shortest and Earliest above the cards; choose a card with the arrow keys and press Enter | 3 | The strip moves to the chosen day and searches it; the cards change order each time and the chosen card stays chosen; Enter opens the seat screen for that flight; with one flight or none there is no Sort by line | `DayStripTest`, `FlightResultListTest`, `FlightSearchViewTest` | | |
| T58 | UC-07 Make Payment | customer | On the payment screen look at the order summary, the card number box and the line under the buttons; press the eye in the card number box | 1 | The summary names the passenger and the seat class; the box has a hint and the eye shows the digits; a line says only the last four digits are kept | `PaymentViewTest` | | |
| T59 | UC-07 Make Payment | customer | After a paid booking look at the confirmation | 0 | A navy band with both airports, times, cities and the duration, and under it the passenger, flight, seat and the amount paid | `ItineraryCardTest`, `ConfirmationViewTest` | | |
| T60 | UC-09 Cancel Reservation | customer | My reservations tab with several bookings; type part of a confirmation number; cancel one | 2 | The list is in Upcoming, Past and Cancelled sections; typing narrows it and "No reservation matches" shows when nothing fits; a card offers only what applies (Pay now only while awaiting payment, Cancel only before the flight leaves) | `MyReservationsViewTest`, `ReservationGroupsTest` | | |
| T61 | UC-14 Manage Flights | admin | Flights tab at the smallest window, choose a flight or press Add flight; set Departure and Arrival with the calendar and the time boxes | 3 | Flight number, price, Departure and Arrival are in view without scrolling; each time has a calendar for the day and a `HH:mm` box; a mistake in either is explained by the same message as before | `FlightManagementViewTest`, `DateTimeFieldTest`, `LayoutAlignmentTest` | | |
| T62 | UC-13 Update Schedules | agent | Schedule tab, look at the flights of a week | 1 | The day is written once, in bold, on the first flight of the day; the rows show only the time of departure and arrival | `ScheduleViewTest`, `DayColumnsTest` | | |
| T63 | UC-15 Manage Routes | admin | Routes tab | 1 | The routes are in order of where they start and then where they end, and each starting airport is named once over its routes | `RouteTableModelTest`, `RouteManagementViewTest` | | |
| T64 | Input | any | Try typing letters in the Rows and Seats per row boxes of the Aircraft screen, in Base price, in the time box of a flight, in the card number and expiry boxes and in a phone box; type `0` in Rows and click into another box | 0 | Letters and other characters that could never be part of a number, a time, a card or a phone number do not appear (a pasted text keeps only what fits); a price takes digits and one dot; leaving Rows with `0` shows its message under it at once, and an empty box is not scolded | `InputFiltersTest`, `FormFieldTest`, `AircraftManagementViewTest`, `FlightManagementViewTest`, `PaymentViewTest`, `RegisterViewTest` | | |
| T65 | Dialogs | any | At the start of a month, with the promotion open, press Enter, then again and press Esc; in a red dialog press Esc | 0 | Enter or Esc closes the promotion; Esc in a confirmation answers no | `PromotionDialogTest`, `ConfirmDialogTest` | | |

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
UC-16 T47 and T48, UC-17 T49 and T50. The screens themselves: T51 to T65 (roles, keyboard, window sizes, start-up, customer Home, the agent's path, nearby days and sorting, payment details, confirmation, reservation sections, flight times, schedule days, route order, what may be typed, dialog keys).

## Clicks from Home

The usability requirement is at most 4 clicks from the dashboard. Counted on the screens as built, from
the Home screen after the login, with the defaults (the Find flights screen already lists the first
route that has flights):

| Use case | Clicks | Within 4? |
|----------|--------|-----------|
| UC-01, UC-02, UC-03, UC-04 (first route), UC-05, UC-08, UC-11, UC-12, UC-16, UC-17 | 0 to 2 | yes |
| UC-03 (agent), UC-09, UC-13, UC-14, UC-15 | 3 to 4 | yes |
| UC-06 Book Flight (to the payment screen: tab, flight, seat, Continue) | 4 | yes |
| UC-07 Make Payment (booking and paying) | 5 | no, by one |
| UC-10 Modify Reservation | 11 | no |

An earlier count, before the screens were redesigned, was 8 for booking, 9 for paying and 13 for
modifying. What changed: the tabs are one click from anywhere, the first route is searched at once,
a double click on a flight opens its seat map, and the lists search while you type.

Paying still has one click more than the requirement, because a payment is a screen of its own after
the seat. Modifying is a chain of three tasks done by an agent: choose the customer, choose the
reservation, then choose the new flight and seat. Both are known differences. The decision for the
submission is whether to read the requirement as "per screen" or to shorten these two flows further.

## Status

The cases are written and each one has an automated check that passes in `mvn verify`. The columns
`Result` and `Date` are filled in when the case is walked on the screen with the built jar; a case
that fails there gets a `bug` issue first.
