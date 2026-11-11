import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NotificationService } from './core/services/notification.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  template: `
    <router-outlet />
    @if (notification.current(); as notice) {
      @for (item of [notice]; track item.id) {
        <div class="notification-region" [attr.role]="item.kind === 'error' ? 'alert' : 'status'">
          <div class="notification" [class.error]="item.kind === 'error'">
            <span class="notification-icon" aria-hidden="true">{{ item.kind === 'error' ? '!' : '✓' }}</span>
            <span class="notification-message">{{ item.message }}</span>
            <button type="button" class="notification-close" aria-label="Fechar notificação" (click)="notification.dismiss()">×</button>
          </div>
        </div>
      }
    }
  `,
  styles: `
    :host {
      display: block;
      min-height: 100vh;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {
  protected readonly notification = inject(NotificationService);
}
