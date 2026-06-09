import { Component, inject, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { UserService } from '../../core/services/user.service';
import { ContaService } from '../../core/services/conta.service';
import { StorageService } from '../../core/services/storage.service';
import { UserPerfil } from '../../models/user-perfil.model';
import { ContaResponse } from '../../models/conta-response.model';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-perfil',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './perfil.html',
  styleUrl: './perfil.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Perfil implements OnInit {
  private readonly userService = inject(UserService);
  private readonly contaService = inject(ContaService);
  private readonly storage = inject(StorageService);

  perfil = signal<UserPerfil | null>(null);
  conta = signal<ContaResponse | null>(null);

  loadingPerfil = signal(true);
  loadingConta = signal(true);
  saving = signal(false);
  error = signal('');
  success = signal('');

  editingName = signal(false);
  editName = signal('');

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.loadingPerfil.set(true);
    this.loadingConta.set(true);
    this.error.set('');

    this.userService.perfil().subscribe({
      next: (p) => {
        this.perfil.set(p);
        this.loadingPerfil.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.loadingPerfil.set(false);
        this.error.set(err.error?.message || err.error || 'Erro ao carregar perfil.');
      },
    });

    this.contaService.minhaConta().subscribe({
      next: (c) => {
        this.conta.set(c);
        this.loadingConta.set(false);
      },
      error: () => {
        this.conta.set(null);
        this.loadingConta.set(false);
      },
    });
  }

  iniciarEdicao(): void {
    this.editName.set(this.perfil()?.username ?? '');
    this.editingName.set(true);
    this.error.set('');
    this.success.set('');
  }

  cancelarEdicao(): void {
    this.editingName.set(false);
    this.error.set('');
  }

  salvarNome(): void {
    const novo = this.editName().trim();
    if (!novo) {
      this.error.set('O nome não pode ficar vazio.');
      return;
    }

    this.saving.set(true);
    this.error.set('');
    this.success.set('');

    this.userService.atualizarNome(novo).subscribe({
      next: (p) => {
        this.perfil.set(p);
        this.storage.setUserName(p.username);
        this.saving.set(false);
        this.editingName.set(false);
        this.success.set('Nome atualizado com sucesso.');
      },
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        this.error.set(err.error?.message || err.error || 'Erro ao atualizar nome.');
      },
    });
  }
}
