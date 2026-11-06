-- Initial schema. Tables are added in this file until the first deploy.

CREATE TABLE rooms (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    sharing_capacity INT NOT NULL,
    default_monthly_rent DECIMAL(10, 2) NOT NULL,
    floor INT NULL,
    ac BOOLEAN NULL,
    attached_bathroom BOOLEAN NULL,
    notes VARCHAR(500) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_rooms_name UNIQUE (name),
    CONSTRAINT ck_rooms_sharing_capacity CHECK (sharing_capacity >= 1),
    CONSTRAINT ck_rooms_default_monthly_rent CHECK (default_monthly_rent >= 0)
);

CREATE TABLE people (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    email VARCHAR(200) NULL,
    notes VARCHAR(500) NULL,
    PRIMARY KEY (id)
);

CREATE TABLE identity_documents (
    id BIGINT NOT NULL AUTO_INCREMENT,
    person_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes INT NOT NULL,
    content LONGBLOB NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_identity_documents_person UNIQUE (person_id),
    CONSTRAINT fk_identity_documents_person FOREIGN KEY (person_id) REFERENCES people (id),
    CONSTRAINT ck_identity_documents_size CHECK (size_bytes > 0)
);

CREATE TABLE stays (
    id BIGINT NOT NULL AUTO_INCREMENT,
    person_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    move_in_date DATE NOT NULL,
    move_out_date DATE NULL,
    agreed_rent DECIMAL(10, 2) NOT NULL,
    spots INT NOT NULL,
    planned_leave_date DATE NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_stays_person FOREIGN KEY (person_id) REFERENCES people (id),
    CONSTRAINT fk_stays_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT ck_stays_agreed_rent CHECK (agreed_rent >= 0),
    CONSTRAINT ck_stays_spots CHECK (spots >= 1),
    CONSTRAINT ck_stays_move_out CHECK (move_out_date IS NULL OR move_out_date >= move_in_date),
    CONSTRAINT ck_stays_planned_leave CHECK (planned_leave_date IS NULL OR planned_leave_date >= move_in_date)
);

CREATE TABLE rent_lines (
    id BIGINT NOT NULL AUTO_INCREMENT,
    person_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    rent_month DATE NOT NULL,
    amount_due DECIMAL(10, 2) NOT NULL,
    amount_paid DECIMAL(10, 2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_rent_lines_person_month UNIQUE (person_id, rent_month),
    CONSTRAINT fk_rent_lines_person FOREIGN KEY (person_id) REFERENCES people (id),
    CONSTRAINT fk_rent_lines_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT ck_rent_lines_amount_due CHECK (amount_due >= 0),
    CONSTRAINT ck_rent_lines_amount_paid CHECK (amount_paid >= 0),
    CONSTRAINT ck_rent_lines_paid_within_due CHECK (amount_paid <= amount_due)
);

CREATE TABLE payments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    rent_line_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    paid_on DATE NOT NULL,
    note VARCHAR(200) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_payments_rent_line FOREIGN KEY (rent_line_id) REFERENCES rent_lines (id),
    CONSTRAINT ck_payments_amount CHECK (amount > 0)
);

CREATE TABLE payment_screenshots (
    id BIGINT NOT NULL AUTO_INCREMENT,
    payment_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes INT NOT NULL,
    content LONGBLOB NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_payment_screenshots_payment UNIQUE (payment_id),
    CONSTRAINT fk_payment_screenshots_payment FOREIGN KEY (payment_id) REFERENCES payments (id),
    CONSTRAINT ck_payment_screenshots_size CHECK (size_bytes > 0)
);
