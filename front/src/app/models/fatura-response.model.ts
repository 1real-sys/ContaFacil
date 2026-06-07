import { CompraCartaoResponse } from './compra-cartao.model';

export interface FaturaResponse {
  id: number;
  ano: number;
  mes: number;
  dataFechamento: string;
  dataVencimento: string;
  valorTotal: number;
  valorPago: number;
  valorPendente: number;
  status: 'ABERTA' | 'FECHADA' | 'PAGA' | 'ATRASADA';
  compras: CompraCartaoResponse[];
}
