import { spLocalToUtcIso, utcIsoToSpLocal } from './sp-time';

describe('São Paulo ↔ UTC conversion', () => {
  it('converts SP wall time to a UTC instant (+3h)', () => {
    expect(spLocalToUtcIso('2026-06-15T20:30')).toBe('2026-06-15T23:30:00.000Z');
  });

  it('converts a UTC instant back to SP wall time (−3h)', () => {
    expect(utcIsoToSpLocal('2026-06-15T23:30:00.000Z')).toBe('2026-06-15T20:30');
  });

  it('handles a day boundary across the offset', () => {
    // 23:30 SP on the 15th is 02:30 UTC on the 16th
    expect(spLocalToUtcIso('2026-06-15T23:30')).toBe('2026-06-16T02:30:00.000Z');
    expect(utcIsoToSpLocal('2026-06-16T02:30:00.000Z')).toBe('2026-06-15T23:30');
  });

  it('round-trips', () => {
    const local = '2026-12-31T18:45';
    expect(utcIsoToSpLocal(spLocalToUtcIso(local))).toBe(local);
  });
});
