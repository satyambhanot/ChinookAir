# ChinookAir UI/UX transformation plan

Status: plan only. No application code has been changed for this document.
Branch it was written on: `ui/redesign` (6 commits already there, see section 1.4).

## Progress

| Milestone | State | Notes |
|---|---|---|
| M0 Brand assets | Done | Logo, mark, six icon sizes, transparent background; window and Dock icon |
| M1 Tokens and fonts | Done | `Palette`, `Type`, `FlatLaf.properties`, Inter; contrast test fails the build below 4.5:1 |
| M2 Components | Done | Icons, badges, form field, search field, empty state, summary card, alert, date picker |
| M3 Shell and navigation | Done | Navy bar, role tabs, step strip. Deviation: the step strip sits under the bar and the tabs stay, so a user can leave a booking |
| M4 Login and customer journey | Done | Login, register, find flights, seats, payment, confirmation, my reservations, profile |
| M5 Agent screens | Done | Customers (live search, detail panel), modify, payment records, schedule (pickers, quick ranges) |
| M6 Admin screens | Done | Flights (sort, filter, card on demand, red confirm dialog), routes, aircraft (seat total while typing). Reschedule on the schedule screen still uses text fields |
| M7 Polish | Done | Enter and first focus on every screen, numbered tab shortcuts, table names for screen readers, start-up window with the logo (database made in the background), promotion dialog look, Back only where it leads somewhere. Deviations: payment is not moved to a background thread because nothing in it is slow, and rescheduling uses a card instead of a dialog |
| M8 Final audit | Not started | Every screen at three sizes, run the test-plan cases, fix what is found |

Deviations from this plan while building M4: the live password checklist on registration was left out, because the rules belong to the controller and a copy in the screen could drift; the reservation detail strip was replaced by tooltips that say why Pay and Cancel are disabled; login still lands on Home, and the agent's Reservations card is still there (decisions 1 and 5 in section 12 are open).

## 0. How this plan was made, and what was and was not checked

| Source | What I did |
|---|---|
| Logo | Opened `ChinookAir Mountain Flight Logo.png` (1536 x 1024, RGB, no transparency, 1.2 MB) and sampled its pixels with a small Java program. The colours below come from those samples, not from the brief. |
| Code | Read `MainFrame`, `Navigator`, `ViewId`, `Dashboard`, `LoginView`, `RegisterView`, `FlightSearchView`, `SeatSelectionView`, `PaymentView`, `ConfirmationView`, `Dialogs`, `AppContext` and the table models; grepped every view for threading, focus, tooltip, icon and resource handling. |
| Screens | Painted 21 screen states to PNG with a throwaway program (headless paint of the real Swing components, no real window) and looked at them. |

**Confirmed by looking at a rendered image:** login, register, customer dashboard, flight search (empty, no result, with a result), seat map (before and after choosing a seat), payment (empty and with an error), confirmation, my reservations, profile, customers, payment records, admin dashboard, flights, aircraft.

**Only read from source, not looked at:** routes, schedule (seen once, earlier), modify reservation, the promotion dialog, every `JOptionPane` pop-up, the agent dashboard (same class as the others).

**Not verified at all, because a painted image cannot show it:** hover, pressed and focus looks, keyboard order, window resizing, how fonts look on Windows or Linux, real start-up time on another machine. The plan has a step for each of these.

## 1. Executive assessment

### 1.1 Where the application is today

The six commits on `ui/redesign` moved it from "operating system defaults" to "clean generic flat app": FlatLaf, a top bar with breadcrumb, dashboard cards, tables that fit their text, blue primary buttons. That is a good base, but it is **still a generic flat app with a made-up blue**. Nothing in it says ChinookAir.

The accent colour I picked earlier (`#0B63CE`) is not from the logo. The logo's blue is about `#0779C8`, its navy about `#093151`, and it also has a sun-gold and a sky cyan that the UI does not use at all. The logo itself appears nowhere: there is no window icon, no logo on the login screen, no logo in the top bar. The word "ChinookAir" in the top bar is plain text.

### 1.2 Audit findings

Impact order. "Seen" = in a rendered image. "Source" = read from code only.

| # | Level | Finding | Evidence | Impact | Fix (section) |
|---|---|---|---|---|---|
| A1 | Critical | First start looks frozen for about 7 seconds. `ChinookAir.main` calls `AppContext.create()` (create database, seed 60 days of flights) before any window exists, with no splash or message. | Source: `ChinookAir.java`, `AppContext.create()`. Measured earlier at about 7.5 s on this Mac. | A new user may think the app crashed and start it twice. | Splash window with the logo and a real progress line (M7). |
| A2 | Critical | Reservation status shows the raw enum name: `PENDING_PAYMENT`. | Seen: My reservations. Source: `ReservationTableModel` line 62 `reservation.status().name()`. | Looks like a bug. Colour-only meaning is also an accessibility problem. | Status badge with words and colour (M2, M4). |
| A3 | Critical | The confirmation screen is a monospaced text dump with a temp-file path in it. | Seen: Confirmation. Source: `ConfirmationView` uses `JTextArea` with `Font.MONOSPACED`. | This is the moment the customer has paid. It should be the best screen in the app and is the worst. | Booking summary card (M4). |
| A4 | High | The logo and brand colours are absent. No `setIconImage`, no image resource at all (`src/main/resources` holds only `db/schema.sql`). | Source: grep for `setIconImage`, `getResource`. | The app does not look like the brand in the logo. Dock and taskbar show the default Java icon. | Brand assets and window icons (M0). |
| A5 | High | The booking journey is long: from the dashboard, 8 clicks to reach payment, 9 to pay, 13 for an agent to modify a reservation. The 4-click target in P9-02 is not met. | Test plan `docs/test-plan.md`, "Clicks from the dashboard". | The most common task is the slowest one. | Role navigation tabs, remembered search, stepper (M3, M4). |
| A6 | High | Dates are typed as text (`yyyy-MM-dd`, `yyyy-MM-dd HH:mm`). Format now shows as a grey hint, but there is no picker and no live check. | Seen: search, flights. Source: `InputParser`. | Typing mistakes; flight search is the entry point of the whole app. | Date picker component (M2, M4). |
| A7 | High | Seat map ignores the aircraft shape: no aisle, no row numbers on both sides, no summary panel; the chosen seat is only described in a bold line under the grid. | Seen: seat screen. Source: `SeatSelectionView`, `SeatGridModel`. | Hard to read a 30-row plane; easy to pick the wrong seat. | Seat map v2 (M4). |
| A8 | High | The payment form floats in the middle of an empty page, with no order summary and no card formatting. | Seen: payment. | Customer cannot see what they are paying for while paying. | Two-column payment screen (M4). |
| A9 | High | Navigation is "dashboard, then one screen, then Back". Moving from Flights to Routes (admin) means going home first. | Source: every `backButton` calls `navigator.show(<dashboard>)`. | Extra clicks for people who work across screens (agents, admins). | Top navigation tabs per role (M3). |
| A10 | High | Everything database-related runs on the UI thread. | Source: no `SwingWorker` anywhere; searches and payments call controllers directly in action listeners. | Fine with SQLite and 60 days of data, but the window cannot repaint during a slow call. | Keep for local reads; use a background worker only for start-up and payment (M7). |
| A11 | Medium | Per-screen layout is inconsistent: outer padding is 15 or 20 or 30 px depending on the view; forms are centred on some screens and left-aligned on others; label text and field widths vary. | Seen: profile (left heading, centred form), payment (centred, heading inside form), flights (form on the right, no heading alignment). | Looks unfinished. | Page scaffold and form grid components (M2). |
| A12 | Medium | Pop-ups are plain `JOptionPane`. A cancel/remove confirmation looks identical to an information message. | Source: `Dialogs`, 10 call sites. | No visual warning before something destructive. | `ConfirmDialog` with a danger style (M7). |
| A13 | Medium | Keyboard: only the login screen focuses a field on show; no default (Enter) button except the promotion dialog; no tooltips; no accessible names. | Source: grep `setDefaultButton`, `setToolTipText`, `getAccessibleContext`. | Mouse-only in practice; screen readers get no useful names. | Accessibility pass (M7). |
| A14 | Medium | Tables cannot be sorted or filtered (`setAutoCreateRowSorter` off). The flights table has 370 rows. | Seen: flights. | Admin has to scroll to find a flight. | Sort and a filter box (M5, M6). |
| A15 | Medium | The agent dashboard's "Reservations" card opens `MY_RESERVATIONS`, which has no customer chosen when an agent arrives from home. | Source: `AgentDashboard`. Known since P8. | Dead-end card. | Remove the card; reservations are reached through Customers (needs your decision, section 12). |
| A16 | Medium | Minimum window is 800 x 600. The flights screen with its side form is already tight there. | Source: `MainFrame.setMinimumSize`. Not verified visually. | Possible clipped forms on small laptops. | Raise minimum to 960 x 640 and verify at that size (M8). |
| A17 | Low | Dashboard cards have no icon and the lower half of the window is empty. | Seen: dashboards. | Looks sparse. | Icons; compact layout (M3). |
| A18 | Low | Colour is the only difference between business, economy and taken seats. | Seen: seat map. | Fails the "not by colour alone" rule. | Seat class text on the seat and a pattern for taken (M4). |

### 1.3 What is already good and stays

Layer separation (presentation / control / entity / data), the `Navigator` and `Screen` interfaces, the controllers, all 769 tests, `HintTable` (auto-fit columns, empty text), `TaskCard`, `Theme` helpers, double-click to select a flight. The plan below changes how things look and how screens are reached. It does not touch business rules, the database or role permissions.

### 1.4 What exists on `ui/redesign` today (so nothing is redone)

`Theme` (tokens, installs FlatLaf), `AppBar` (breadcrumb, user, Log out), `TaskCard`, `HintTable`, login and register on a card, seat colours, placeholders for dates. These are kept and reworked in place, not rebuilt.

## 2. Target design vision

### 2.1 One sentence

A calm, confident flight-booking tool in the colours of the logo: deep mountain navy for structure, aviation blue for the one thing you can do next, a thin line of sky and a touch of sun-gold for recognition.

### 2.2 Colour philosophy (taken from the logo)

Sampled from the PNG:

| Logo part | Sampled value | Where it comes from |
|---|---|---|
| "Chinook" wordmark, mountain | `#093151` (mountain shadows `#082848`) | dominant colour, about 110 k px |
| "Air" wordmark | `#0779C8` | about 35 k px |
| Sun | `#FDB52E` | about 24 k px |
| Sky swoosh | `#2DB1E9` | about 13 k px |
| Glacier tint | `#A8D8F8` | about 6 k px |

The brief's reference values (`#102A43`, `#1679C5`, `#36B7E9`, `#F2B544`) are close but slightly different from the real file. I propose the logo's own values for brand elements so the UI and the logo match exactly.

**Rules for using them**
1. Navy is structure: top bar background, headings, table header text.
2. Blue is action: primary buttons, links, selected tab, focus ring. Only one filled blue button per screen.
3. Cyan and gold are never text colours on a light background (contrast 2.45:1 and 1.78:1, measured). Cyan is used for a 3 px accent line and the selected-seat outline; gold only for the sun dot on the logo mark, a "Business" seat tint and one highlight on the confirmation screen.
4. Everything else is white and a cool grey-white.

### 2.3 What visibly changes

| Today | After |
|---|---|
| White top bar, text "ChinookAir" in an unrelated blue | Navy top bar, the logo mark in colour on a small white tile, role tabs, thin cyan underline on the selected tab |
| Dashboard of four boxes | Role tabs make the boxes unnecessary for moving around; Home stays as a welcome page with real, useful content (section 5) |
| Grey page, white tables | Cool page background, white cards with a 1 px border, table header in a navy tint |
| Raw `PENDING_PAYMENT` | Amber badge "Awaiting payment" with a dot |
| Text dump after payment | Booking summary card with route, times, seat, price, confirmation number and a gold highlight on the number |
| Plain login form | Split login: left navy panel with the logo and mountains line art, right form (only if it fits at 960 px; falls back to a single card) |

## 3. Screen-by-screen blueprint

Priority: C critical, H high, M medium, L low. "Files" are the Java files under `src/main/java/ca/ucalgary/chinookair/presentation/`.

### 3.1 Application shell (all screens)

| Field | Detail |
|---|---|
| Current problems | White bar with a text brand; breadcrumb duplicates the page heading; Log out is a normal button; no window icon (A4, A9). |
| Redesign | 56 px navy bar. Left: logo mark (about 32 px tall, on a white rounded tile so the PNG's white background is not an issue) + word "ChinookAir" in white; clicking it goes Home. Centre-left: role tabs (section 5). Right: user chip (initial circle + name + role in small text) and a Log out link-button. 3 px cyan line under the selected tab. During booking the tabs are replaced by a 4-step stepper. |
| UX | Home is always one click away; the current place is always shown; no screen has to repeat the page name in a breadcrumb. |
| Components | `AppBar` (rework), `NavTabs`, `Stepper`, `UserChip`, `BrandMark`. |
| Files | `AppBar.java`, `MainFrame.java`, `ViewId.java`; new `BrandMark.java`, `NavTabs.java`, `Stepper.java`. |
| Priority | H |
| Dependencies | M0 logo assets, M1 tokens. |
| Acceptance | On every post-login screen the bar shows logo, tabs for that role, user, Log out; selected tab matches the current screen for all 18 `ViewId`s; bar contrast at least 4.5:1 for text; tab order logo > tabs > Log out; existing `AppBarTest` updated and passing. |

### 3.2 Login (`LoginView`)

| Field | Detail |
|---|---|
| Current problems | Text title "ChinookAir" in plain blue on a card; no logo; labels beside fields; message line reserved even when empty (seen). |
| Redesign | Logo (about 220 px wide, cropped, on white) above the form; labels above fields; Show/Hide password toggle; Log in is the default button; error text appears under the form with an icon and `role="alert"`-style accessible text; optional left navy panel with a mountain ridge line drawn in Java2D from the logo silhouette (decorative, hidden below 960 px). |
| UX | Enter submits; focus starts in Username; failed login clears only the password and keeps focus there; "Register a new account" stays as a link. |
| Components | `BrandLogo`, `PasswordField` with toggle, `FormField` (label + field + helper + error). |
| Files | `LoginView.java`, `Theme.java`; new `FormField.java`, `BrandLogo.java`. |
| Priority | H |
| Dependencies | M0, M2. |
| Acceptance | Logo visible and not stretched at 960 x 640 and at 1400 x 900; wrong password keeps username, clears password, puts focus in password; `LoginViewTest` kept passing plus new tests for the toggle and focus. |

### 3.3 Register (`RegisterView`)

| Field | Detail |
|---|---|
| Current problems | Same card as login but with a different title treatment; password rule is a grey line far from the field (seen). |
| Redesign | Two-column form (name/email left, username/password right) at 960 px; password rule shown as a live checklist (8+ characters, one digit) under the field; field-level error borders. |
| UX | Errors appear next to the field that caused them instead of one line at the bottom. |
| Components | `FormField`, `RuleChecklist`. |
| Files | `RegisterView.java`, `ErrorMessages.java` (map messages to fields), new `RuleChecklist.java`. |
| Priority | M |
| Dependencies | M2. |
| Acceptance | Each validation error from `CustomerController.register` is shown on the right field; checklist updates while typing; `RegisterViewTest` updated and passing. |

### 3.4 Home / dashboards (`Dashboard`, `CustomerDashboard`, `AgentDashboard`, `AdminDashboard`, `TaskCard`)

| Field | Detail |
|---|---|
| Current problems | Four equal boxes and a mostly empty window (seen); the agent "Reservations" card is a dead end (A15). |
| Redesign | Keep the three classes (the design report names them). Home shows: greeting; one primary call to action ("Find a flight" for customers, "Find a customer" for agents, "Open the schedule" for admins); and only real data. Customer: next upcoming reservation (from `ReservationController`) with status badge and "Pay now" if pending; empty state with the mountain illustration when there is none. Agent and admin: the same task cards, smaller, with icons. |
| UX | Home stops being a hub you must return to; it is a landing page. |
| Components | `TaskCard` (icon), `ReservationSummaryCard`, `EmptyState`. |
| Files | `Dashboard.java`, `CustomerDashboard.java`, `AgentDashboard.java`, `AdminDashboard.java`, `TaskCard.java`. |
| Priority | M |
| Dependencies | M3. Needs a decision on where login lands (section 12). |
| Acceptance | Customer with no reservation sees the empty state; with a pending one sees the badge and a working Pay now; no fake numbers anywhere; `DashboardTest` updated. |

### 3.5 Find flights (`FlightSearchView`, `FlightTableModel`)

| Field | Detail |
|---|---|
| Current problems | A row of small fields and a button above a table; typed date; both airport lists start at the first airport, so the first view is "No flights match" (seen). |
| Redesign | Search card: From, swap button, To, Date picker, Airline, Search (primary). Last search remembered for the session. Results as a table with larger times, duration (computed from departure and arrival), airline, seats left, and "From $184" with the cheaper class. Row selected state in glacier tint. Empty state before the first search is a prompt, after a search with no rows "No flights match" with a suggestion (try the next day). Select flight stays; double-click already works. |
| UX | Fewer clicks (remembered search, default airports that actually have a route today), clearer price, error prevention (date picker, no past dates). |
| Components | `DatePickerField`, `SearchCard`, `EmptyState`, `Badge`. |
| Files | `FlightSearchView.java`, `FlightTableModel.java`, `FlightSearchController` only if duration needs a helper (it does not; it is computed in the table model). |
| Priority | H |
| Dependencies | M2 date picker. |
| Acceptance | Today's date preselected; past dates cannot be chosen; swap exchanges airports; duration column correct for a flight that crosses midnight; `FlightSearchViewTest` extended. |

### 3.6 Choose seats (`SeatSelectionView`, `SeatGridModel`)

| Field | Detail |
|---|---|
| Current problems | No aisle, hard to scan; colours only (A18); selection described below the grid (seen). |
| Redesign | Left: cabin drawing. Row numbers on both sides, aisle gap in the middle (after half the seats per row), a subtle cabin outline, business rows separated by a thin gold rule. Right (260 px): "Your seat" summary card: flight, seat number, class, price, Continue (primary). Taken seats show a small x and a muted tint; business seats show "B" mark in the legend. The chosen seat is blue with a white check. |
| UX | Price and class visible while choosing; the agent modify mode shows the old seat as a dashed outline. |
| Components | `SeatMap` (painted or grid of toggle buttons), `SummaryCard`, `Stepper`. |
| Files | `SeatSelectionView.java`, `SeatGridModel.java` (aisle index only; layout rules stay in `SeatLayout`), `SeatSelectionModifyTest`. |
| Priority | H |
| Dependencies | M3 stepper. |
| Acceptance | Aisle appears for 4 and 6 seats-per-row aircraft; every state is distinguishable without colour; keyboard arrows move between seats; existing seat tests pass. |

### 3.7 Payment (`PaymentView`)

| Field | Detail |
|---|---|
| Current problems | Form floats in the middle; no summary; amount in a read-only field (seen). |
| Redesign | Two columns. Left: card form (type, name, number with groups of four, expiry with automatic slash). Right: order summary card (flight, route, date, seat, price, taxes line only if the data has one). Pay button shows the amount ("Pay $184.00"). |
| UX | Card number grouped as typed; field errors inline; the card field is still cleared after each attempt (existing behaviour kept). |
| Components | `FormField`, `SummaryCard`, `CardNumberFormatter` (pure logic, tested). |
| Files | `PaymentView.java`; new `CardNumberFormatter.java`. |
| Priority | H |
| Dependencies | M2. |
| Acceptance | Formatter tests for 16 digits with and without spaces; validation behaviour unchanged (`PaymentViewTest` passes); amount visible in the button. |

### 3.8 Confirmation (`ConfirmationView`)

| Field | Detail |
|---|---|
| Current problems | Monospaced dump, temp path in the text (seen). |
| Redesign | Success header with a check; a boarding-pass style card: route (big YYC to YVR), date and times, flight and airline, seat, passenger, amount paid; confirmation number in a highlighted box with a Copy button; small line "A copy was saved" with an Open folder link (`Desktop.open`, only if supported). Gold used once, for the number box. Buttons: Done (primary), Book another flight. |
| Components | `SummaryCard`, `Badge`, `CopyButton`. |
| Files | `ConfirmationView.java`. |
| Priority | C |
| Dependencies | M2. |
| Acceptance | No raw enum or path text; Copy puts the number on the clipboard; `ConfirmationViewTest` updated. |

### 3.9 My reservations (`MyReservationsView`, `ReservationTableModel`)

| Field | Detail |
|---|---|
| Current problems | Raw status text, Pay now and Cancel disabled with no hint why (seen). |
| Redesign | Table with status badges (Awaiting payment amber, Paid green, Cancelled grey, Completed navy); detail strip under the table for the selected row with its actions; disabled actions show a tooltip ("Only unpaid reservations can be paid"). Cancel uses the danger dialog. |
| Components | `Badge`, `ConfirmDialog`, `DetailStrip`. |
| Files | `MyReservationsView.java`, `ReservationTableModel.java`. |
| Priority | H |
| Dependencies | M2, M7 dialog. |
| Acceptance | Each status has words and colour; actions' enabled state unchanged (`MyReservationsViewTest` passes). |

### 3.10 Modify reservation (agent) (`ModifyReservationView`)

| Field | Detail |
|---|---|
| Current problems | Not inspected visually; reached only through Customers > Modify (13 clicks, A5). Same table and button pattern as reservations (source). |
| Redesign | Same table, header shows the customer name and a short explanation of the two steps (pick a new flight, then a seat). Stepper "Change flight > Change seat > Confirm". |
| Files | `ModifyReservationView.java`, `FlightSearchView.java` (pick mode), `SeatSelectionView.java` (modify mode). |
| Priority | M |
| Dependencies | M3 stepper. |
| Acceptance | Stepper correct in modify mode; clicks to modify from the Customers screen reduced to 7 or fewer (measured in the test plan). |

### 3.11 Customers (agent) (`CustomerManagementView`, `CustomerForm`, `CustomerTableModel`)

| Field | Detail |
|---|---|
| Current problems | Table on top, narrow form centred under it, five buttons in one row (seen). |
| Redesign | Split view: left list with a search field (live filter), right detail panel with the form and the actions Reservations / Modify a reservation as buttons in the panel header. New customer opens the same panel empty. Save is the single primary button. |
| Components | `SplitPage`, `SearchField`, `FormField`. |
| Files | the three above. |
| Priority | H |
| Dependencies | M2. |
| Acceptance | Typing in the search box filters without pressing Search; selecting a row fills the panel; all existing `CustomerManagementViewTest` cases pass. |

### 3.12 Profile (`ProfileView`)

| Field | Detail |
|---|---|
| Current problems | Left heading, centred form, Save far away at the bottom right (seen). |
| Redesign | Single card, max width 560 px, aligned to the page's left edge under the heading; Save inside the card; "Saved" confirmation as an inline success line instead of a pop-up. |
| Files | `ProfileView.java`. |
| Priority | M |
| Acceptance | Save disabled until something changes; success text visible for 3 seconds; `ProfileViewTest` passes. |

### 3.13 Payment records (agent) (`PaymentRecordsView`, `PaymentTableModel`)

| Field | Detail |
|---|---|
| Current problems | Two stacked tables with equal weight (seen). |
| Redesign | Customer list as a narrow left column, payments as the main table on the right; status badges for Paid / Refunded / Declined; amounts right-aligned. |
| Priority | M. Files: `PaymentRecordsView.java`, `PaymentTableModel.java`. |
| Acceptance | Selecting a customer fills the right table; empty text shown when none selected. |

### 3.14 Schedule (`ScheduleView`, `ScheduleTableModel`)

| Field | Detail |
|---|---|
| Current problems | Date range as two text fields; admin reschedule controls under the table (seen once, earlier). |
| Redesign | Date range with two pickers and quick ranges (Today, 7 days, 30 days); sort by clicking headers; reschedule opens a small dialog with two date-time pickers instead of two text fields in the footer. |
| Priority | M. Files: `ScheduleView.java`, `ScheduleTableModel.java`. Dependencies: date picker (M2). |
| Acceptance | Same results as before for the same range; admin-only controls still hidden for agents (`ScheduleViewTest`). |

### 3.15 Flights (admin) (`FlightManagementView`)

| Field | Detail |
|---|---|
| Current problems | About 400 rows with no sorting or filtering; the form beside the table is 380 px wide (seen). |
| Redesign | Filter box (flight number or route) and sortable headers; form in a right-hand card with Add, Update, Remove; Remove is a red outline button that opens the danger dialog and says if the flight has reservations (the controller already blocks it; the message becomes inline). |
| Priority | M. Files: `FlightManagementView.java`. |
| Acceptance | Filter by "WS" shows only WS flights; remove of a flight with bookings still refused with the same business message. |

### 3.16 Routes and Aircraft (admin) (`RouteManagementView`, `AircraftManagementView`)

| Field | Detail |
|---|---|
| Current problems | Add form squeezed into one row above the table (seen for aircraft). |
| Redesign | Same split card layout as Flights for consistency; aircraft form previews the seat count live (rows times seats per row) which is already shown in the table. |
| Priority | M. Files: both views. Acceptance: forms validated inline; tests pass. |

### 3.17 Pop-ups (`Dialogs`, `PromotionDialog`, `PromotionPanel`)

| Field | Detail |
|---|---|
| Current problems | `JOptionPane` for everything; promotion dialog not inspected. |
| Redesign | `Dialogs.error/info` keep their API; add `Dialogs.confirm(...)` and `ConfirmDialog` with a title, one sentence, a secondary Cancel and a primary or danger action. Promotion dialog gets the logo mark, the promotion title in navy and one gold rule. |
| Priority | M. Files: `Dialogs.java`, `PromotionPanel.java`, `PromotionDialog.java`. |
| Acceptance | Cancel/remove dialogs use the danger style; Esc closes every dialog; `PromotionPanelTest` passes. |

## 4. Design system specification

Tokens live in code (section 7) and are mirrored here. Values marked with a contrast figure were computed from the real hex values.

### 4.1 Colour tokens

| Token | Value | Use | Contrast note |
|---|---|---|---|
| `navy-900` | `#093151` | top bar, headings, table header text | 13.4:1 on white |
| `navy-950` | `#082848` | pressed states on navy | |
| `blue-600` | `#0779C8` | brand fills, selected-tab line, focus ring | 4.6:1 on white, 4.3:1 on page grey |
| `blue-700` | `#066AB0` | text links, filled primary buttons | 5.7:1 on white, 5.3:1 on page grey |
| `sky-500` | `#2DB1E9` | 3 px accents only, never text | 2.45:1 on white |
| `sky-100` | `#E6F3FC` | selected row, hover tint | navy text on it 11.8:1 |
| `gold-500` | `#FDB52E` | one highlight per screen, business tint, never text | 1.8:1 on white; navy on gold 7.5:1 |
| `gold-100` | `#FCE7A8` | business seat | navy on it 10.9:1 |
| `page` | `#F5F8FB` | window background | |
| `surface` | `#FFFFFF` | cards, tables, inputs | |
| `border` | `#D9E2EC` | 1 px outlines, dividers | |
| `text` | `#102A43` | body | 14.6:1 on white |
| `text-muted` | `#52606D` | helper, captions | 6.5:1 on white, 6.1:1 on page |
| `disabled` | `#8795A5` | disabled text only (non-essential) | 3.1:1, acceptable for disabled controls |
| `success` | `#1A7F37` text on `#E3F5E8` tint | badges, success line | text on tint is 4.5:1 or lower and must be darkened by the contrast test |
| `warning` | `#8A5A00` on `#FFF4D6` | "Awaiting payment" | 5.4:1 |
| `danger` | `#B3261E` on `#FDECEA` | errors, Remove | 5.7:1 |

A unit test will compute the contrast of every foreground/background pair in this table and fail the build below 4.5:1 (3:1 for large text and focus ring).

### 4.2 Typography

Recommended family: **Inter** through the official `flatlaf-fonts-inter` artifact (same look on macOS, Windows and Linux, tabular numbers for prices and times). The logo's wordmark is an image, so the UI font does not have to match it. Manrope is the alternative but needs the TTF files bundled by hand.

| Style | Size / weight | Use |
|---|---|---|
| Display | 28 / 700 | login title only |
| Page heading | 22 / 700 | one per screen |
| Section heading | 16 / 600 | card titles, form sections |
| Body | 14 / 400 | default |
| Label | 13 / 500 | form labels |
| Helper / caption | 12 / 400, muted | hints, errors under fields |
| Table header | 12.5 / 600, muted navy | not capitals |
| Table cell | 14 / 400, numbers tabular | |
| Button | 14 / 600 | |
| Nav tab | 14 / 500 (600 selected) | |

Swing has no line-height setting, so rhythm comes from insets and row heights, below.

### 4.3 Spacing, size and shape

| Token | Value |
|---|---|
| Spacing scale | 4, 8, 12, 16, 24, 32, 48 |
| Page padding | 32 left/right, 24 top |
| Content max width | 1200 (tables full width inside it; forms 560, summary cards 320) |
| App bar | 56 high |
| Control height | 36 (input, button); 40 for the main form action |
| Table | row 40, header 36, compact mode 32 |
| Radius | 8 controls, 12 cards, 999 badges |
| Border | 1 px `border`; elevation: none, except dialogs (one soft shadow) |
| Focus ring | 2 px `blue-600`, offset 1 |

### 4.4 Components

| Component | Spec |
|---|---|
| Primary button | `blue-700` fill, white text, darker on hover and again on press. Disabled: `#EEF2F6` fill with `disabled` text (white on a pale fill would fail contrast) |
| Secondary | white fill, `border`, navy text; hover `sky-100` |
| Tertiary / link | no border, `blue-700` text |
| Destructive | white fill, `danger` text and border; the confirm dialog's action is filled `danger` |
| Text field | 36 high, radius 8, `border`, focus ring `blue-600`, error ring `danger` with message under it |
| Select | same as text field; arrow in `text-muted` |
| Date picker | text field with calendar button; popup month grid; today ring, selected `blue-700`; disabled days muted; pure grid model is unit-tested |
| Search field | text field with magnifier icon and clear button |
| Table | white surface, header tint `#EEF3F8`, row hover `#F5F9FD`, selected `sky-100` with a 3 px `blue-600` left edge, numbers right-aligned |
| Card | white, 1 px border, radius 12, padding 24 |
| Badge | 22 high pill, tinted fill, 600 12 px text, small dot; words always present |
| Alert | tinted strip with icon, one sentence, optional action |
| Empty state | small illustration (mountain line from the logo) or icon, one sentence, one action |
| Stepper | 4 steps, done = check, current = blue, next = muted; step names, not numbers only |
| Dialog | 440 wide, title 16/600, body 14, buttons right-aligned, Esc cancels, Enter confirms safe action only |
| Loading | splash for start-up; indeterminate bar for payment; no skeleton screens (nothing loads from a network) |

### 4.5 Accessibility rules (WCAG 2.2 AA where it applies to a desktop app)

Contrast tested in code; visible focus on every control; every control reachable by Tab in reading order; every icon-only button has a tooltip and accessible name; status never by colour alone (words, dots, text in the seat); minimum target 32 x 32 px; text scales with `-Dflatlaf.uiScale`; default button per form; Esc closes dialogs; no timing-dependent behaviour.

### 4.6 How the logo shapes the system

Navy structure from the mountain and wordmark; blue action from "Air"; the cyan line from the swoosh; gold used as rarely as the sun is in the mark; flight-path curve reused once on the login panel and once in the empty state; mountain ridge line reused in empty states and the login panel. No plane or mountain on ordinary screens.

## 5. Navigation redesign

### 5.1 Decision: top tabs, not a sidebar

Each role has three or four destinations. A left sidebar would take about 220 px of a 1000 px window to show four words, and the most used screens are wide tables and forms. Top tabs cost 40 px of height, keep the width, and fit the 960 px minimum. A sidebar would make sense only with ten or more destinations.

### 5.2 Before

```
Login -> Dashboard (4 cards)
         |-- Customer: Search -> Seats -> Payment -> Confirmation -> Dashboard
         |             My reservations -> Dashboard      My profile -> Dashboard
         |-- Agent:    Customers -> [Reservations | Modify -> Search -> Seats] -> Dashboard
         |             Payment records -> Dashboard   Schedule -> Dashboard
         `-- Admin:    Flights / Routes / Aircraft / Schedule, each back to Dashboard
Every move between two screens passes through the dashboard.
```

### 5.3 After

```
Login -> (role landing)
Customer tabs:  Find flights | My reservations | Profile      Booking steps: Search > Seats > Payment > Done
Agent tabs:     Customers | Payment records | Schedule        Modify steps: Reservation > Flight > Seat > Confirm
Admin tabs:     Flights | Routes | Aircraft | Schedule
Logo = Home (dashboard stays, as a landing page).  User chip + Log out always on the right.
Back buttons only inside multi-step flows (Seats back to Search, Payment back to Seats).
```

Booking clicks from login to payment: Find flights tab is already selected, choose airports (remembered or defaults), Search, double-click flight, choose seat, Continue = 4 to 5 clicks, against 8 today.

## 6. Functional improvement plan

| ID | Existing behaviour | Problem | Proposed | Benefit | Complexity | Backend / DB need | Tests |
|---|---|---|---|---|---|---|---|
| F1 | Status shows the enum name | Looks broken | Badge with words (Awaiting payment, Paid, Cancelled, Completed) | Clear status | Low | None | Table-model test for the mapping |
| F2 | Typed dates | Errors, slow | `DatePickerField` | Fewer errors | Medium | None | Grid-model unit tests |
| F3 | Search starts empty on a pair with no flights | First view is "No flights" | Remember last search; default to a pair with flights today | Fewer clicks | Low | None (read from `FlightSearchController`) | View test |
| F4 | No duration | User must subtract | Duration column, computed | Faster comparison | Low | None | Table-model test incl. midnight crossing |
| F5 | Seat map without aisle | Hard to read | Aisle, row numbers both sides, summary card | Fewer wrong seats | Medium | None | `SeatGridModel` test for aisle index |
| F6 | Plain payment form | No summary | Order summary, card grouping, inline errors | Trust | Medium | None | Formatter tests |
| F7 | Text-dump confirmation | Worst screen | Booking card, Copy, Open folder | Delight at the key moment | Medium | None | View test |
| F8 | No sort or filter | 370 rows | Sortable headers and filter box on Flights, Customers, Schedule | Faster admin and agent work | Low | None | Filter model test |
| F9 | Reservations card dead end for agents | Confusing | Remove card or route via Customers | No dead end | Low | Product decision | `DashboardTest` |
| F10 | `JOptionPane` for confirmations | No danger signal | `ConfirmDialog` | Prevents mistakes | Medium | None | Dialog test with injected confirmer (existing pattern) |
| F11 | No keyboard defaults | Mouse only | Default button, focus first field, Esc closes dialogs | Speed, accessibility | Low | None | Focus tests |
| F12 | Silent 7 s start | Looks frozen | Splash with logo and "Preparing flights" | Trust | Medium | Move seeding progress callback only | Start-up test with fake clock |
| F13 | Work on the UI thread | Possible freeze | Background worker for start-up and payment only | Responsiveness | Medium | None | Tests keep calling controllers directly |
| F14 | Pagination | Brief lists it | **Not proposed**: at most a few hundred rows; filter and sort serve better | Avoids complexity | n/a | n/a | n/a |
| F15 | Dark mode | Brief hints at "modern" | Not in this plan; tokens make it possible later | | | | |

Nothing here needs a database change or a new business rule.

## 7. Technical architecture

### 7.1 Theme architecture

1. **Design tokens as Java** in a small package-private class set: `Palette` (colours), `Type` (fonts), `Space` (numbers). The contrast test reads `Palette`.
2. **FlatLaf theme file** `src/main/resources/ca/ucalgary/chinookair/presentation/ChinookAir.properties` registered with `FlatLaf.registerCustomDefaultsSource(...)`. It holds `@accentColor`, `Component.arc`, table and button defaults and named style classes (`[style]Button.primary`, `.danger`, `.link`, `Label.h1`, `Label.muted`). This replaces the inline `"FlatLaf.style"` strings now in `Theme.java` and `TaskCard.java`, which is hard to maintain.
3. **Helper classes** `Theme` (install, small factories) and `Components` (builders returning styled standard Swing components, so views stay thin).
4. **Reusable components** (new, package-private, each with a test where it has logic):

| Class | Purpose |
|---|---|
| `BrandMark`, `BrandLogo` | scaled, HiDPI-sharp logo painters |
| `NavTabs`, `Stepper`, `UserChip` | shell parts |
| `FormField` | label, field, helper, error in one unit |
| `DatePickerField` + `MonthGridModel` | date input |
| `Badge`, `EmptyState`, `SummaryCard`, `ConfirmDialog` | content pieces |
| `CardNumberFormatter`, `SeatMap` | logic and painting |

### 7.2 Assets

- Move `ChinookAir Mountain Flight Logo.png` (now untracked in the repo root) to `src/main/resources/branding/`.
- Produce from it: logo with text (cropped to its content box, 117,147 to 1418,893), mark only (mountain, plane, trails, no wordmark), wordmark only, and icon sizes 16, 32, 64, 128, 256, 512 for `setIconImages` and `Taskbar.setIconImage`.
- The PNG has a solid white background. For white surfaces that is fine. For the navy bar the mark goes on a white rounded tile, so no transparency is required. If you have the original vector or a transparent file, that is better; otherwise I can generate a clean alpha version by flood-filling from the corners (keeps interior whites such as snow and plane windows).
- Icons: about 12 simple glyphs (plane, ticket, user, calendar, search, swap, check, alert, close, arrow, copy, folder). Options: FlatLaf SVG icons (`flatlaf-extras` plus JSVG, about 0.3 MB, themeable and HiDPI) or Java2D drawn icons (no dependency, more code). Recommendation: SVG, with Lucide icons (ISC licence). Needs your OK for the extra dependency.

### 7.3 Dependencies

| Library | Why | Size | Decision |
|---|---|---|---|
| `flatlaf` 3.5.4 | already added | about 0.8 MB | kept |
| `flatlaf-fonts-inter` | consistent font | about 1 MB | proposed |
| `flatlaf-extras` + `jsvg` | SVG icons | about 0.3 MB | proposed, optional |
| date picker library | not needed, built in-house | | rejected (extra weight, own look) |

### 7.4 Architecture rules kept

`presentation` still depends only on controllers and entities; `ArchitectureTest` stays green; no UI class touches `data`. New classes are package-private in `presentation` except `Theme` and `ViewId` (already public).

### 7.5 Risks and trade-offs

| Risk | Mitigation |
|---|---|
| Existing tests reach into views by field name | Keep field names and accessors; rename only where a test is updated in the same commit |
| Fonts differ per OS | Bundle Inter; verify on Windows and Linux if you can |
| Painting differs on HiDPI | Use FlatLaf scaling APIs; verify on a 2x display (you have one) |
| Headless CI cannot create a `JFrame` | Component tests only; screenshots generated by a local script, not in CI |
| Scope grows | Milestones are independent; stop after M4 and the app is already branded end to end |

## 8. Implementation roadmap

Each milestone ends with a working app, `mvn clean verify` green (all tests, 0 Checkstyle violations), small commits in your name, a pull request into `dev`, and your approval before any push.

### M0 Brand assets (about 0.5 day)
- Objective: the logo exists in the project in the sizes the UI needs.
- Tasks: move the PNG; make crop, mark, wordmark and icon sizes with a script kept in `scripts/`; load them through one `Brand` class; set the window icon and Dock/taskbar icon.
- Files: `src/main/resources/branding/*`, new `Brand.java`, `MainFrame.java`, `scripts/make-brand-assets.*`.
- Dependencies: none. Risk: PNG background; mitigated by the white tile.
- Verify: window shows the mark in the title bar and Dock; image loads in a jar run.
- Accept: icons sharp at 1x and 2x; jar size increase under 1 MB.

### M1 Tokens and typography (about 1 day)
- Tasks: `Palette`, `Type`, `Space`; `ChinookAir.properties`; add Inter; move all colours out of views (`SeatSelectionView`, `LoginView`, `Theme`, `TaskCard`); contrast test.
- Verify: screenshots of all 21 states; contrast test fails if a token pair drops below 4.5:1 (checked by breaking one).
- Accept: no hex literal left in any view; the contrast test passes; all existing tests pass.

### M2 Component library (about 2 days)
- Tasks: `FormField`, `Badge`, `EmptyState`, `SummaryCard`, `DatePickerField`, `MonthGridModel`, `SearchField`, button style classes, table styling (header tint, hover, selected edge), `HintTable` updated.
- Verify: unit tests for `MonthGridModel`, badge mapping, formatter; a throw-away showcase frame rendered to PNG.
- Accept: every component has a test where it has logic; screens not yet converted still work.

### M3 Shell and navigation (about 1.5 days)
- Tasks: `AppBar` v2 with logo, `NavTabs`, `UserChip`; `Stepper`; role landing; Home cards with icons; remove the agent dead end per your decision.
- Files: `AppBar.java`, `MainFrame.java`, `ViewId.java`, dashboards.
- Risk: `MainFrameSmokeTest` and `DashboardTest` change; updated in the same commits.
- Accept: section 3.1 and 3.4 criteria; tab selection correct for all 18 views; stepper correct in booking and modify flows.

### M4 Login and customer journey (about 3 days)
- Tasks: Login (3.2), Register (3.3), Find flights (3.5), Seat map v2 (3.6), Payment (3.7), Confirmation (3.8), My reservations (3.9), Profile (3.12).
- Accept: the customer booking path is 5 or fewer clicks from login to the seat screen's Continue; confirmation shows no raw enum or path; all `*ViewTest` pass; manual run of test cases T01 to T20 of `docs/test-plan.md` by you.

### M5 Agent screens (about 1.5 days)
- Tasks: Customers split view (3.11), Modify reservation (3.10), Payment records (3.13), Schedule pickers (3.14).
- Accept: modify from Customers takes 7 or fewer clicks; live filter works; agent cannot see reschedule controls.

### M6 Admin screens (about 1.5 days)
- Tasks: Flights (3.15), Routes and Aircraft (3.16), sorting and filter box, danger styling for Remove.
- Accept: filter and sort work on 370 rows without lag; remove-with-bookings message unchanged.

### M7 Functional polish (about 2 days)
- Tasks: `ConfirmDialog`, promotion dialog, splash and start-up progress, background worker for start-up and payment, default buttons, focus management, tooltips and accessible names, Esc handling.
- Risk: threading changes; payment test keeps calling the controller directly, only the view wraps it.
- Accept: no window freeze during start-up; every screen reachable and operable by keyboard; every icon button has a tooltip.

### M8 Final audit (about 1 day)
- Tasks: render every state at 960 x 640, 1000 x 700 and 1400 x 900; check each screen against this plan; run all test-plan cases; fix regressions; update `docs/test-plan.md`, the report's screen descriptions and the README screenshots.
- Accept: section 11.

Total estimate: about 14 working days of focused work if every milestone is done; M0 to M4 alone (about 8 days) already gives a fully branded customer experience.

## 9. Priority and impact matrix

| Change | User impact | Visual impact | Effort | Risk | Order |
|---|---|---|---|---|---|
| Logo, icons, brand colours (M0, M1) | Med | Very high | Low | Low | 1 |
| Status badges, confirmation card (F1, F7) | High | High | Low-Med | Low | 2 |
| Top nav, stepper (M3) | High | High | Med | Med | 3 |
| Seat map v2, payment summary (F5, F6) | High | High | Med | Med | 4 |
| Date picker, remembered search (F2, F3) | High | Med | Med | Low | 4 |
| Table sort and filter (F8) | Med | Low | Low | Low | 5 |
| Confirm dialog, keyboard, a11y (F10, F11) | Med | Low | Med | Low | 6 |
| Splash and background work (F12, F13) | Med | Low | Med | Med | 7 |
| Dark mode | Low | Med | Med | Low | not planned |

Quick wins (each under half a day): logo and icons, status badges, kept-last search, table hover and selected edge, default buttons, tooltips on disabled actions.

## 10. Before and after

| Screen | Today (seen) | After |
|---|---|---|
| Login | Blue text "ChinookAir" on a white card, labels beside fields | Logo on white, labels above fields, Show password, optional navy panel with a ridge line; Enter logs in; failed login keeps focus in password |
| Customer home | Four equal boxes, empty lower half | Greeting, one "Find a flight" button, next reservation card with badge or an empty state with the mountain line |
| Find flights | Small row of fields; first view is "No flights match this search." | Search card with swap and date picker, a pair that has flights preselected, duration and "From $" in results |
| Seats | Grid with no aisle; "Pick a seat" in bold | Cabin with aisle and numbers, summary card with class and price, check mark on the chosen seat |
| Payment | Floating form, read-only amount box | Form left, order summary right, "Pay $184.00" |
| Confirmation | Monospaced dump with a temp path | Boarding-pass style card, gold-highlighted number with Copy |
| My reservations | `PENDING_PAYMENT` | Amber "Awaiting payment" badge, hint on disabled actions |
| Admin flights | 370-row table, form squeezed right | Sortable and filterable table, card form, red Remove with warning dialog |

## 11. Validation and definition of done

1. Every screen in section 3 reviewed in a rendered image at 960 x 640, 1000 x 700 and 1400 x 900, and by you on a real display.
2. No hex colour literal outside `Palette` (a test greps the sources).
3. Contrast test passes for every token pair.
4. All 18 `ViewId` screens reachable; tab or stepper state correct for each (test).
5. Keyboard: Tab order logical, Enter submits, Esc closes dialogs, focus visible (manual checklist added to `docs/test-plan.md`).
6. `mvn clean verify`: all tests green (769 now, more after), 0 Checkstyle violations, `ArchitectureTest` green.
7. Existing business tests unchanged in meaning; changed tests listed in the pull request.
8. New tests for every component with logic (badge mapping, month grid, card formatter, aisle index, filter model, stepper).
9. Jar runs from `target/ChinookAir.jar` on macOS and, if you can test, Windows.
10. No fake data on any screen; every control does something.
11. Mutation check done for each new test (break the code, see it fail).
12. The report (`docs/report`) and `docs/test-plan.md` mention the final screens.

A milestone is done only when its acceptance criteria are met and you have seen it run, not when it compiles.

## 12. Decisions I need from you

| # | Decision | My recommendation |
|---|---|---|
| 1 | After login, land on Home (dashboard) or go straight to the role's first task? | Customer: Find flights. Agent: Customers. Admin: Flights. Home reachable through the logo. |
| 2 | UI font: bundle Inter (about 1 MB) or keep the system font? | Inter |
| 3 | Add `flatlaf-extras` and JSVG for SVG icons (about 0.3 MB), or draw icons in Java2D? | SVG |
| 4 | Do you have a transparent or vector version of the logo? | If not, I generate an alpha PNG and the white tile covers edge cases |
| 5 | Agent "Reservations" dashboard card: remove it? | Remove; reservations are reached through Customers |
| 6 | Scope cut line: stop after M4 (customer journey), or do M0 to M8? | Decide by your deadline; M0 to M4 first, then reassess |
| 7 | Create GitHub issues for M0 to M8 (they are outward-facing, so only with your yes)? | Yes, one issue per milestone, labelled `ui`, after the plan is approved |
| 8 | Where does this sit relative to P9-04 (jar and CI) and P9-05 (submission)? | Finish M0 to M4 before P9-04 so the jar and the archive contain the final UI |

## 13. Review of this plan against its own questions

Changes I made after challenging the first draft:
- I dropped a left sidebar: it costs 220 px for four words and the screens are wide tables.
- I dropped pagination and charts: the data is a few hundred rows and there is nothing real to chart.
- I changed the interactive blue from the logo's `#0779C8` to `#066AB0` for text and buttons, because the logo blue on the page grey measures 4.3:1, under the 4.5:1 line. The logo blue remains for brand fills and the focus ring.
- I replaced Manrope with Inter as the default: a ready artifact for FlatLaf, no hand-bundled TTFs.
- I excluded animation beyond FlatLaf's hover and focus states: Swing has no transition system, and a hand-rolled one would add risk without helping a booking task.
- I flagged the logo file problems (opaque white, 1.2 MB, wordmark and mark in one image) instead of assuming it can be dropped in.
- I flagged that the success badge colours need a darker green than the first guess, which the contrast test will force.

Still open: the real feel of hover, pressed and focus states and window resizing can only be judged by running the app. That is why every milestone ends with you looking at it.
