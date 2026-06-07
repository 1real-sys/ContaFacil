export interface ExtratoItem {
  transacaoId: number;
  dataTransacao: string;
  tipo: 'DEPOSITO' | 'SAQUE' | 'TED';
  valor: number;
  saldoAntes: number;
  saldoDepois: number;
  observacao: string | null;
}

export interface ExtratoResponse {
  contaId: number;
  contaCorrente: string;
  agencia: string;
  dataInicio: string;
  dataFim: string;
  saldoInicialPeriodo: number;
  saldoFinalPeriodo: number;
  itens: ExtratoItem[];
}
