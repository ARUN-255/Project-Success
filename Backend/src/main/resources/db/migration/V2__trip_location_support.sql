CREATE UNIQUE INDEX uq_trip_running_route ON trip (route_id) WHERE status = 'RUNNING';
CREATE UNIQUE INDEX uq_trip_running_driver ON trip (driver_id) WHERE status = 'RUNNING';

ALTER TABLE student ADD COLUMN share_location_allowed BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE alert ADD COLUMN alert_date DATE NOT NULL DEFAULT CURRENT_DATE;
ALTER TABLE alert ADD COLUMN trip_type VARCHAR(10);
CREATE UNIQUE INDEX uq_alert_once ON alert (route_id, alert_type, alert_date, trip_type)
WHERE alert_type IN ('MISSED_START', 'AUTO_ENDED');