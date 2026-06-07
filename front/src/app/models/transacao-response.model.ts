export interface TransacaoResponse {
  valor: number;
  descricao: 'DEPOSITO' | 'SAQUE' | 'TED';
  observacao: string | null;
  dataTransacao: string;
  saldoAntes: number;
  saldoDepois: number;
}
