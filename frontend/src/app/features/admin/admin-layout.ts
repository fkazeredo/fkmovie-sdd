import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

/** Admin console shell: a tab bar over the back-office pages (SPEC-0026). */
@Component({
  selector: 'app-admin-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, TranslatePipe],
  template: `
    <div class="mx-auto max-w-6xl px-6 pt-6">
      <nav class="flex gap-1 border-b border-white/10">
        <a
          routerLink="/admin/filmes"
          routerLinkActive="border-primary text-primary"
          [routerLinkActiveOptions]="{ exact: false }"
          class="-mb-px border-b-2 border-transparent px-4 py-2 text-sm font-medium text-slate-400 hover:text-white"
          >{{ 'admin.movies.title' | translate }}</a
        >
        <a
          routerLink="/admin/sessoes"
          routerLinkActive="border-primary text-primary"
          class="-mb-px border-b-2 border-transparent px-4 py-2 text-sm font-medium text-slate-400 hover:text-white"
          >{{ 'admin.screenings.title' | translate }}</a
        >
      </nav>
    </div>
    <router-outlet />
  `,
})
export class AdminLayout {}
