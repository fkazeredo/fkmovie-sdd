/**
 * Timezone helpers for the screening scheduler (SPEC-0026). Admins think in America/São Paulo wall
 * time, but the API speaks UTC. Brazil has no DST under current rules, so São Paulo is a fixed
 * UTC−03:00 — we encode that offset explicitly rather than relying on the host's local zone.
 */
const SP_OFFSET = '-03:00';
const SP_OFFSET_MS = 3 * 60 * 60 * 1000;

/** Converts a `datetime-local` value (SP wall time, "YYYY-MM-DDTHH:mm") to a UTC ISO instant. */
export function spLocalToUtcIso(local: string): string {
  return new Date(`${local}:00${SP_OFFSET}`).toISOString();
}

/** Converts a UTC ISO instant to a `datetime-local` value in SP wall time ("YYYY-MM-DDTHH:mm"). */
export function utcIsoToSpLocal(iso: string): string {
  const spWallClock = new Date(new Date(iso).getTime() - SP_OFFSET_MS);
  return spWallClock.toISOString().slice(0, 16);
}
