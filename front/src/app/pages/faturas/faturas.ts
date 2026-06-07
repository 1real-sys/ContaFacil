import { Component, inject, OnInit, signal, computed, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ContaService } from '../../core/services/conta.service';
import { CartaoResponse } from '../../models/cartao-response.model';
import { FaturaResponse } from '../../models/fatura-response.model';
import { CompraCartaoResponse } from '../../models/compra-cartao.model';
import { HttpErrorResponse } from '@angular/common/http';

const PAGE_SIZE = 10;

@Component({
  selector: 'app-faturas',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './faturas.html',
  styleUrl: './faturas.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Faturas implements OnInit {
  private readonly contaService = inject(ContaService);

  cartaoAtivo = signal<CartaoResponse | null>(null);
  faturas = signal<FaturaResponse[]>([]);
  faturaSelecionada = signal<FaturaResponse | null>(null);

  loadingFaturas = signal(true);
  loadingCartao = signal(true);
  error = signal('');

  showPagar = signal(false);
  valorPagamento = signal('');
  pagando = signal(false);
  pagarError = signal('');

  visibleCount = signal(PAGE_SIZE);

  compraDetalhe = signal<CompraCartaoResponse | null>(null);
  cancelando = signal(false);
  estornando = signal(false);
  confirmandoAcao = signal<'cancelar' | 'estornar' | null>(null);

  comprasVisiveis = computed(() => {
    const compras = this.faturaSelecionada()?.compras ?? [];
    return [...compras].reverse().slice(0, this.visibleCount());
  });

  mostrarMais = computed(() => {
    const compras = this.faturaSelecionada()?.compras ?? [];
    return this.visibleCount() < compras.length;
  });

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.loadingCartao.set(true);
    this.loadingFaturas.set(true);
    this.error.set('');

    this.contaService.meusCartoes().subscribe({
      next: (cards) => {
        const ativo = cards.find((c) => c.status !== 'CANCELADO') ?? cards[0] ?? null;
        this.cartaoAtivo.set(ativo);
        this.loadingCartao.set(false);
        if (ativo) this.carregarHistorico();
        else this.loadingFaturas.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.loadingCartao.set(false);
        this.loadingFaturas.set(false);
        this.error.set(err.error?.message || err.error || 'Erro ao carregar cartão.');
      },
    });
  }

  carregarHistorico(): void {
    const card = this.cartaoAtivo();
    if (!card) return;
    this.loadingFaturas.set(true);
    this.contaService.historicoFaturas(card.id).subscribe({
      next: (faturas) => {
        this.faturas.set(faturas);
        this.loadingFaturas.set(false);
        const atual = this.findFaturaAtual(faturas);
        if (atual) this.selecionarFatura(atual);
        else if (faturas.length > 0) this.selecionarFatura(faturas[0]);
      },
      error: (err: HttpErrorResponse) => {
        this.loadingFaturas.set(false);
        this.error.set(err.error?.message || err.error || 'Erro ao carregar faturas.');
      },
    });
  }

  private findFaturaAtual(faturas: FaturaResponse[]): FaturaResponse | null {
    const hoje = new Date();
    const mes = hoje.getMonth() + 1;
    const ano = hoje.getFullYear();
    return faturas.find((f) => f.mes === mes && f.ano === ano) ?? null;
  }

  selecionarFatura(fatura: FaturaResponse): void {
    this.faturaSelecionada.set(fatura);
    this.visibleCount.set(PAGE_SIZE);
    this.showPagar.set(false);
    this.pagarError.set('');
  }

  findById(id: number): FaturaResponse {
    return this.faturas().find((f) => f.id === id)!;
  }

  pagarFatura(): void {
    const fatura = this.faturaSelecionada();
    const card = this.cartaoAtivo();
    if (!fatura || !card) return;

    this.pagarError.set('');
    const valorNum = parseFloat(
      this.valorPagamento().replace(/\./g, '').replace(',', '.')
    );

    if (isNaN(valorNum) || valorNum < 0.01) {
      this.pagarError.set('Informe um valor válido maior que zero.');
      return;
    }

    this.pagando.set(true);
    this.contaService.pagarFatura(fatura.id, valorNum).subscribe({
      next: (res) => {
        this.pagando.set(false);
        this.showPagar.set(false);
        this.valorPagamento.set('');
        this.carregarHistorico();
      },
      error: (err: HttpErrorResponse) => {
        this.pagando.set(false);
        this.pagarError.set(err.error?.message || err.error || 'Erro ao pagar fatura.');
      },
    });
  }

  abrirDetalhe(compra: CompraCartaoResponse): void {
    this.compraDetalhe.set(compra);
  }

  fecharDetalhe(): void {
    this.compraDetalhe.set(null);
    this.confirmandoAcao.set(null);
  }

  confirmarCancelamento(): void {
    this.confirmandoAcao.set('cancelar');
  }

  confirmarEstorno(): void {
    this.confirmandoAcao.set('estornar');
  }

  cancelarConfirmacao(): void {
    this.confirmandoAcao.set(null);
  }

  executarCancelamento(): void {
    const compra = this.compraDetalhe();
    if (!compra) return;
    this.cancelando.set(true);
    this.contaService.cancelarCompra(compra.id).subscribe({
      next: () => {
        this.cancelando.set(false);
        this.confirmandoAcao.set(null);
        this.compraDetalhe.set(null);
        this.carregarHistorico();
      },
      error: (err: HttpErrorResponse) => {
        this.cancelando.set(false);
        this.confirmandoAcao.set(null);
        this.error.set(err.error?.message || err.error || 'Erro ao cancelar compra.');
      },
    });
  }

  executarEstorno(): void {
    const compra = this.compraDetalhe();
    if (!compra) return;
    this.estornando.set(true);
    this.contaService.estornarCompra(compra.id).subscribe({
      next: () => {
        this.estornando.set(false);
        this.confirmandoAcao.set(null);
        this.compraDetalhe.set(null);
        this.carregarHistorico();
      },
      error: (err: HttpErrorResponse) => {
        this.estornando.set(false);
        this.confirmandoAcao.set(null);
        this.error.set(err.error?.message || err.error || 'Erro ao estornar compra.');
      },
    });
  }

  verMais(): void {
    this.visibleCount.update((v) => v + PAGE_SIZE);
  }

  nomeMes(mes: number): string {
    const nomes = [
      'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
      'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro',
    ];
    return nomes[mes - 1] ?? '';
  }

  formatCurrency(value: number): string {
    return value.toLocaleString('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    });
  }

  formatDate(dateStr: string): string {
    const date = new Date(dateStr + 'T00:00:00');
    return date.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' });
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

  categoriaLabel(cat: string): string {
    const map: Record<string, string> = {
      RESTAURANTE: 'Restaurante',
      MERCADO: 'Mercado',
      POSTODECOMBUSTIVEL: 'Posto',
      SAUDE: 'Saúde',
      SHOPPING: 'Shopping',
    };
    return map[cat] || cat;
  }

  statusLabel(status: string): string {
    const map: Record<string, string> = {
      ABERTA: 'Aberta',
      FECHADA: 'Fechada',
      PAGA: 'Paga',
      ATRASADA: 'Atrasada',
      AUTORIZADA: 'Autorizada',
      CANCELADA: 'Cancelada',
    };
    return map[status] || status;
  }
}
