import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { MessageService } from 'primeng/api';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { Movie } from '../../admin.model';
import { AdminApiService } from '../../admin-api.service';
import { MoviesPage } from './movies-page';

const movie: Movie = {
  id: 'm1',
  title: 'Duna',
  durationMinutes: 166,
  synopsis: null,
  posterUrl: null,
  ageRating: 'A14',
  status: 'ACTIVE',
};

const page = { content: [movie], page: 0, size: 50, totalElements: 1, totalPages: 1 };

async function render(api: Partial<AdminApiService>): Promise<ComponentFixture<MoviesPage>> {
  await TestBed.configureTestingModule({
    imports: [MoviesPage],
    providers: [
      provideTranslateService(),
      { provide: AdminApiService, useValue: { listMovies: () => of(page), ...api } },
      { provide: MessageService, useValue: { add: vi.fn() } },
    ],
  }).compileComponents();
  const fixture = TestBed.createComponent(MoviesPage);
  fixture.detectChanges();
  await fixture.whenStable();
  fixture.detectChanges();
  return fixture;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function instance(fixture: ComponentFixture<MoviesPage>): any {
  return fixture.componentInstance;
}

describe('MoviesPage', () => {
  it('renders the movie list', async () => {
    const fixture = await render({});
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Duna');
  });

  it('does not create when the form is invalid', async () => {
    const createMovie = vi.fn();
    const fixture = await render({ createMovie });
    instance(fixture).openCreate();
    instance(fixture).form.patchValue({ title: '' });
    instance(fixture).save();
    expect(createMovie).not.toHaveBeenCalled();
  });

  it('creates a valid movie', async () => {
    const createMovie = vi.fn().mockReturnValue(of(movie));
    const fixture = await render({ createMovie });
    instance(fixture).openCreate();
    instance(fixture).form.setValue({
      title: 'Alien',
      durationMinutes: 117,
      ageRating: 'A16',
      synopsis: '',
      posterUrl: '',
    });
    instance(fixture).save();
    expect(createMovie).toHaveBeenCalledWith({
      title: 'Alien',
      durationMinutes: 117,
      ageRating: 'A16',
      synopsis: null,
      posterUrl: null,
    });
  });

  it('updates an existing movie when editing', async () => {
    const updateMovie = vi.fn().mockReturnValue(of(movie));
    const fixture = await render({ updateMovie });
    instance(fixture).openEdit(movie);
    instance(fixture).form.patchValue({ title: 'Duna: Parte Dois' });
    instance(fixture).save();
    expect(updateMovie).toHaveBeenCalledWith(
      'm1',
      expect.objectContaining({ title: 'Duna: Parte Dois' }),
    );
  });
});
