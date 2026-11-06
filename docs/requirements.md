# PG maintenance

One owner tracks a single PG: rooms, who is staying, and rent for each month. There is no login in this version.

Occupancy is the beds held in a room, not the number of people. A stay holds one or more beds. Rent is per person, per month.

## Rooms

- The owner can add and edit a room.
- A room has a name or number, sharing capacity, default monthly rent per person, and optional details (floor, AC, attached bathroom, notes).
- Sharing capacity is how many people the room can hold, such as 1, 2, 3, or 4.
- A room profile shows those details, the occupied count, the free spots, and who is living there now.
- When nobody is living there, the room stays and is shown as empty.
- Empty means nobody is living there. Semi-empty means at least one person is living there and the room is still under its sharing capacity.
- The owner can list rooms by occupancy and by sharing. Occupancy is all, empty, or semi-empty. Sharing is all, or a capacity such as 3. The two filters combine.
- Each room in that list shows its capacity, default monthly rent, occupied count, and free spots. A full room stays visible when the owner looks at one sharing, such as every 3-sharing room.

## Residents

- The owner can add, edit, and move a person in or out.
- A person has a name, phone number, optional email, and optional notes (emergency contact, ID reference).
- A person has one ID file. The owner uploads a JPEG, PNG, WebP, or PDF of up to 5 MB, and a later upload replaces it.
- A person profile shows their details, current room, rent history, and, when an ID file is stored, its name, type, and size. The file itself is downloaded separately.
- The ID file is stored in the database. Moving it to object storage comes later.
- Moving out sets a leave date. The person and their past rent stay in the system.

## Occupancy

- A person lives in at most one room at a time.
- The owner assigns a person to a room on move-in, and can shift them to another room later.
- A stay holds one or more beds. Omit the number and it holds one. The occupied count is the sum of beds held, and free spots are the beds still open.
- A room cannot take a stay that needs more beds than are free.
- Past stays stay recorded: room, beds held, move-in date, move-out date, and the rent agreed for that stay.
- A stay uses the room's per-person rent times the beds held, unless the owner sets a different amount.
- The owner can record a planned leave date on the current stay when notice is given. Move-out still sets the actual leave date and closes the stay.
- A planned leave opens one spot in that room. In a shared room, the room can remain occupied after that one person leaves.
- The owner can list stays whose planned leave date falls within the next month, with the person, room, sharing, and rent.

## Rent

- Rent is tracked per person, per month.
- The amount due is the agreed rent for that person's stay in that month.
- For each month, a person is pending, partial, or paid.
- The owner records a payment against a month: amount, date, and an optional note (cash, UPI).
- A payment has one screenshot. The owner uploads a JPEG, PNG, WebP, or PDF of up to 5 MB, and a later upload replaces it. The file is stored in the database.
- The owner can list, for a chosen month, who has paid, who is partial, and whose rent is pending.
- A person who moved out still has rent lines for the months they stayed. No new rent line is created after the leave date.

## Lists the owner needs

- All rooms, with capacity, default monthly rent, occupied count, and free spots.
- The same room list filtered by occupancy (all, empty, semi-empty) and sharing (all, or a capacity such as 3).
- Stays with a planned leave date in the next month.
- One room, with the people living there now.
- All current residents, and separately the people who have left.
- People whose name contains a search, each with the full profile. Choosing one then loads that person by id.
- One person, with profile, current room, month-by-month rent, and the stored ID file.
- Rent for a month: paid, partial, and pending.

## Later

Leave these for a later version: deleting a room, named beds, security deposit, electricity and other charges, complaints, other document uploads, moving ID files and payment screenshots to object storage, reminders, more than one PG, and login.
