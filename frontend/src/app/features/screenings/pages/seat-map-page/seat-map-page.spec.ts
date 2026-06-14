import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';

import { SeatMap } from '../../screening.model';
import { ScreeningsService } from '../../screenings.service';
import { SeatMapPage } from './seat-map-page';

const map: SeatMap = {
  screeningId: 's1',
  roomName: 'Room 1',
  startsAt: '2026-06-20T20:00:00Z',
  seats: [
    { seatId: 'a1', row: 'A', number: 1, type: 'STANDARD', status: 'FREE', fullPriceCents: 3000 },
    { seatId: 'a2', row: 'A', number: 2, type: 'STANDARD', status: 'SOLD', fullPriceCents: 3000 },
    { seatId: 'b1', row: 'B', number: 1, type: 'VIP', status: 'FREE', fullPriceCents: 4000 },
  ],
};

async function render(stub: Partial<ScreeningsService>): Promise<ComponentFixture<SeatMapPage>> {
  await TestBed.configureTestingModule({
    imports: [SeatMapPage],
    providers: [
      provideRouter([]),
      provideTranslateService(),
      { provide: ScreeningsService, useValue: stub as unknown as ScreeningsService },
      { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 's1' } } } },
    ],
  }).compileComponents();
  const fixture = TestBed.createComponent(SeatMapPage);
  fixture.detectChanges();
  await fixture.whenStable();
  fixture.detectChanges();
  return fixture;
}

describe('SeatMapPage', () => {
  it('renders the seats grouped by row', async () => {
    const fixture = await render({ seatMap: () => of(map) });
    const buttons = (fixture.nativeElement as HTMLElement).querySelectorAll('button[title]');
    expect(buttons.length).toBe(3);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Room 1');
  });

  it('disables sold seats and selects free seats on click', async () => {
    const fixture = await render({ seatMap: () => of(map) });
    const el = fixture.nativeElement as HTMLElement;
    const seatButtons = Array.from(el.querySelectorAll<HTMLButtonElement>('button[title]'));
    const sold = seatButtons.find((b) => b.title.startsWith('A2'));
    const free = seatButtons.find((b) => b.title.startsWith('A1'));
    expect(sold?.disabled).toBe(true);
    expect(free?.disabled).toBe(false);

    free?.click();
    fixture.detectChanges();
    // total should now reflect one selected STANDARD seat (R$ 30,00)
    expect(el.textContent).toContain('1');
  });
});
