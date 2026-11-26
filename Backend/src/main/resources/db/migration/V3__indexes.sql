CREATE INDEX IF NOT EXISTS idx_driver_college ON driver (college_id);
CREATE INDEX IF NOT EXISTS idx_pref_route ON student_route_pref (route_id, trip_type);
CREATE INDEX IF NOT EXISTS idx_request_route_date ON bus_request (route_id, request_date, trip_type);