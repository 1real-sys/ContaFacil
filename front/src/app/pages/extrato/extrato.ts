import { Component, inject, OnInit, signal, computed, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ContaService } from '../../core/services/conta.service';
import { ExtratoResponse, ExtratoItem } from '../../models/extrato-response.model';
import { HttpErrorResponse } from '@angular/common/http';

const PAGE_SIZE = 5;

@Component({
  selector: 'app-extrato',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './extrato.html',
  styleUrl: './extrato.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Extrato implements OnInit {
  private readonly contaService = inject(ContaService);

  extrato = signal<ExtratoResponse | null>(null);
  loading = signal(false);
  error = signal('');

  dataInicio = signal('');
  dataFim = signal('');
  tipo = signal('');

  visibleCount = signal(PAGE_SIZE);
  hoje = new Date().toISOString().split('T')[0];

  itensVisiveis = computed<ExtratoItem[]>(() => {
    const items = this.extrato()?.itens ?? [];
    return items.length > 0 ? items.slice(-this.visibleCount()) : items;
  });

  mostrarMais = computed(() => {
    const items = this.extrato()?.itens ?? [];
    return this.visibleCount() < items.length;
  });

  ngOnInit(): void {
    const hoje = this.hoje;
    this.dataInicio.set(hoje);
    this.dataFim.set(hoje);
    this.carregarExtrato();
  }

  carregarExtrato(): void {
    this.error.set('');
    this.loading.set(true);

    const tipoVal = this.tipo() || undefined;

    this.contaService.extrato(this.dataInicio(), this.dataFim(), tipoVal).subscribe({
      next: (res) => {
        this.extrato.set(res);
        this.visibleCount.set(PAGE_SIZE);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(err.error?.message || err.error || 'Erro ao carregar extrato.');
      },
    });
  }

  filtrar(): void {
    const inicio = this.dataInicio();
    const fim = this.dataFim();

    if (!inicio || !fim) {
      this.error.set('Selecione as datas inicial e final.');
      return;
    }

    if (inicio > this.hoje || fim > this.hoje) {
      this.error.set('Não é permitido selecionar datas futuras.');
      return;
    }

    const msDiff = new Date(fim).getTime() - new Date(inicio).getTime();
    const daysDiff = msDiff / (1000 * 60 * 60 * 24);

    if (daysDiff > 365) {
      this.error.set('O período não pode ultrapassar 1 ano.');
      return;
    }

    if (daysDiff < 0) {
      this.error.set('A data final deve ser maior ou igual à data inicial.');
      return;
    }

    this.carregarExtrato();
  }

  verMais(): void {
    this.visibleCount.update((v) => v + PAGE_SIZE);
  }

  formatCurrency(value: number): string {
    return value.toLocaleString('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    });
  }

  formatDateTime(dateStr: string): string {
    const date = new Date(dateStr);
    return date.toLocaleString('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  tipoLabel(tipo: string): string {
    const map: Record<string, string> = {
      DEPOSITO: 'Depósito',
      SAQUE: 'Saque',
      TED: 'Transferência',
      PAGAMENTO_FATURA_CARTAO: 'Pagamento Fatura',
    };
    return map[tipo] || tipo;
  }

  tipoIcon(tipo: string): string {
    const map: Record<string, string> = {
      DEPOSITO: '\u2193',
      SAQUE: '\u2191',
      TED: '\u2192',
      PAGAMENTO_FATURA_CARTAO: '\u{1F4B3}',
    };
    return map[tipo] || '\u2022';
  }
}
