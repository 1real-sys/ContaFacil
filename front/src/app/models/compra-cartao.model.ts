export interface CompraCartaoRequest {
  valor: number;
  estabelecimento: string;
  categoria: 'RESTAURANTE' | 'MERCADO' | 'POSTODECOMBUSTIVEL' | 'SAUDE' | 'SHOPPING';
}

export interface CompraCartaoResponse {
  id: number;
  valor: number;
  dataCompra: string;
  estabelecimento: string;
  ultimos4Digitos: string;
  categoria: 'RESTAURANTE' | 'MERCADO' | 'POSTODECOMBUSTIVEL' | 'SAUDE' | 'SHOPPING';
  faturaId: number;
  status: 'AUTORIZADA' | 'NEGADA' | 'CANCELADA';
}
