export interface CartaoResponse {
  id: number;
  numeroCartaoOculto: string;
  status: 'ATIVO' | 'INATIVO' | 'BLOQUEADO' | 'CANCELADO';
  dataValidade: string;
  limiteTotal: number;
  limiteUtilizado: number;
  limiteDisponivel: number;
  bandeira: 'VISA' | 'MASTERCARD';
}
