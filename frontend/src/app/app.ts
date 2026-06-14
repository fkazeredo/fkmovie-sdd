import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastModule } from 'primeng/toast';

import { AppHeader } from './core/layout/app-header';
import { VerifyEmailBanner } from './core/layout/verify-email-banner';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ToastModule, AppHeader, VerifyEmailBanner],
  templateUrl: './app.html',
})
export class App {}
