import { Component } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';

/**
 * Placeholder page (spec 0002 acceptance criteria): proves PrimeNG components and
 * Tailwind layout work together and that labels go through ngx-translate.
 * Replaced by the public screenings list in spec 0022.
 */
@Component({
  selector: 'app-home-page',
  imports: [CardModule, ButtonModule, TagModule, TranslatePipe],
  templateUrl: './home-page.html',
})
export class HomePage {}
