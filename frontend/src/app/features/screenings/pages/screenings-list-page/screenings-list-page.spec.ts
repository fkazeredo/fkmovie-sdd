import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';

import { ApiError } from '../../../../core/http/api-error.model';
import { PageResponse, PublicScreening } from '../../screening.model';
import { ScreeningsService } from '../../screenings.service';
import { ScreeningsListPage } from './screenings-list-page';

function page(content: PublicScreening[]): PageResponse<PublicScreening> {
  return { content, page: 0, size: 50, totalElements: content.length, totalPages: 1 };
}

const sample: PublicScreening = {
  id: 'a1',
  movieTitle: 'Duna Parte Dois',
  ageRating: 'A14',
  posterUrl: null,
  roomName: 'Room 1',
  startsAt: '2026-06-20T20:00:00Z',
  durationMinutes: 166,
  fromPriceCents: 3000,
};

async function render(stub: Partial<ScreeningsService>): Promise<ComponentFixture<ScreeningsListPage>> {
  await TestBed.configureTestingModule({
    imports: [ScreeningsListPage],
    providers: [
      provideRouter([]),
      provideTranslateService(),
      { provide: ScreeningsService, useValue: stub as unknown as ScreeningsService },
    ],
  }).compileComponents();
  const fixture = TestBed.createComponent(ScreeningsListPage);
  fixture.detectChanges();
  await fixture.whenStable();
  fixture.detectChanges();
  return fixture;
}

describe('ScreeningsListPage', () => {
  it('renders a card per screening', async () => {
    const fixture = await render({ list: () => of(page([sample])) });
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Duna Parte Dois');
    expect(text).toContain('Room 1');
    expect((fixture.nativeElement as HTMLElement).querySelectorAll('article').length).toBe(1);
  });

  it('shows the empty state when there are no screenings', async () => {
    const fixture = await render({ list: () => of(page([])) });
    expect((fixture.nativeElement as HTMLElement).querySelectorAll('article').length).toBe(0);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('screenings.empty');
  });

  it('shows the error state when loading fails', async () => {
    const apiError: ApiError = { code: 'network.error', message: 'boom', fields: [], status: 0 };
    const fixture = await render({ list: () => throwError(() => apiError) });
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('boom');
  });
});
