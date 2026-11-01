import { Injectable, signal, effect } from '@angular/core';

export type Theme = 'purple' | 'orange' | 'green' | 'red';

const THEME_KEY = 'cf-theme-v2';
const LEGACY_THEME_KEY = 'cf-theme';
const THEME_CLASS_PREFIX = 'theme-';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly theme = signal<Theme>(this.load());

  constructor() {
    this.apply(this.theme());

    effect(() => {
      const t = this.theme();
      this.apply(t);
      this.save(t);
    });
  }

  setTheme(t: Theme): void {
    this.theme.set(t);
  }

  private apply(t: Theme): void {
    document.documentElement.classList.remove(
      `${THEME_CLASS_PREFIX}purple`,
      `${THEME_CLASS_PREFIX}orange`,
      `${THEME_CLASS_PREFIX}green`,
      `${THEME_CLASS_PREFIX}red`,
    );
    document.documentElement.classList.add(`${THEME_CLASS_PREFIX}${t}`);
  }

  private load(): Theme {
    try {
      const stored = localStorage.getItem(THEME_KEY);
      if (stored === 'orange' || stored === 'green' || stored === 'purple' || stored === 'red') {
        return stored;
      }

      const legacy = localStorage.getItem(LEGACY_THEME_KEY);
      // O tema roxo antigo também era salvo automaticamente; agora ele migra para verde.
      if (legacy === 'orange' || legacy === 'green' || legacy === 'red') {
        return legacy;
      }
    } catch {
      // localStorage indisponível
    }
    return 'green';
  }

  private save(t: Theme): void {
    try {
      localStorage.setItem(THEME_KEY, t);
    } catch {
      // localStorage indisponível
    }
  }
}
