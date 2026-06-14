import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { MessageService } from 'primeng/api';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { AdminApiService } from '../../admin-api.service';
import { ScreeningsAdminPage } from './screenings-admin-page';

const emptyPage = { content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 };

function baseApi(over: Partial<AdminApiService> = {}): Partial<AdminApiService> {
  return {
    listScreenings: () => of(emptyPage),
    listMovies: () => of(emptyPage),
    listRooms: () => of([{ id: 'r1', name: 'Room 1' }]),
    ...over,
  };
}

async function render(
  api: Partial<AdminApiService>,
): Promise<ComponentFixture<ScreeningsAdminPage>> {
  await TestBed.configureTestingModule({
    imports: [ScreeningsAdminPage],
    providers: [
      provideTranslateService(),
      { provide: AdminApiService, useValue: api },
      { provide: MessageService, useValue: { add: vi.fn() } },
    ],
  }).compileComponents();
  const fixture = TestBed.createComponent(ScreeningsAdminPage);
  fixture.detectChanges();
  await fixture.whenStable();
  fixture.detectChanges();
  return fixture;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function instance(fixture: ComponentFixture<ScreeningsAdminPage>): any {
  return fixture.componentInstance;
}

describe('ScreeningsAdminPage', () => {
  it('converts the SP wall-clock input to a UTC instant on save', async () => {
    const createScreening = vi.fn().mockReturnValue(of({}));
    const fixture = await render(baseApi({ createScreening }));

    instance(fixture).form.setValue({
      movieId: 'm1',
      roomId: 'r1',
      startsAtLocal: '2026-06-15T20:30',
      basePriceReais: 30,
    });
    instance(fixture).save();

    expect(createScreening).toHaveBeenCalledWith({
      movieId: 'm1',
      roomId: 'r1',
      startsAt: '2026-06-15T23:30:00.000Z',
      basePriceCents: 3000,
    });
  });

  it('does not submit an incomplete form', async () => {
    const createScreening = vi.fn();
    const fixture = await render(baseApi({ createScreening }));
    instance(fixture).save();
    expect(createScreening).not.toHaveBeenCalled();
  });
});
