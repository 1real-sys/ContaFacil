import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, tap, catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginRequest } from '../../models/login-request.model';
import { RegisterRequest } from '../../models/register-request.model';
import { AuthResponse } from '../../models/auth-response.model';
import { StorageService } from './storage.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly storage = inject(StorageService);
  private readonly baseUrl = `${environment.apiUrl}/auth`;

  login(data: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/login`, data).pipe(
      tap((res) => {
        this.storage.setToken(res.token);
        this.storage.setUserName(res.name);
      }),
      catchError((err: HttpErrorResponse) => {
        const msg = this.extrairMensagem(err);
        return throwError(() => new Error(msg));
      })
    );
  }

  register(data: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/register`, data).pipe(
      tap((res) => {
        this.storage.setToken(res.token);
        this.storage.setUserName(res.name);
      }),
      catchError((err: HttpErrorResponse) => {
        const msg = this.extrairMensagem(err);
        return throwError(() => new Error(msg));
      })
    );
  }

  private extrairMensagem(err: HttpErrorResponse): string {
    if (typeof err.error === 'string' && err.error.length > 0) {
      return err.error;
    }

    const body = err.error;

    if (body?.detail ? typeof body.detail === 'string' : false) {
      return body.detail;
    }
    if (body?.message ? typeof body.message === 'string' : false) {
      return body.message;
    }
    if (body?.reason ? typeof body.reason === 'string' : false) {
      return body.reason;
    }

    return err.message || 'Erro inesperado. Tente novamente.';
  }

  logout(): void {
    this.storage.clear();
  }

  isAuthenticated(): boolean {
    return this.storage.isAuthenticated();
  }
}
