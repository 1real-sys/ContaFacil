import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { LoginRequest } from '../../models/login-request.model';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Login {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  email = '';
  password = '';
  loading = signal(false);
  error = signal('');
  showPassword = signal(false);

  onSubmit(): void {
    if (!this.email || !this.password) {
      this.error.set('Preencha todos os campos.');
      return;
    }

    this.loading.set(true);
    this.error.set('');

    const data: LoginRequest = {
      email: this.email,
      password: this.password,
    };

    this.authService.login(data).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/dashboard']);
      },
      error: (err: Error) => {
        this.loading.set(false);
        this.error.set(err.message || 'Credenciais inválidas.');
      },
    });
  }
}
