# ChinookAir UI refinement plan (round two)

Status: R1 to R4 are built; R5 to R8 are still to do. This file started as a plan only (PR 208).

## Progress

| Milestone | State | Notes |
|---|---|---|
| R1 Tokens and consistency | Done | `Space`, `Format`, `SurfaceBorder`, `SuccessMark`; dates written for people and real times in the table models; spacing and format rules enforced by tests. Deviations: cards get their shadow through one border (`SurfaceBorder.apply`) instead of `Theme.card`, because a border can paint the rounded body only on a see-through panel; the right-aligned money column moves to R4 and R5, where those tables are rebuilt; the `Dashboard` side padding fix from F3 landed here |
| R2 Layout and alignment | Done | `ContentPane` (readable width, centred), `FilterBar`, `ButtonRow`, `TableArea` (sized to rows, with the buttons right under it), label inset in `FormField`, plain one-pixel line on every scroll pane so tables line up with cards, success banner and buttons moved onto the page edge. `LayoutAlignmentTest` measures all of it (headings, labels against input edges, filter-bar centre lines, button rows, table edges, card tops, wide and small windows). Deviations: tables that sit beside a card (customers, flights, routes, aircraft, payment records) still fill the height so they end level with the card; the left-aligned confirmation buttons are drawn 2 px inside the page edge (FlatLaf keeps a 2 px focus margin around buttons); the customers card starts level with the search box, not the table |
| R3 Navigation and Home | Done | One name per screen (tab, title, card: Find flights, My reservations, Schedule; Modify reservation); login lands on Home for a customer, Customers for an agent, Flights for the administrator (`ViewId.landingFor`); the agent's dead-end Reservations card is gone; the customer Home shows the next trip, a Find a flight box that starts on a route with flights, and the three latest bookings, all from data already stored; a `Breadcrumb` shows the agent's way back (Customers › name › Reservations or Modify reservation) and replaces the two Back buttons; a customer is shown by name in the bar and the greeting. Deviations: the step names stay "Find a flight, Choose a seat, Pay, Done" (the naming table had "Choose flight", the text above it said they stay); the agent and administrator Homes keep their cards and have no counts, because the logo reaches them but nobody lands there; link buttons lose their side padding so a link lines up with the text beside it |
| R4 Customer journey | Done | Flight results are cards (`FlightResultList`) with times and airports, a route line with the duration, airline, flight and plane, seats left (in the warning colour at ten or fewer) and the lowest price; above them a `DayStrip` of the chosen day and three either side with their lowest prices (from the existing search, seven calls) and Earliest/Cheapest/Shortest chips that keep the chosen card chosen; Enter on a card chooses it. The seat screen has a swatch legend, a heading over each cabin, a Passenger row, tooltips with class and price, and Back/Continue under the card. Payment names the passenger and seat class, has a card hint and an eye to show the digits, and says only the last four digits are kept. The confirmation is a boarding-pass `ItineraryCard`. My reservations is `ReservationCard`s in Upcoming/Past/Cancelled sections with a search box and Pay now/Cancel on the card (shown only when they apply). Deviations: card-number grouping was left out because the field is masked, so grouping would not show; the eye lets the user check the digits; the reservation cards show only the actions that apply, so there is no "why is this button off" tooltip any more; with the seeded data a search returns one or two flights, so the sort chips appear only when there are two or more |
| R5 to R8 | To do | |


This builds on the redesign that is already on `dev`. Nothing here replaces the design system, the brand colours or the layers. The aim is to close the gap between "modern and consistent" and "feels designed".

## 0. How this was inspected, and what was not

| Source | What I did |
|---|---|
| App | Built `dev` (`mvn clean package`, 163 main files, 0 Checkstyle violations) and painted 24 screen states from the shaded jar to PNG, at 2x, in a 1100 x 692 window. Every observation below marked "measured" comes from those images. |
| Code | Read the presentation package sizes, `Dashboard`, `AppBar`, `Theme`, `Palette`, `Type`, `FlatLaf.properties`, the table models and the business API (`FlightOffer`, `Reservation`, controllers), and grepped for threading, spacing literals, shadows, date formats and accessibility names. |
| Earlier work | The start-up window, the red confirm dialog and the promotion dialog were rendered during M7; I did not re-render them for this plan. |

**Not checked:** a real window on a real display (the jar has been run twice, but I have no notes on what it looked like), hover and pressed states (painted frames have none), keyboard focus rings on custom components, 125 % and 150 % display scaling, Windows and Linux. Where a point below depends on one of these it says so.

Pixel numbers are logical pixels, taken from the 2x render (displayed size divided by 1.82). They are good to about 1 px.

**On the brand colours in the brief.** The brief names Deep Navy `#102A43`, Aviation Blue `#1679C5`, Fresh Cyan `#36B7E9`, Warm Gold `#F2B544`. The palette in `Palette.java` was sampled from the logo file itself: navy `#093151`, blue `#0779C8`, sky `#2DB1E9`, gold `#FDB52E`. The brief's `#102A43` is already our body text colour. The two sets are close but not equal. I recommend keeping the sampled values, because they match the logo pixel for pixel. Switching is a one-file change in `Palette` plus a run of `PaletteTest`, not a redesign. This is open decision 4 in section 11.

## 1. Where the product stands today

### 1.1 Maturity assessment

| Category | Rating | Evidence |
|---|---|---|
| Visual sophistication | Good, not yet premium | One border colour and one radius family do all the separating; there is no elevation anywhere (no shadow in `Theme` or `FlatLaf.properties`). Surfaces are flat white cards on a pale page. Clean, but every card looks the same. |
| Brand consistency | Good | Navy bar, blue primary, gold used sparingly (confirmation rule, Business seats, today in the calendar). The mountain mark appears only at 40 px in the bar and on the login card. |
| Navigation | Good, with duplication | Role tabs, step strip, logo goes Home, Cmd/Ctrl+1..9. But Home repeats the tabs as cards, one screen has four names (below), and Back buttons duplicate the tabs. |
| Information hierarchy | Weak in content areas | Pages are top-left anchored. Measured empty area: customer Home 55 % of the window, My reservations table 80 %, confirmation 40 % (right side), profile 45 %, payment 25 %. Nothing says "this is the important thing" except the page title. |
| Component quality | Good parts, generic results | Status badge, step strip, search field, date picker, form field are good. Flights, reservations and the trip summary are plain tables or key/value lists. |
| Functional usability | Good | Booking to payment is 4 clicks; live search; validation per field. Gaps: ISO date text everywhere, no sort or compare on results, actions need select-then-press. |
| Accessibility | Good base, unverified edges | Contrast is a failing unit test (4.5:1 text, 3:1 lines), status is dot plus word, seat buttons have names, tables are named, Tab order is tested for payment. Not verified: focus ring on custom components, 125/150 % scale, seat map by keyboard on a screen reader. |
| Desktop experience | Good | Minimum 960 x 640, forms scroll, window opens from a splash. Not verified on Windows. |
| Overall polish | Close | The remaining distance is alignment details, empty-space handling, and presenting flights and reservations as designed objects. |

### 1.2 What already works and must be kept

1. The token layer: `Palette` (every colour number in one file), `Type` (28 / 22 / 16 / 14 / 13 / 12), `Theme`, `FlatLaf.properties`, Inter. The contrast test and the "no hex outside Palette" test are real guard rails.
2. The navy `AppBar`: logo tile, role tabs with a sky underline, user chip, Log out, numbered shortcuts.
3. `StepStrip` (completed steps get a check, the current one is bold and filled). It is the best piece of navigation in the app.
4. `Badge` / `Status` (tinted pill, dot, word). Colour-independent.
5. `SearchField`, `DatePickerField`, `FormField` with field-level errors, `Alert`, `EmptyState`, `ConfirmDialog` (red button, Enter on Cancel).
6. Agent Customers screen: list on the left, detail panel on the right, live search. It is the right pattern and should be copied to other screens rather than replaced.
7. Booking path length (4 clicks to the payment screen) and the first-route auto-search.
8. Confirmation: the gold rule beside the confirmation number is a good use of gold.
9. The layering and tests (ArchUnit, 939 tests). Any change must keep presentation free of data-layer imports.

## 2. Findings

Each finding has evidence, why it matters, and the fix. F1 to F7 apply to every screen; F8 on are per screen.

### F1. Content is anchored top-left and tables do not fit their rows

*Evidence (measured).* My reservations: one row, then 800 px of empty white table. Search results: one row, then 470 px empty. Reservations, flights, customers and routes tables stretch to the bottom of the window whatever they hold. Confirmation card is 580 px wide in a 1100 px window with 40 % empty on the right. Customer Home: cards end at 340 px, rest empty.

*Why it matters.* A large empty white rectangle reads as "broken or unfinished", and it makes the one useful row look small. It is the single biggest reason the app still looks assembled instead of composed.

*Fix.*
- A `ContentPane` wrapper: content centred, maximum width 1200 px, 32 px side padding (today `PAGE` is 32 and some screens add 8). Wide windows then show a column, not a stretched table.
- Tables size to their rows (minimum 5 rows of height, maximum the available height) and sit on the page without a tall white box. The empty state goes inside that short box.
- Pages with one primary object (confirmation, profile, payment) use a two-column layout of main card plus side card, filling the width on purpose.

### F2. Dates and times are ISO text

*Evidence.* "2026-12-02 07:00" appears in 7 places (flight results, seats, payment, confirmation, reservations, schedule, admin flights), from five copies of the same `DateTimeFormatter` (`FlightTableModel`, `PaymentTableModel`, `ScheduleTableModel`, `SeatSelectionView`, `FlightManagementView`). The schedule prints the date twice per row (Departs and Arrives).

*Why it matters.* Dates are the most scanned data in a flight app. ISO is for typing, not reading.

*Fix.* One `Format` class: `Wed, Dec 2` and `7:00 AM`, durations `1h 30m` (exists), arrival shown as `8:30 AM` with `+1` when it lands the next day. Input fields keep `yyyy-MM-dd` (the calendar already writes it). Table models call `Format`; their tests change in one place. Whether time is 12 h or 24 h is open decision 3.

### F3. Alignment errors that a designer would flag at once

All measured on the 2x render.

| Where | What | Size | Cause |
|---|---|---|---|
| Customer and admin Home | Heading starts at x = 40, every other screen at x = 32 | 8 px | `Dashboard` and `Theme` use `PAGE + 8` side padding |
| Every form (login, Flights From/To, payment) | Labels start about 2 px left of the field's visible edge | 2 px | FlatLaf focus inset on text fields; labels have none |
| Flights and Schedule filter bars | "Show" button centre sits 14 px below the date fields' centre | 14 px | button and fields are in layouts that align to the top, not the centre line |
| Payment | Pay button's right edge ends 7 px short of the field edge | 7 px | button row has its own insets |
| Seat screen | Seat map card top is 3 px below the summary card top | 3 px | different parent borders |
| Register | Space from the password hint to the next label is 18 px; between every other field and the next label it is 37 px | 19 px | hint added under the field without changing the pair spacing |
| Routes | Table top and the card top differ by 2 px | 2 px | same as above |

*Fix.* A single `Space` token set (4, 8, 12, 16, 24, 32) and a `Align` helper; replace the 62 hand-written `createEmptyBorder` / `Insets` literals in the presentation package (they are the cause of every row above). `FormField` owns its inset so label and field edges match. Filter bars use one `FilterBar` component with centre alignment. A rendered-geometry test compares the left edges and the centre lines of the pieces (see R2).

### F4. Flight results are a table

*Evidence.* Flights show as an 8-column grid: Flight, Airline, Route, Departs, Arrives, Duration, Seats left, From. The price column is called "From", which reads like a label for the previous column. Seats left is right-aligned but the price next to it is left-aligned.

*Fix.* Result cards (one per flight) in a scrolling list:

```
 07:00 AM  ───── 1h 30m ─────▶  08:30 AM          from $184.00
 YYC Calgary                    YVR Vancouver      180 seats left
 Air Canada · AC101 · Airbus A320                  [ Select ]
```

A `JList` with a custom cell renderer on the existing `FlightTableModel` data keeps keyboard navigation and double-click. Sort chips above the list (Earliest, Cheapest, Shortest) work on the data already loaded. This is the main visual change for the customer.

### F5. Home screens duplicate the tabs

*Evidence.* Customer Home has three cards (Search flights, My reservations, My profile) and the bar above it has the same three tabs. The third card sits alone on its row. One screen has four names: tab "Find flights", card "Search flights", title "Find available flights", step "Find a flight". The greeting says "Hello, customer1" while the account name is "Casey Customer" (the bar chip also shows the username).

*Why it matters.* A screen that only repeats the navigation makes the user read the same thing twice. Four names for one place makes the app feel inconsistent.

*Fix.*
- One name per place, a naming table in this plan (section 6).
- Home becomes useful or goes away (open decision 1). A useful Home uses only data the app already has:
  - Customer: "Your next trip" card (the first Confirmed reservation by departure, with route, date, seat, confirmation number and a Copy button); a find-a-flight box (From, To, Date, Search) that opens the results; the last three reservations with status. Nothing invented: all three come from `ReservationController.history` and the search controller.
  - Agent: flights today and in the next 7 days (`ScheduleController`), and a customer search box.
  - Admin: flights in the next 7 days, number of routes, number of aircraft (counts from existing list calls).
- The greeting uses the customer's name when the user is a customer, the username otherwise.

### F6. Reservations: select a row, then press a button

*Evidence.* The table has 7 columns and two disabled buttons under it ("Pay now", "Cancel reservation"). With nothing selected both are grey; a tooltip explains why. The Price column is left-aligned while Seats-left in search is right-aligned.

*Fix.* Reservation cards, grouped Upcoming, Past and Cancelled, each with status badge, route and times, seat, price, confirmation number, and the actions that apply to that card (Pay now, Change, Cancel). No selection step; one click fewer for cancel and pay. The agent's "reservations of this customer" screen uses the same card list. The table stays where a table is right (schedule, payment records).

### F7. No elevation, and one success mark that breaks the language

*Evidence.* No shadow anywhere. The only things that separate a card from the page are a 1 px border and a white fill. The confirmation success mark is a sharp-cornered green square, while every other shape is rounded (8 px) or round.

*Fix.* Two surface levels only: the page, and a card with a 1 px border plus a soft two-layer shadow (navy at 6 % and 3 %, 1 px and 6 px offset), and a stronger shadow for popups (calendar, dialogs). Implemented as one `SurfaceBorder` painted in `Theme.card`, so no screen changes. The success mark becomes a filled circle. Restraint rule: no shadow on table rows, buttons or inputs.

### F8. Login and register

*Strengths.* Full logo, one card, clear primary button, Enter works, field-level errors, reveal button.
*Gaps (measured).* On login the card is 380 px wide in a 1100 px window with the page blank around it. Register's password field has no placeholder while the others do, and its logo is the small mark, not the full logo. At 1100 x 692 the register card touches the top edge of the window.
*Changes.* Split the login page into a left navy panel with the mountain mark, the tagline and nothing else, and a right panel with the form (the full logo is already on the left, so the card shrinks). Keep the form card on small windows (< 900 px wide). Equal vertical rhythm using `Space`. Password placeholder "At least 8 characters". Register uses the same split.
*Acceptance.* At 1100 x 692, 960 x 640 and 1400 x 900 nothing touches an edge; left edges of labels and fields match.

### F9. Seat selection

*Strengths.* Arrow keys, aisle gap, taken seats marked with a cross, summary card, legend, Business and Economy tints.
*Gaps.*
- The legend chips (Business, Economy, Taken, Your seat) are filled rectangles in the same size and style as the seats; at a glance they look like more seats.
- No cabin headings: Business rows 1 to 2 and Economy rows 3 on are only distinguished by tint.
- The summary card shows no passenger and no class price; the price changes with the class but the screen does not say that.
- Back and Continue sit at the far bottom right, about 250 px below the summary card they act on.
*Changes.* Legend as small swatches with text (dot, not button). A cabin label row above each cabin ("Business, rows 1 to 2", "Economy, rows 3 to 28") and a thin gold rule above Business only. Summary card gains passenger and the price of the chosen class, and Back/Continue move into it (sticky at its bottom). Tooltip on a hovered seat: seat number, class, price.
*Acceptance.* Continue visible without scrolling at 960 x 640 (checked by the layout test); every seat has an accessible name with class and price (the name exists today; the price is the addition).

### F10. Payment

*Strengths.* Order summary, error per field, Tab order Name, Number, Type then Expiry (tested), Pay button carries the amount.
*Gaps (measured).* The card number field has no placeholder or grouping while Name and Expiry have; the form column is 440 px and the page has 25 % empty on the right; the summary lacks the passenger and the seat class; nothing tells the user how the card is handled (the code keeps only the masked number, `**** **** **** 1111`, and the charge is simulated; see `Payment` and `PaymentDetails`).
*Changes.* Card number groups as typed (`4111 1111 1111 1111`) with a card icon for the type; expiry placeholder `MM/yy` stays; summary adds passenger, class and a "paid with" line after payment; one line under the button saying only the last four digits are kept (true today), worded so it does not promise more than the simulated charge does. Back moves left of Pay, aligned with the field edge.
*Check before building.* Whether a Luhn check exists in the card classes (grep found none). Adding one is business logic: it must be agreed first (section 8).

### F11. Confirmation

*Strengths.* Gold rule, big confirmation number, Copy, "Open folder", Done and Book another.
*Gaps.* The trip is a key/value list with the label at the left edge and the value 1000 px away (reading a row means tracking a long line); no itinerary at a glance; right 40 % empty; the green square.
*Changes.* A boarding-pass style card: navy top band with the route and times large (`YYC 7:00 AM → YVR 8:30 AM`, duration between), the details in a two-column grid below (Passenger, Flight, Seat, Class, Amount), the confirmation number in the gold-ruled strip on the side. Card capped at 720 px. This is the one place where the brand may be loud, because it is a document people keep.

### F12. My reservations

See F6. Empty state when there are none: the existing `EmptyState` with a "Find a flight" button.

### F13. Agent: customers, modify, payment records, schedule

*Strengths.* Customers is the model screen (live search, list plus panel).
*Gaps.*
- Customer panel title is the generic word "Customer"; it should be the person's name with their avatar initial, and the number of reservations.
- Payment records stacks two tables; the top one (customers) takes half the page to show one row, then the payments table shows an instruction text. A list on the left and the payments on the right, like Customers, uses the same pattern and the space better.
- Schedule prints the date twice per row; group by day with a day header ("Wed, Dec 2 · 4 flights") and show only times in the rows.
- The "Show" button after the date pickers is redundant now that a picked day loads the flights. Keep it only when a typed date is not yet applied.
*Changes.* As listed; payment records gain totals (paid, refunded) computed from the rows on the screen.

### F14. Admin: flights, routes, aircraft

*Strengths.* Sort, filter, card shown only when needed, red confirm dialog, seat total while typing.
*Gaps (measured).*
- The Flights card scrolls inside itself, and at 1100 x 692 the Departure and Arrival fields are cut off at the bottom of the visible card. The two most important fields of a flight are hidden. The space between one field and the next label is about 37 px.
- Departure and Arrival are text fields with the pattern in the placeholder, while the same screen has date pickers above. No time picker.
- Routes are listed in storage order (Edmonton to Calgary, Ottawa to Toronto, Montreal to Toronto, Vancouver to Calgary, then three Calgary routes, then three Toronto routes), so the same origin shows up in scattered places and in no alphabetical order.
- The flights table does not show how full a flight is.
*Changes.* Tighten the space between a field and the next label from 37 px to 16 px (about 84 px saved over four gaps), group the card into "Flight" (number, price), "Route and aircraft" and "Times" so Times is visible without scrolling at 960 x 640; a date field plus a time field for each time (the date field already exists; the time field is a spinner or a 15-minute list); routes sorted by origin then destination and grouped by origin; the flights table gets a "Seats left" column from the details query that already exists (`FlightSearchController.details`). Anything that needs a new query is in section 8.

### F15. Dashboards for agent and admin

Same as F5. The agent's "Reservations" card is a dead end today (decision 2).

### F16. Dialogs and popups

*Strengths (seen at M7).* ConfirmDialog, promotion dialog with the mark and gold rule, splash.
*Gaps.* I did not render the calendar popup for this plan, so whether it needs the stronger popup shadow from F7 is for the R7 audit to judge. `Alert` already carries an icon per kind, so nothing to add there.

### F17. Typography details

*Strengths.* A small, consistent scale; Inter; bold page titles.
*Gaps.* Table header text is regular weight in muted grey at 13 px with full vertical grid lines between header cells (visible in every table); numbers are not aligned (price left in reservations, right in search); labels are 13 px bold navy, equal in weight to the field text so the label does not recede.
*Changes.* Header cells: 12 px, semibold, muted, no vertical separators, 1 px bottom line. Numeric columns (price, seats, amount) right-aligned through one shared renderer. Labels: 13 px medium weight. Tabular figures from Inter are not available through Swing text rendering on every JDK; I will test `TextAttribute` features first and fall back to right alignment, so this is marked a spike, not a promise.

### F18. Threading

*Evidence.* 47 controller call sites in the views run directly in listeners on the Event Dispatch Thread (flights screen 10, schedule 6, search 5, routes 4). Only start-up is off the EDT. All data is local SQLite and everything measured so far is instant, so there is no visible freeze.
*Plan.* Do not add spinners or background workers for work that takes under about 100 ms; that adds states and bugs for nothing. Add one timing test that runs a seeded search, the schedule load for 30 days and the flight list and fails above 100 ms; if a call ever exceeds it, move that call to a `SwingWorker` with the existing `Alert` as the failure path.

## 3. Navigation: before and after

| | Before | After |
|---|---|---|
| Customer | Home (3 cards) / tabs: Find flights, My reservations, My profile | Home with next trip, quick search and recent bookings / same tabs |
| Agent | Home (cards incl. a dead-end Reservations) / tabs: Customers, Payment records, Schedule | Land on Customers (or Home, decision 1) / same tabs; Reservations card removed |
| Admin | Home (4 cards) / tabs: Flights, Routes, Aircraft, Schedule | Land on Flights (or Home) / same tabs |
| Booking | 4-step strip, Back buttons | same strip; Back stays only inside the card that acts (not floating bottom right) |
| Agent modify | Customers, Reservations, Modify, then "Back to customers" | A breadcrumb in the page header: Customers › Casey Customer › Modify reservation; each part is a link |
| One screen, four names | Find flights / Search flights / Find available flights / Find a flight | Tab "Find flights", title "Find flights", step "Choose flight", Home button "Find flights" |

How it reduces friction: removes the duplicate Home for roles that only repeat the tabs, one name per place, a breadcrumb for the only three-level path, and actions placed in the card they apply to.

Naming table (tab = title = card): Find flights, My reservations, My profile; Customers, Payment records, Schedule; Flights, Routes, Aircraft, Schedule. Step names stay: Find a flight, Choose a seat, Pay, Done.

## 4. Screen-by-screen matrix

| Screen | What works | What needs improvement | Proposed change | Priority | Files |
|---|---|---|---|---|---|
| Login | Logo, one card, Enter | Blank page around a narrow card; rhythm | Split panel, tokens | Medium | `LoginView`, `Theme`, `Space` |
| Register | Field errors, scrolling | Hint spacing, no placeholder, edge touch | Same split, fix rhythm | Medium | `RegisterView`, `FormField` |
| Customer Home | Cards, icons | Duplicates tabs, 55 % empty, name | Next trip, quick search, recent | High | `CustomerDashboard`, `Dashboard`, new `TripCard` |
| Find flights | Search card, swap, calendar, auto-search | Table of 8 columns, "From" header | Result cards, sort chips, `Format` | High | `FlightSearchView`, new `FlightResultList`, `FlightTableModel` |
| Choose seat | Arrow keys, aisle, summary | Legend looks like seats, no cabins, buttons far | Cabin labels, swatch legend, sticky actions | High | `SeatSelectionView`, `SeatGridModel` |
| Payment | Order summary, Tab order | Card number unformatted, summary thin | Grouping, summary, Back placement | Medium | `PaymentView`, `SummaryCard` |
| Confirmation | Gold rule, Copy | Long key/value, square mark, empty right | Boarding-pass card | High | `ConfirmationView`, new `ItineraryCard` |
| My reservations | Status badge, tooltips | 80 % empty, select-then-act | Reservation cards, groups, inline actions | High | `MyReservationsView`, new `ReservationCard` |
| Profile | Save only when changed | 45 % empty, one lonely card | Two columns: details, account summary | Low | `ProfileView`, `CustomerForm` |
| Customers (agent) | List plus panel | Generic panel title | Name, avatar, reservation count | Medium | `CustomerManagementView` |
| Modify | Hints, tooltip | No breadcrumb | Breadcrumb | Low | `ModifyReservationView`, `AppBar` |
| Payment records | Live search | Two stacked tables | List left, payments right, totals | Medium | `PaymentRecordsView` |
| Schedule | Pickers, quick ranges | Date twice per row, Show button | Day groups, times only | Medium | `ScheduleView`, `ScheduleTableModel` |
| Flights (admin) | Sort, filter, card on demand | Hidden times, text times, tight card | Grouped card, date plus time, seats left | High | `FlightManagementView` |
| Routes | Card form | Storage order, repeats | Sort and group | Low | `RouteManagementView` |
| Aircraft | Seat total live | Same layout rules | `Space`, table sizing | Low | `AircraftManagementView` |
| Dialogs and popups | Red confirm, promo | Flat popup | Popup shadow | Low | `ConfirmDialog`, `DatePickerField` |

## 5. Design system: keep, change, add

**Keep:** `Palette` values, `Type` scale, FlatLaf plus Inter, `AppBar`, `StepStrip`, `Badge`/`Status`, `SearchField`, `DatePickerField`, `FormField`, `Alert`, `EmptyState`, `ConfirmDialog`, `Screen` defaults, `HintTable`.

**Change:**
- `Theme.card` gets `SurfaceBorder` (border plus soft shadow); popups get the stronger one.
- `HintTable` header: 12 px semibold, no vertical separators; one shared right-aligned numeric renderer; tables size to rows with a 5-row minimum.
- `FormField`: owns its left inset so label and field edges match; label weight medium.
- `Dashboard` padding fixed to `Space.PAGE` (the `+ 8` goes).
- Success mark becomes a circle.

**Add:**
- `Space` (4, 8, 12, 16, 24, 32) and `ContentPane` (centred, max 1200 px).
- `Format` (dates, times, durations, money).
- `FilterBar` (centre-aligned row for date pickers, buttons, filter box).
- `FlightResultList`, `ReservationCard`, `ItineraryCard`, `TripCard`, `Breadcrumb`, `Avatar` (the bar's circle, reused).
- Token tests: no `createEmptyBorder` with a number that is not a `Space` value; every date shown goes through `Format`.

**States.** Hover: `ROW_HOVER` on rows and cards. Pressed: one step darker (`BLUE_800`). Focus: a 2 px `BLUE_600` ring on every focusable custom component, verified in the audit. Disabled: the existing `DISABLED_*` pair.

**Tokens stay in one place.** `PaletteTest` keeps failing the build for a hex outside `Palette`.

## 6. Functional enhancements

Only enhancements that use existing data and queries are in the milestones. Anything that needs new business logic is separate (section 8).

| Enhancement | Problem it solves | Uses | Dependency |
|---|---|---|---|
| Sort chips on results (Earliest, Cheapest, Shortest) | Comparing flights needs reading eight columns | already loaded rows | none |
| Date strip (the three days either side of the chosen day) | Users change the day often | seven `search` calls, no new query | none; if slow, load lazily |
| Reservation groups and filter by confirmation number | Finding a booking in a long list | `history()` already returns all | none |
| Seat tooltip with class and price | Price surprise at the next screen | `PricingPolicy`, seat data | none |
| Card number grouping | Typing 16 digits blind | `JFormattedTextField` or document filter | none |
| Inline validation on leaving a field | Errors currently show on submit | `FormField` | none |
| Next-trip and recent bookings on Home | Home does nothing today | `history()` | none |
| Totals on payment records | Agents add by hand | rows on screen | none |
| Seats left and booked on admin flights | Can't see how full | `details()` per row | one call per visible row; measure first |

## 7. Milestones (round two)

Named R1 to R8 so they are not confused with M0 to M8. One PR per milestone, into `dev`, each with `mvn clean verify` green and the 54-case test plan updated where a screen changes.

**R1. Tokens and consistency.** Tasks: `Space`, `Format`, `SurfaceBorder`, success circle, table header style, numeric renderer, replace the 62 spacing literals, route every date through `Format`. Files: `Theme`, `Palette` (unchanged), `HintTable`, the five table models, all views (spacing only). Dependencies: none. Risks: wide, mechanical change, test text for dates changes. Tests: token tests above; model tests compare `Format` output. Acceptance: no spacing literal outside `Space`; no `DateTimeFormatter.ofPattern` for display outside `Format`; contrast test still green.

**R2. Layout and alignment.** Tasks: `ContentPane`, tables sized to rows, `FilterBar`, `FormField` inset, `Dashboard` padding, button rows aligned to field edges, card top alignment. Files: `MainFrame` (host), every `*View`. Risks: scroll panes and `ScrollableForm` interplay. Tests: a geometry test (`LayoutAlignmentTest`) that lays out each screen at 1100 x 692 and 960 x 640 and asserts: heading x equal on all screens, label x within 1 px of field x, filter bar centre lines within 1 px, last button right edge equals field right edge. Acceptance: the seven rows of the F3 table pass; no table taller than rows + 1 empty row unless it has more rows.

**R3. Navigation and Home.** Tasks: naming table applied, `Breadcrumb`, Home content (customer: next trip, quick search, recent; agent and admin per decision 1), remove agent Reservations card, greeting uses the name. Files: `Dashboard`, `*Dashboard`, `NavModel`, `ViewId.label()`, `AppBar`, `ModifyReservationView`. Dependencies: decisions 1 and 2. Tests: `NavModelTest`, a new `HomeViewTest` (next trip is the earliest confirmed future reservation; empty state when none). Acceptance: every screen has exactly one name; breadcrumb links work by keyboard.

**R4. Customer journey.** Tasks: `FlightResultList` with sort chips and date strip, seat screen changes, payment changes, `ItineraryCard`, `ReservationCard` and grouping. Files as in section 4. Risks: replacing the results table touches many tests (`FlightSearchViewTest`); keep the model API and test the list through it. Tests: sort order tests, card text tests, seat Continue visible at 960 x 640, booking click count unchanged (still 4 to payment). Acceptance: the customer path from login to confirmation shows no data table (results, reservations and the trip are cards); screenshots of every state reviewed.

**R5. Agent and admin.** Tasks: customer panel header, payment records two-pane, schedule day groups, Flights card regrouping with date plus time fields, routes sort and grouping, seats left column. Risks: flights card is the most complex form; the time field is new. Tests: existing view tests updated; sort and group tests. Acceptance: Departure and Arrival visible without scrolling at 960 x 640; routes sorted.

**R6. Interactions and small functions.** Tasks: inline validation on blur, card number grouping, seat tooltips, date strip polish, popup shadow, Alert icons, confirmation of Enter and Esc on every dialog. Tests: `FormFieldTest`, a payment grouping test, seat tooltip test. Acceptance: no field accepts a character that can never be valid for its type where the control can prevent it.

**R7. Accessibility, scale, performance.** Tasks: verify and fix the focus ring on `TaskCard`, seat toggles, calendar day buttons; render and test at 125 % and 150 % scale (`-Dsun.java2d.uiScale`); timing test (F18); accessible names for every new card; keyboard-only run of the three roles. Acceptance: contrast tests pass for new colours; at 150 % nothing is cut off at 960 x 640 or it scrolls; all screens usable without a mouse; timing test under 100 ms.

**R8. Regression and final audit.** Tasks: render every screen at three sizes again, compare with the F3 geometry test, update `docs/test-plan.md` (cases for the cards, sort, breadcrumb, Home), refresh README screenshots, update this plan's progress table. Acceptance: all 54 plus the new cases listed; the user's manual run of the plan on a real display completed (nobody but a person can do this).

## 8. Items that need new business logic or a decision, not just UI

1. **Find a reservation by confirmation number across all customers** (agent): needs a new DAO query and controller method. The UI-only version (filter inside one customer's list) is in R4.
2. **Luhn check on the card number:** none exists; adding it is business logic and a test plan change.
3. **Seats left and booked per flight on the admin table:** a per-row `details` call works but may be slow with many flights; a single grouped query would need a new DAO method.
4. **Export the schedule (CSV or print):** a new requirement; not planned.
5. **Window and aisle seat filters:** depend on `SeatLayout` giving the seat position; not checked.

## 9. Impact-based order

Scored 1 to 5 (benefit higher is better; effort and risk lower is better).

| Item | Visible | UX | Effort | Risk | Order |
|---|---|---|---|---|---|
| F1 empty space, F3 alignment | 5 | 4 | 2 | 2 | 1 (R1, R2) |
| F2 dates through `Format` | 4 | 4 | 2 | 1 | 2 (R1) |
| F4 flight result cards | 5 | 5 | 4 | 3 | 3 (R4) |
| F6 reservation cards | 5 | 5 | 3 | 2 | 4 (R4) |
| F11 confirmation boarding pass | 5 | 3 | 2 | 1 | 5 (R4) |
| F5 useful Home, naming | 4 | 4 | 3 | 2 | 6 (R3) |
| F7 elevation, success circle | 3 | 1 | 1 | 1 | with R1 |
| F9 seat screen | 4 | 4 | 3 | 2 | 7 (R4) |
| F14 admin Flights card | 3 | 4 | 3 | 3 | 8 (R5) |
| F13 agent screens | 3 | 3 | 3 | 2 | 9 (R5) |
| F8 split login | 3 | 1 | 2 | 1 | 10 (R6) |

R1 and R2 are the cheapest and change the most screens at once. R4 is the largest visible step and should be reviewed with screenshots before moving on.

## 10. Verification

- Before and after renders at 960 x 640, 1100 x 692 and 1400 x 900 for every screen, kept in `docs/screenshots` for the main ones.
- The geometry test (R2) runs in CI; it is the evidence that alignment fixes hold.
- Token tests keep spacing, dates and colours from drifting back.
- `mvn clean verify` stays green at every milestone (939 tests today).
- The 54-case test plan is the acceptance run. A person has to run it on a real display; I cannot do that.
- A short user check after R2 and after R4 with the real jar, because those two change what the app looks like the most.

## 11. Decisions

Settled on 2026-11-15 (the recommended option was taken for each):

1. Login lands on Home for customers, on Customers for agents, on Flights for admins. Home stays reachable by the logo.
2. The agent Home "Reservations" card is removed.
3. Times are 12 h (`7:00 AM`).
4. The logo-sampled palette stays; the brief's hex values are not used.
5. Flight results become cards.
6. Section 8 (new business logic) is out of scope for now: this round is UI only.
7. No GitHub issues are created for R1 to R8; the milestones live in this document.

## 12. Final quality checklist

- [ ] One name per screen across tab, title, card, step.
- [ ] Left edge of every page heading is the same x.
- [ ] Left edge of every label equals its field edge (within 1 px).
- [ ] No table shows more than one empty row of white space unless it holds more rows.
- [ ] Every date, time, duration and money amount comes from `Format`.
- [ ] No spacing literal outside `Space`; no colour literal outside `Palette`.
- [ ] Cards have the same border, radius and shadow; popups have the stronger shadow.
- [ ] Status is shown by dot and word, never colour alone.
- [ ] Every interactive element shows a visible focus ring.
- [ ] Every screen works with the keyboard only, in a logical Tab order.
- [ ] Contrast test passes; new colours are added to it.
- [ ] 960 x 640 shows the primary action without scrolling on every screen.
- [ ] 125 % and 150 % scaling: nothing cut off.
- [ ] Booking to payment is still 4 clicks.
- [ ] `mvn clean verify` green; ArchUnit green; no data-layer import in presentation.
- [ ] README screenshots and test plan match the screens.
- [ ] The manual run of the test plan is done on a real display.
