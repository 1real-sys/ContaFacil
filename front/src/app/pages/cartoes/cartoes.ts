import { Component, inject, OnInit, OnDestroy, signal, computed, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ContaService } from '../../core/services/conta.service';
import { CartaoResponse } from '../../models/cartao-response.model';
import { CartaoDadosSensiveis } from '../../models/cartao-dados-sensiveis.model';
import { CompraCartaoResponse } from '../../models/compra-cartao.model';
import { HttpErrorResponse } from '@angular/common/http';
import { NotificationService, httpErrorMessage } from '../../core/services/notification.service';

@Component({
  selector: 'app-cartoes',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './cartoes.html',
  styleUrl: './cartoes.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Cartoes implements OnInit, OnDestroy {
  private readonly contaService = inject(ContaService);
  private readonly notification = inject(NotificationService);

  cartoes = signal<CartaoResponse[]>([]);
  loading = signal(true);
  error = signal('');
  acaoLoading = signal(false);

  showEmitirForm = signal(false);
  bandeira = signal('VISA');
  emitirLoading = signal(false);

  showCompraForm = signal(false);
  compraValor = signal('');
  compraEstabelecimento = signal('');
  compraCategoria = signal<'RESTAURANTE' | 'MERCADO' | 'POSTODECOMBUSTIVEL' | 'SAUDE' | 'SHOPPING'>('RESTAURANTE');
  compraLoading = signal(false);
  compraError = signal('');
  comprovanteCompra = signal<CompraCartaoResponse | null>(null);

  showConfirmCancel = signal(false);

  showDadosSensiveis = signal(false);
  dadosSensiveis = signal<CartaoDadosSensiveis | null>(null);
  loadingDados = signal(false);
  dadosError = signal('');
  contador = signal(30);
  private timerId: ReturnType<typeof setInterval> | null = null;

  cartaoPrincipal = computed(() => {
    const ativo = this.cartoes().find((c) => c.status === 'ATIVO');
    if (ativo) return ativo;
    const outros = this.cartoes().filter((c) => c.status !== 'CANCELADO');
    return outros.length > 0 ? outros[0] : this.cartoes()[0] ?? null;
  });

  podeComprar = computed(() => {
    const c = this.cartaoPrincipal();
    return c && c.status === 'ATIVO' && c.limiteDisponivel > 0;
  });

  categorias = ['RESTAURANTE', 'MERCADO', 'POSTODECOMBUSTIVEL', 'SAUDE', 'SHOPPING'] as const;

  ngOnInit(): void {
    this.carregarCartoes();
  }

  carregarCartoes(): void {
    this.loading.set(true);
    this.error.set('');
    this.contaService.meusCartoes().subscribe({
      next: (cards) => {
        this.cartoes.set(cards);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(err.error?.message || err.error || 'Erro ao carregar cartões.');
      },
    });
  }

  emitirCartao(): void {
    this.emitirLoading.set(true);
    this.contaService.emitirCartao(this.bandeira()).subscribe({
      next: () => {
        this.emitirLoading.set(false);
        this.showEmitirForm.set(false);
        this.notification.success('Cartão emitido com sucesso.');
        this.carregarCartoes();
      },
      error: (err: HttpErrorResponse) => {
        this.emitirLoading.set(false);
        this.notification.error(httpErrorMessage(err, 'Não foi possível emitir o cartão.'));
      },
    });
  }

  solicitarLimite(): void {
    const c = this.cartaoPrincipal();
    if (!c) return;
    this.acaoLoading.set(true);
    this.contaService.solicitarLimite(c.id).subscribe({
      next: (cartao) => {
        this.atualizarCartao(cartao);
        this.notification.success('Limite solicitado com sucesso.');
      },
      error: (err) => this.handleAcaoError(err),
    });
  }

  ativarCartao(): void {
    const c = this.cartaoPrincipal();
    if (!c) return;
    this.acaoLoading.set(true);
    this.contaService.desbloquearCartao(c.id).subscribe({
      next: (cartao) => {
        this.atualizarCartao(cartao);
        this.notification.success('Cartão ativado com sucesso.');
      },
      error: (err) => this.handleAcaoError(err),
    });
  }

  bloquearCartao(): void {
    const c = this.cartaoPrincipal();
    if (!c) return;
    this.acaoLoading.set(true);
    this.contaService.bloquearCartao(c.id).subscribe({
      next: (cartao) => {
        this.atualizarCartao(cartao);
        this.notification.success('Cartão bloqueado com sucesso.');
      },
      error: (err) => this.handleAcaoError(err),
    });
  }

  confirmarCancelamento(): void {
    const c = this.cartaoPrincipal();
    if (!c) return;
    this.showConfirmCancel.set(false);
    this.acaoLoading.set(true);
    this.contaService.cancelarCartao(c.id).subscribe({
      next: () => {
        this.acaoLoading.set(false);
        this.notification.success('Cartão cancelado com sucesso.');
        this.carregarCartoes();
      },
      error: (err) => this.handleAcaoError(err),
    });
  }

  lancarCompra(): void {
    this.compraError.set('');
    const valorNum = parseFloat(
      this.compraValor().replace(/\./g, '').replace(',', '.')
    );

    if (isNaN(valorNum) || valorNum < 0.01) {
      this.compraError.set('Informe um valor válido maior que zero.');
      return;
    }

    if (!this.compraEstabelecimento().trim()) {
      this.compraError.set('Informe o estabelecimento.');
      return;
    }

    const c = this.cartaoPrincipal();
    if (!c) return;

    this.compraLoading.set(true);
    this.contaService
      .lancarCompra(c.id, {
        valor: valorNum,
        estabelecimento: this.compraEstabelecimento().trim(),
        categoria: this.compraCategoria(),
      })
      .subscribe({
        next: (res) => {
          this.comprovanteCompra.set(res);
          this.compraLoading.set(false);
          this.notification.success('Compra efetuada com sucesso.');
          this.carregarCartoes();
        },
        error: (err: HttpErrorResponse) => {
          this.compraLoading.set(false);
          const message = httpErrorMessage(err, 'Não foi possível efetuar a compra.');
          this.notification.error(/limite insuficiente/i.test(message)
            ? 'Compra não efetuada: limite insuficiente.'
            : message);
        },
      });
  }

  private atualizarCartao(cartao: CartaoResponse): void {
    this.acaoLoading.set(false);
    const lista = this.cartoes().map((c) => (c.id === cartao.id ? cartao : c));
    this.cartoes.set(lista);
  }

  private handleAcaoError(err: HttpErrorResponse): void {
    this.acaoLoading.set(false);
    this.notification.error(httpErrorMessage(err, 'Não foi possível concluir a ação.'));
  }

  ngOnDestroy(): void {
    this.limparTimer();
  }

  formatCurrency(value: number): string {
    return value.toLocaleString('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    });
  }

  verDados(): void {
    const c = this.cartaoPrincipal();
    if (!c) return;
    this.dadosError.set('');
    this.loadingDados.set(true);
    this.showDadosSensiveis.set(true);
    this.contaService.dadosSensiveis(c.id).subscribe({
      next: (dados) => {
        this.dadosSensiveis.set(dados);
        this.loadingDados.set(false);
        this.iniciarContador();
      },
      error: (err: HttpErrorResponse) => {
        this.loadingDados.set(false);
        this.dadosError.set(err.error?.message || err.error || 'Erro ao carregar dados.');
      },
    });
  }

  fecharDados(): void {
    this.limparTimer();
    this.showDadosSensiveis.set(false);
    this.dadosSensiveis.set(null);
    this.dadosError.set('');
    this.contador.set(30);
  }

  private iniciarContador(): void {
    this.limparTimer();
    this.contador.set(30);
    this.timerId = setInterval(() => {
      const atual = this.contador() - 1;
      this.contador.set(atual);
      if (atual <= 0) {
        this.fecharDados();
      }
    }, 1000);
  }

  private limparTimer(): void {
    if (this.timerId !== null) {
      clearInterval(this.timerId);
      this.timerId = null;
    }
  }

  formatDate(dateStr: string): string {
    const date = new Date(dateStr);
    return date.toLocaleDateString('pt-BR', { month: '2-digit', year: '2-digit' });
  }

  formatDateTime(dateStr: string): string {
    const date = new Date(dateStr);
    return date.toLocaleString('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  statusLabel(status: string): string {
    const map: Record<string, string> = {
      ATIVO: 'Ativo',
      INATIVO: 'Inativo',
      BLOQUEADO: 'Bloqueado',
      CANCELADO: 'Cancelado',
    };
    return map[status] || status;
  }

  categoriaLabel(cat: string): string {
    const map: Record<string, string> = {
      RESTAURANTE: 'Restaurante',
      MERCADO: 'Mercado',
      POSTODECOMBUSTIVEL: 'Posto de Combustível',
      SAUDE: 'Saúde',
      SHOPPING: 'Shopping',
    };
    return map[cat] || cat;
  }
}
