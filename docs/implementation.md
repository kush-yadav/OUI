# Implementation order

Follow `docs/requirements.md`. Leave the Later list untouched. Keep one Flyway `V1` file and edit it in place as each step adds tables.

## 1. Project skeleton

- Add Spring Data JPA, Flyway, Bean Validation, and springdoc.
- Add Docker Compose with MySQL as the only service.
- Point the app at that database. Hibernate must not create or update tables. Flyway owns the schema.
- Create the packages `controller`, `dto`, `entity`, `repository`, `service`, and `exception` under `com.example.pg`.
- Add one `@RestControllerAdvice` that returns RFC 9457 Problem Details.
- Add an empty `V1` migration.
- Confirm the app starts, Flyway runs, and the OpenAPI UI loads.
- Do not add room, person, or rent types in this step.

## 2. Rooms

A room can be added and edited. It stays when it is empty.

Fields:

- Name or number, required and unique.
- Sharing capacity, required, at least 1.
- Default monthly rent per person, required, zero or more.
- Optional floor, AC, attached bathroom, and notes.

Endpoints:

- `POST /rooms` creates a room and returns the DTO with `Location`.
- `PUT /rooms/{id}` edits it and returns the DTO.
- `GET /rooms/{id}` returns the room profile: details, occupied count, free spots, and current residents.
- `GET /rooms` lists every room with capacity, default monthly rent, occupied count, and free spots.
- `occupancy` filters that list: `empty` or `semi-empty`. Omit it to return all rooms.
- `sharing` filters that list by capacity, such as `3`. Omit it to return every sharing.
- The two filters combine. `GET /rooms?sharing=3` is every 3-sharing room, including full ones. `GET /rooms?sharing=3&occupancy=empty` is the empty 3-sharing rooms. `GET /rooms?sharing=3&occupancy=semi-empty` is the 3-sharing rooms with a free spot.

Until people exist, occupied count is 0, free spots equal capacity, and current residents is an empty list. Every room is empty, and the semi-empty list is empty.

Add `NotFoundException` for a missing room and `ConflictException` for a duplicate name. Document each endpoint with springdoc, including the problem-details responses. Add the `rooms` table to `V1`.

## 3. People

A person can be added, edited, moved in, shifted, given notice, and moved out.

Fields:

- Name and phone number, required.
- Email and notes, optional. Notes cover an emergency contact or an ID reference.

Endpoints:

- `POST /people` creates the person and returns the DTO with `Location`. The person has no room yet.
- `PUT /people/{id}` edits the profile and returns the DTO.
- `GET /people/{id}` returns the profile, the current room, past stays, and rent history.
- `GET /people/name/{name}` returns every person whose name contains that text, ignoring case. Each item is the full profile for now. A blank name is rejected. No match returns an empty list.
- `GET /people?stay=current` lists people who have not left.
- `GET /people?stay=left` lists people who have moved out.
- `POST /people/{id}/move-in` assigns a room, a move-in date, optional spots, and an optional agreed rent. Omit spots and the stay holds 1 bed. Omit agreed rent and the price is the room's per-person rent times spots.
- `POST /people/{id}/shift` moves the person to another room and closes the previous stay. Spots apply to the new stay.
- `POST /people/{id}/notice` records a planned leave date on the current stay.
- `GET /people/leaving` lists stays whose planned leave date falls within the next month, with the person, room, sharing, and rent.
- `POST /people/{id}/move-out` sets the actual leave date and closes the current stay.

Rules:

- A person lives in at most one room at a time.
- A stay is rejected when spots is greater than the free beds. Occupied count is the sum of spots on open stays.
- Each stay records the room, spots, move-in date, move-out date, agreed rent, and planned leave date.
- A planned leave opens one spot. In a shared room, the room can remain occupied after that person leaves.
- Moving out keeps the person and their stays.
- The room profile from step 2 then shows who is living there now, the occupied count, and the free spots.
- Rent history on the person profile is an empty list until rent exists.

Add `BadRequestException` when the room is full, the person already has a current room, or notice is recorded with no current stay. Document each endpoint with springdoc. Add the `people` and `stays` tables to `V1`.

## 4. Rent

Track rent per person per month, record a payment, list who is pending, partial, or paid for a chosen month, and show revenue for every month.

A rent line is one person and one calendar month. Reading a month creates a line for each stay that covers that month. The amount due is the agreed rent for that stay. A person who has moved out keeps the lines for the months they stayed. No line is created for a month after the leave date.

Fields on a rent line:

- Person, room, and month (`YYYY-MM`).
- Amount due, amount paid, and balance.
- Status: `pending` when nothing is paid, `partial` when some of the due amount is paid, `paid` when the paid amount equals the amount due.

A payment has an amount, a date, an optional note (cash, UPI), and one optional screenshot. The screenshot is a JPEG, PNG, WebP, or PDF of at most 5 MB. Uploading again replaces it. The bytes stay in MySQL. The payment shows the file name, content type, and size, and does not include the bytes.

Endpoints:

- `GET /rent?month=2026-11` lists that month. Each row has the person, room, sharing, amount due, amount paid, balance, and status.
- `status` filters that list: `pending`, `partial`, or `paid`. Omit it to return all three.
- `POST /people/{id}/payments` records a payment for one month and returns the payment DTO with `Location`. The screenshot is absent until one is uploaded.
- `POST /people/{id}/payments/{paymentId}/screenshot` stores the screenshot from a multipart part named `file`. The first upload returns the metadata with `Location`. A replacement returns the same metadata.
- `GET /people/{id}/payments/{paymentId}/screenshot` downloads the screenshot. A payment with no screenshot returns 404.
- `GET /revenue` lists every month that has a rent line, oldest first.
- `GET /revenue?month=2026-11` returns that one month. A month the owner asks for before anyone has paid still returns its totals.

Revenue for a month:

- Month.
- Total due, collected, and outstanding.
- Counts of pending, partial, and paid lines.

Rules:

- A stay covers a month when the move-in date is on or before the last day of the month and the person has not left before the first day of that month.
- A shift inside one month keeps a single line. The amount due is the agreed rent of the stay that is open at the end of that month. When the person has already left, it is the agreed rent of the stay that closed in that month.
- Several payments can add up to the amount due. A payment that would make the paid amount greater than the amount due is rejected.
- An amount due of zero is `paid`, with nothing to collect.
- The person profile from step 3 then lists this rent history: month, amount due, amount paid, and status.

Add `BadRequestException` when the person did not stay that month, the amount is not greater than zero, the payment exceeds the balance, or the screenshot is empty, larger than 5 MB, or not one of those types. Add `NotFoundException` when the person, payment, or screenshot is missing. Document each endpoint with springdoc. Add the `rent_lines`, `payments`, and `payment_screenshots` tables to `V1`.

## Identity document

A person has one ID file. Uploading again replaces it. The bytes stay in MySQL until object storage is added. There is no storage abstraction in this step.

The file is a JPEG, PNG, WebP, or PDF, and at most 5 MB. The profile shows the file name, content type, and size. It does not include the bytes.

Endpoints:

- `POST /people/{id}/identity` stores the file from a multipart part named `file`. The first upload returns the metadata with `Location`. A replacement returns the same metadata.
- `GET /people/{id}/identity` downloads the file. A person with no file returns 404.

Add `BadRequestException` when the file is empty, larger than 5 MB, or not one of those types. Add `NotFoundException` when the person or the file is missing. Document each endpoint with springdoc. Add the `identity_documents` table to `V1`.

## PG-X

The local database holds one PG, PG-X. People and rent are added on top of these rooms.

Three blocks, A, B, and C. Each block has floors 1–6. Each floor has rooms 01–12. The room name is the block, floor, and number, such as `A101`, `B212`, and `C612`. The floor number is stored on the room.

Every floor of every block uses the same layout. Rent is per person.

- `01` and `12`: sharing 1, rent 22000
- `06` and `07`: sharing 3, rent 9000
- `02`, `03`, `04`, `05`, `08`, `09`, `10`, and `11`: sharing 2, rent 14000

That is 216 rooms and 432 beds.

## 5. Tests

Add unit tests and integration tests with Testcontainers MySQL.
