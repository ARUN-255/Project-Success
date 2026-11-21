CREATE TABLE college(
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE,
    email_domain VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE admin (
    id BIGSERIAL PRIMARY KEY,
    college_id BIGINT REFERENCES college(id) ON DELETE RESTRICT,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    is_owner BOOLEAN NOT NULL DEFAULT FALSE,
    owner_access_requested BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    failed_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (is_owner OR college_id IS NOT NULL)
);

CREATE TABLE bus (
    id BIGSERIAL PRIMARY KEY,
    college_id BIGINT NOT NULL REFERENCES college(id) ON DELETE CASCADE,
    bus_number VARCHAR(30) NOT NULL,
    registration_number VARCHAR(30) NOT NULL,
    bus_type VARCHAR(20) NOT NULL CHECK (bus_type IN ('AC', 'NON_AC')),
    photo_url TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (college_id, bus_number)
);

CREATE TABLE route (
    id BIGSERIAL PRIMARY KEY,
    bus_id BIGINT NOT NULL REFERENCES bus(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE stop (
    id BIGSERIAL PRIMARY KEY,
    college_id BIGINT NOT NULL REFERENCES college(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE route_stop (
    id BIGSERIAL PRIMARY KEY,
    route_id BIGINT NOT NULL REFERENCES route(id) ON DELETE CASCADE,
    stop_id BIGINT NOT NULL REFERENCES stop(id) ON DELETE CASCADE,
    sequence_order INT NOT NULL,
    morning_time TIME NOT NULL,
    evening_time TIME NOT NULL,
    UNIQUE (route_id, sequence_order)
);

CREATE TABLE run_schedule (
    id BIGSERIAL PRIMARY KEY,
    route_id BIGINT NOT NULL REFERENCES route(id) ON DELETE CASCADE,
    run_date DATE NOT NULL,
    morning_start TIME,
    morning_end TIME,
    evening_start TIME,
    evening_end TIME,
    is_running BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (route_id, run_date)
);

CREATE TABLE driver (
    id BIGSERIAL PRIMARY KEY,
    college_id BIGINT NOT NULL REFERENCES college(id) ON DELETE CASCADE,
    login_id VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    dob DATE NOT NULL,
    must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    failed_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    active_device_id VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE student (
    id BIGSERIAL PRIMARY KEY,
    college_id BIGINT NOT NULL REFERENCES college(id) ON DELETE CASCADE,
    reg_no VARCHAR(60) NOT NULL,
    name VARCHAR(150) NOT NULL,
    address TEXT,
    department VARCHAR(100) NOT NULL,
    year INT NOT NULL,
    dob DATE NOT NULL,
    college_email VARCHAR(255) UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    failed_dob_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (college_id, reg_no) 
);

CREATE TABLE student_route_pref (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES student(id) ON DELETE CASCADE,
    route_id BIGINT NOT NULL REFERENCES ROUTE(id) ON DELETE CASCADE,
    stop_id BIGINT NOT NULL REFERENCES stop(id) ON DELETE CASCADE,
    trip_type VARCHAR(10) NOT NULL CHECK (trip_type IN ('MORNING', 'EVENING')),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (student_id, trip_type)
 );

 CREATE TABLE trip(
    id BIGSERIAL PRIMARY KEY,
    route_id BIGINT NOT NULL REFERENCES route(id) ON DELETE RESTRICT,
    driver_id BIGINT REFERENCES driver(id) ON DELETE SET NULL,
    trip_type VARCHAR(10) NOT NULL CHECK (trip_type IN ('MORNING', 'EVENING')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('RUNNING', 'ENDED','AUTO_ENDED')),
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ended_at TIMESTAMPTZ,
    ended_by VARCHAR(20) CHECK (ended_by IN ('DRIVER','AUTO','ADMIN')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
 );

 CREATE TABLE location_ping (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL REFERENCES trip(id) ON DELETE CASCADE,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    speed DOUBLE PRECISION,
    accuracy DOUBLE PRECISION,
    source VARCHAR(10) NOT NULL CHECK (source IN ('DRIVER', 'STUDENT')),
    source_user_id BIGINT
 );

 CREATE TABLE stop_arrival (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL REFERENCES trip(id) ON DELETE CASCADE,
    stop_id BIGINT NOT NULL REFERENCES stop(id) ON DELETE CASCADE,
    arrived_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (trip_id, stop_id)
 );

 CREATE TABLE bus_request(
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES student(id) ON DELETE CASCADE,
    route_id BIGINT NOT NULL REFERENCES route(id) ON DELETE CASCADE,
    trip_id BIGINT REFERENCES trip(id) ON DELETE SET NULL,
    request_date DATE NOT NULL,
    trip_type VARCHAR(10) NOT NULL CHECK (trip_type IN ('MORNING','EVENING')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (student_id, route_id, request_date, trip_type)
 );

 CREATE TABLE bug_report (
    id BIGSERIAL PRIMARY KEY,
    app_name VARCHAR(50) NOT NULL,
    app_version VARCHAR(30),
    device_info VARCHAR(255),
    reporter_id BIGINT,
    role VARCHAR(20) NOT NULL,
    description TEXT NOT NULL,
    screenshot_url TEXT,
    emailed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
 );

 CREATE TABLE email_otp (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
 );

 CREATE TABLE alert (
    id BIGSERIAL PRIMARY KEY,
    alert_type VARCHAR(30) NOT NULL CHECK (alert_type IN ('MISSED_START', 'AUTO_ENDED', 'SIGNAL_LOST', 'BUS_NOT_RUNNING')),
    route_id BIGINT REFERENCES route(id) ON DELETE CASCADE,
    trip_id BIGINT REFERENCES trip(id) ON DELETE SET NULL,
    request_count INT NOT NULL DEFAULT 0,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
 );

 CREATE INDEX idx_trip_route_status ON trip (route_id, status);
 CREATE INDEX idx_ping_trip_time ON location_ping (trip_id, recorded_at DESC);
 CREATE INDEX idx_schedule_route_date ON run_schedule (route_id, run_date);
 CREATE INDEX idx_arrival_stop_time ON stop_arrival (stop_id, arrived_at);
 CREATE INDEX idx_email_otp_email ON email_otp (email, expires_at DESC);
 CREATE INDEX idx_alert_resolved ON alert (resolved, created_at DESC);