import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ContaResponse } from '../../models/conta-response.model';
import { SaldoResponse } from '../../models/saldo-response.model';
import { ExtratoResponse } from '../../models/extrato-response.model';
import { DepositoRequest, SaqueRequest, TedRequest } from '../../models/transacao-request.model';
import { TransacaoResponse } from '../../models/transacao-response.model';
import { CartaoResponse } from '../../models/cartao-response.model';
import { CartaoDadosSensiveis } from '../../models/cartao-dados-sensiveis.model';
import { CompraCartaoRequest, CompraCartaoResponse } from '../../models/compra-cartao.model';
import { FaturaResponse } from '../../models/fatura-response.model';

@Injectable({ providedIn: 'root' })
export class ContaService {
  private readonly http = inject(HttpClient);

  private readonly contaUrl = `${environment.apiUrl}/conta`;
  private readonly transacaoUrl = `${environment.apiUrl}/transacao`;
  private readonly cartaoUrl = `${environment.apiUrl}/cartao`;
  private readonly comprasUrl = `${environment.apiUrl}/compras`;
  private readonly faturasUrl = `${environment.apiUrl}/faturas`;

  abrirConta(): Observable<ContaResponse> {
    return this.http.post<ContaResponse>(`${this.contaUrl}/abrirConta`, {});
  }

  minhaConta(): Observable<ContaResponse> {
    return this.http.get<ContaResponse>(`${this.contaUrl}/minhaConta`);
  }

  saldo(): Observable<SaldoResponse> {
    return this.http.get<SaldoResponse>(`${this.contaUrl}/saldo`);
  }

  extrato(dataInicio: string, dataFim: string, tipo?: string): Observable<ExtratoResponse> {
    let url = `${this.contaUrl}/extrato?dataInicio=${dataInicio}&dataFim=${dataFim}`;
    if (tipo) url += `&tipo=${tipo}`;
    return this.http.get<ExtratoResponse>(url);
  }

  encerrarConta(): Observable<string> {
    return this.http.delete(`${this.contaUrl}/encerrar`, { responseType: 'text' });
  }

  depositar(data: DepositoRequest): Observable<TransacaoResponse> {
    return this.http.post<TransacaoResponse>(`${this.transacaoUrl}/depositar`, data);
  }

  sacar(data: SaqueRequest): Observable<TransacaoResponse> {
    return this.http.post<TransacaoResponse>(`${this.transacaoUrl}/saque`, data);
  }

  transferir(data: TedRequest): Observable<TransacaoResponse> {
    return this.http.post<TransacaoResponse>(`${this.transacaoUrl}/ted`, data);
  }

  meusCartoes(): Observable<CartaoResponse[]> {
    return this.http.get<CartaoResponse[]>(`${this.cartaoUrl}/meusCartoes`);
  }

  emitirCartao(bandeira: string): Observable<CartaoResponse> {
    return this.http.post<CartaoResponse>(`${this.cartaoUrl}/emitirCartao`, { bandeira });
  }

  solicitarLimite(cartaoId: number): Observable<CartaoResponse> {
    return this.http.post<CartaoResponse>(`${this.cartaoUrl}/${cartaoId}/solicitarLimite`, {});
  }

  desbloquearCartao(cartaoId: number): Observable<CartaoResponse> {
    return this.http.post<CartaoResponse>(`${this.cartaoUrl}/${cartaoId}/desbloquearCartao`, {});
  }

  bloquearCartao(cartaoId: number): Observable<CartaoResponse> {
    return this.http.post<CartaoResponse>(`${this.cartaoUrl}/${cartaoId}/bloquearCartao`, {});
  }

  cancelarCartao(cartaoId: number): Observable<string> {
    return this.http.patch(`${this.cartaoUrl}/${cartaoId}/cancelarCartao`, {}, { responseType: 'text' });
  }

  dadosSensiveis(cartaoId: number): Observable<CartaoDadosSensiveis> {
    return this.http.get<CartaoDadosSensiveis>(`${this.cartaoUrl}/${cartaoId}/dadosSensiveis`);
  }

  lancarCompra(cartaoId: number, data: CompraCartaoRequest): Observable<CompraCartaoResponse> {
    return this.http.post<CompraCartaoResponse>(`${this.comprasUrl}/${cartaoId}/lancar`, data);
  }

  cancelarCompra(compraId: number): Observable<CompraCartaoResponse> {
    return this.http.patch<CompraCartaoResponse>(`${this.comprasUrl}/${compraId}/cancelar`, {});
  }

  estornarCompra(compraId: number): Observable<CompraCartaoResponse> {
    return this.http.patch<CompraCartaoResponse>(`${this.comprasUrl}/${compraId}/estornar`, {});
  }

  faturaAtual(cartaoId: number): Observable<FaturaResponse> {
    return this.http.get<FaturaResponse>(`${this.faturasUrl}/${cartaoId}/atual`);
  }

  historicoFaturas(cartaoId: number): Observable<FaturaResponse[]> {
    return this.http.get<FaturaResponse[]>(`${this.faturasUrl}/${cartaoId}/historico`);
  }

  pagarFatura(faturaId: number, valor: number): Observable<FaturaResponse> {
    return this.http.post<FaturaResponse>(`${this.faturasUrl}/${faturaId}/pagar`, { valor });
  }
}
