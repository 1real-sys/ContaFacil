import { Injectable, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';

export function httpErrorMessage(error: HttpErrorResponse, fallback: string): string {
  if (typeof error.error === 'string' && error.error.trim()) return error.error;
  if (typeof error.error?.message === 'string' && error.error.message.trim()) return error.error.message;
  return fallback;
}

export type NotificationKind = 'success' | 'error';
export interface AppNotification {
  id: number;
  kind: NotificationKind;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  readonly current = signal<AppNotification | null>(null);
  private nextId = 0;
  private timeoutId: ReturnType<typeof setTimeout> | null = null;

  success(message: string): void {
    this.show('success', message);
  }

  error(message: string): void {
    this.show('error', message);
  }

  dismiss(): void {
    if (this.timeoutId !== null) clearTimeout(this.timeoutId);
    this.timeoutId = null;
    this.current.set(null);
  }

  private show(kind: NotificationKind, message: string): void {
    this.dismiss();
    this.current.set({ id: ++this.nextId, kind, message });
    this.timeoutId = setTimeout(() => this.dismiss(), 3000);
  }
}
