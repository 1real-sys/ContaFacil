export interface DepositoRequest {
  valor: number;
  observacao?: string;
}

export interface SaqueRequest {
  valor: number;
  observacao?: string;
}

export interface TedRequest {
  valor: number;
  observacao?: string;
  contaDestino: string;
}
