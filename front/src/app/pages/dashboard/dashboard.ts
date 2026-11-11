import { Component, inject, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';
import { Router } from '@angular/router';
import { ContaService } from '../../core/services/conta.service';
import { StorageService } from '../../core/services/storage.service';
import { ContaResponse } from '../../models/conta-response.model';
import { CartaoResponse } from '../../models/cartao-response.model';
import { ExtratoItem } from '../../models/extrato-response.model';
import { FaturaResponse } from '../../models/fatura-response.model';
import { HttpErrorResponse } from '@angular/common/http';
import { formatLocalDate } from '../../core/local-date';
import { NotificationService, httpErrorMessage } from '../../core/services/notification.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Dashboard implements OnInit {
  private readonly contaService = inject(ContaService);
  private readonly storage = inject(StorageService);
  private readonly notification = inject(NotificationService);
  protected readonly router = inject(Router);

  conta = signal<ContaResponse | null>(null);
  cartoes = signal<CartaoResponse[]>([]);
  faturaAtual = signal<FaturaResponse | null>(null);
  ultimasTransacoes = signal<ExtratoItem[]>([]);

  loadingConta = signal(true);
  loadingTransacoes = signal(true);
  loadingCartoes = signal(true);
  loadingFatura = signal(false);
  criandoConta = signal(false);

  balanceVisible = signal(true);

  userName = signal(this.storage.getUserName() || 'Usuário');

  ngOnInit(): void {
    this.loadConta();
    this.loadTransactions();
    this.loadCartoes();
  }

  loadConta(): void {
    this.loadingConta.set(true);
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

  criarConta(): void {
    this.criandoConta.set(true);
    this.contaService.abrirConta().subscribe({
      next: (c) => {
        this.conta.set(c);
        this.criandoConta.set(false);
        this.notification.success('Conta criada com sucesso.');
        this.loadTransactions();
      },
      error: (err: HttpErrorResponse) => {
        this.criandoConta.set(false);
        this.notification.error(httpErrorMessage(err, 'Não foi possível criar a conta.'));
      },
    });
  }

  toggleBalance(): void {
    this.balanceVisible.update((v) => !v);
  }

  navigateToTransacoes(tab: 'depositar' | 'transferir'): void {
    this.router.navigate(['/transacoes'], { queryParams: { tab } });
  }

  private loadTransactions(): void {
    this.loadingTransacoes.set(true);
    const hoje = new Date();
    const trintaDiasAtras = new Date();
    trintaDiasAtras.setDate(trintaDiasAtras.getDate() - 30);

    const dataInicio = formatLocalDate(trintaDiasAtras);
    const dataFim = formatLocalDate(hoje);

    this.contaService.extrato(dataInicio, dataFim).subscribe({
      next: (extrato) => {
        this.ultimasTransacoes.set(
          extrato.itens.slice(0, 5)
        );
        this.loadingTransacoes.set(false);
      },
      error: () => {
        this.loadingTransacoes.set(false);
      },
    });
  }

  private loadCartoes(): void {
    this.loadingCartoes.set(true);
    this.contaService.meusCartoes().subscribe({
      next: (cards) => {
        this.cartoes.set(cards);
        this.loadingCartoes.set(false);
        this.loadFaturaAtual();
      },
      error: () => {
        this.loadingCartoes.set(false);
      },
    });
  }

  private loadFaturaAtual(): void {
    const card = this.cartaoAtivo();
    if (!card) return;
    this.loadingFatura.set(true);
    this.contaService.faturaAtual(card.id).subscribe({
      next: (fatura) => {
        this.faturaAtual.set(fatura);
        this.loadingFatura.set(false);
      },
      error: () => {
        this.faturaAtual.set(null);
        this.loadingFatura.set(false);
      },
    });
  }

  cartaoAtivo(): CartaoResponse | null {
    return this.cartoes().find((c) => c.status === 'ATIVO') ?? this.cartoes()[0] ?? null;
  }

  formatCurrency(value: number): string {
    return value.toLocaleString('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    });
  }

  formatDate(dateStr: string): string {
    const date = new Date(dateStr);
    return date.toLocaleDateString('pt-BR', { day: '2-digit', month: 'short' });
  }

  tipoLabel(tipo: string): string {
    const map: Record<string, string> = {
      DEPOSITO: 'Depósito',
      SAQUE: 'Saque',
      TED: 'Transferência',
      PAGAMENTO_FATURA_CARTAO: 'Pagamento Fatura',
      CREDITO_CONFIANCA: 'Crédito de confiança',
    };
    return map[tipo] || tipo;
  }

  tipoIcon(tipo: string): string {
    const map: Record<string, string> = {
      DEPOSITO: '\u2193',
      SAQUE: '\u2191',
      TED: '\u2192',
      PAGAMENTO_FATURA_CARTAO: '\u{1F4B3}',
      CREDITO_CONFIANCA: '\u2193',
    };
    return map[tipo] || '\u2022';
  }

  tipoClass(tipo: string): string {
    return tipo.toLowerCase();
  }
}
