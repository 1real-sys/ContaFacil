import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ThemeService, Theme } from '../../core/services/theme.service';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './landing.html',
  styleUrl: './landing.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Landing {
  private readonly themeService = inject(ThemeService);

  readonly theme = this.themeService.theme;
  readonly themes: Theme[] = ['purple', 'orange', 'green', 'red'];

  setTheme(t: Theme): void {
    this.themeService.setTheme(t);
  }
}
