-- Seed of the 4 fixed cinema rooms and their typed seats (SPEC-0007).
-- Idempotent by design: guarded by "skip if any room exists", so re-running outside Flyway
-- (e.g. a manual replay) never duplicates. Layout and seat-type rules come straight from the
-- spec: row A seats 1-2 ACCESSIBLE, 3-4 COMPANION; the last row of each room is VIP; the rest
-- STANDARD. No room has fewer than 6 rows, so the last-row-VIP rule never collides with row A.
DO $$
DECLARE
    room        RECORD;
    new_room_id UUID;
    r           INT;
    n           INT;
    seat_letter TEXT;
    seat_type   TEXT;
BEGIN
    IF EXISTS (SELECT 1 FROM cinema_rooms) THEN
        RAISE NOTICE 'SPEC-0007 seed skipped: cinema_rooms already populated';
        RETURN;
    END IF;

    FOR room IN
        SELECT * FROM (VALUES
            ('Room 1',  8, 10),
            ('Room 2',  8, 10),
            ('Room 3', 10, 12),
            ('Room 4',  6,  8)
        ) AS t(name, row_count, seat_count)
    LOOP
        new_room_id := gen_random_uuid();
        INSERT INTO cinema_rooms (id, tenant_id, name, created_at, updated_at)
        VALUES (new_room_id, 'default', room.name, now(), now());

        FOR r IN 1..room.row_count LOOP
            seat_letter := chr(64 + r);
            FOR n IN 1..room.seat_count LOOP
                IF r = 1 AND n IN (1, 2) THEN
                    seat_type := 'ACCESSIBLE';
                ELSIF r = 1 AND n IN (3, 4) THEN
                    seat_type := 'COMPANION';
                ELSIF r = room.row_count THEN
                    seat_type := 'VIP';
                ELSE
                    seat_type := 'STANDARD';
                END IF;

                INSERT INTO seats (id, room_id, seat_row, seat_number, type, created_at)
                VALUES (gen_random_uuid(), new_room_id, seat_letter, n, seat_type, now());
            END LOOP;
        END LOOP;
    END LOOP;

    RAISE NOTICE 'SPEC-0007 seed applied: % rooms, % seats',
        (SELECT count(*) FROM cinema_rooms), (SELECT count(*) FROM seats);
END $$;
