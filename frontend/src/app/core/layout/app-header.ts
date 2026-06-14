import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { ButtonModule } from 'primeng/button';
import { ToolbarModule } from 'primeng/toolbar';

import { LanguageService } from '../config/language.service';

/** Application toolbar: brand title, screenings nav and runtime language switch (spec 0002/0022). */
@Component({
  selector: 'app-header',
  imports: [ToolbarModule, ButtonModule, TranslatePipe, RouterLink, RouterLinkActive],
  templateUrl: './app-header.html',
})
export class AppHeader {
  protected readonly language = inject(LanguageService);
}
