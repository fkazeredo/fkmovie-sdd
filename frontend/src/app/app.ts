import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastModule } from 'primeng/toast';

import { AppHeader } from './core/layout/app-header';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ToastModule, AppHeader],
  templateUrl: './app.html',
})
export class App {}
