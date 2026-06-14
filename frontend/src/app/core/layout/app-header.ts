import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { ButtonModule } from 'primeng/button';
import { ToolbarModule } from 'primeng/toolbar';

import { AuthService } from '../auth/auth.service';
import { LanguageService } from '../config/language.service';

/** Application toolbar: brand, role-aware nav, language switch and the auth area (spec 0002/0021). */
@Component({
  selector: 'app-header',
  imports: [ToolbarModule, ButtonModule, TranslatePipe, RouterLink, RouterLinkActive],
  templateUrl: './app-header.html',
})
export class AppHeader {
  protected readonly language = inject(LanguageService);
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected logout(): void {
    this.auth.logout().subscribe(() => void this.router.navigateByUrl('/sessoes'));
  }
}
