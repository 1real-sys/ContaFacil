import { Component, inject, signal, computed, ChangeDetectionStrategy } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { RegisterRequest } from '../../models/register-request.model';

interface RuleCheck {
  label: string;
  ok: boolean;
}

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Register {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  username = '';
  email = '';
  password = signal('');
  confirmPassword = '';
  loading = signal(false);
  error = signal('');
  showPassword = signal(false);

  passwordRules = computed((): RuleCheck[] => {
    const pwd = this.password();
    return [
      { label: 'Mínimo de 10 caracteres', ok: pwd.length >= 10 },
      { label: 'Ao menos 1 número', ok: /[0-9]/.test(pwd) },
      { label: 'Ao menos 1 letra maiúscula', ok: /[A-Z]/.test(pwd) },
    ];
  });

  passwordValid = computed(() => this.passwordRules().every((r) => r.ok));

  onSubmit(): void {
    this.error.set('');

    if (!this.username || !this.email || !this.password() || !this.confirmPassword) {
      this.error.set('Preencha todos os campos.');
      return;
    }

    if (!this.passwordValid()) {
      return;
    }

    if (this.password() !== this.confirmPassword) {
      this.error.set('As senhas não conferem.');
      return;
    }

    this.loading.set(true);

    const data: RegisterRequest = {
      username: this.username,
      email: this.email,
      password: this.password(),
    };

    this.authService.register(data).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/dashboard']);
      },
      error: (err: Error) => {
        this.loading.set(false);
        this.error.set(err.message || 'Erro ao criar conta.');
      },
    });
  }
}
