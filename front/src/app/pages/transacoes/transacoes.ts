import { Component, inject, OnInit, OnDestroy, signal, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { ContaService } from '../../core/services/conta.service';
import { TransacaoResponse } from '../../models/transacao-response.model';
import { HttpErrorResponse } from '@angular/common/http';

type Tab = 'depositar' | 'sacar' | 'transferir';

@Component({
  selector: 'app-transacoes',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './transacoes.html',
  styleUrl: './transacoes.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Transacoes implements OnInit, OnDestroy {
  private readonly contaService = inject(ContaService);
  private readonly route = inject(ActivatedRoute);

  private queryParamsSub: Subscription | null = null;

  activeTab = signal<Tab>('depositar');
  loading = signal(false);
  error = signal('');
  comprovante = signal<TransacaoResponse | null>(null);

  tabs: Tab[] = ['depositar', 'sacar', 'transferir'];

  valor = signal('');
  observacao = signal('');
  contaDestino = signal('');

  ngOnInit(): void {
    this.queryParamsSub = this.route.queryParams.subscribe((params) => {
      const tab = params['tab'];
      if (tab === 'depositar' || tab === 'sacar' || tab === 'transferir') {
        this.activeTab.set(tab);
      }
    });
  }

  ngOnDestroy(): void {
    this.queryParamsSub?.unsubscribe();
  }

  setTab(tab: Tab): void {
    this.activeTab.set(tab);
    this.error.set('');
    this.comprovante.set(null);
    this.valor.set('');
    this.observacao.set('');
    this.contaDestino.set('');
  }

  novo(): void {
    this.comprovante.set(null);
    this.valor.set('');
    this.observacao.set('');
    this.contaDestino.set('');
    this.error.set('');
  }

  onSubmit(): void {
    this.error.set('');

    const valorNum = parseFloat(
      this.valor().replace(/\./g, '').replace(',', '.')
    );

    if (isNaN(valorNum) || valorNum < 0.01) {
      this.error.set('Informe um valor válido maior que zero.');
      return;
    }

    if (this.activeTab() === 'transferir' && !this.contaDestino()) {
      this.error.set('Informe a conta destino.');
      return;
    }

    this.loading.set(true);

    const obs = this.observacao().trim() || undefined;

    if (this.activeTab() === 'depositar') {
      this.contaService.depositar({ valor: valorNum, observacao: obs }).subscribe(
        this.handleResult()
      );
    } else if (this.activeTab() === 'sacar') {
      this.contaService.sacar({ valor: valorNum, observacao: obs }).subscribe(
        this.handleResult()
      );
    } else {
      this.contaService
        .transferir({ valor: valorNum, observacao: obs, contaDestino: this.contaDestino() })
        .subscribe(this.handleResult());
    }
  }

  private handleResult() {
    return {
      next: (res: TransacaoResponse) => {
        this.comprovante.set(res);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(err.error?.message || err.error || 'Erro ao processar transação.');
      },
    };
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

  tabLabel(tab: Tab): string {
    const map: Record<Tab, string> = {
      depositar: 'Depositar',
      sacar: 'Sacar',
      transferir: 'Transferir',
    };
    return map[tab];
  }

  descricaoLabel(desc: string): string {
    const map: Record<string, string> = {
      DEPOSITO: 'Depósito',
      SAQUE: 'Saque',
      TED: 'Transferência',
    };
    return map[desc] || desc;
  }
}
