import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserPerfil } from '../../models/user-perfil.model';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/user`;

  perfil(): Observable<UserPerfil> {
    return this.http.get<UserPerfil>(`${this.baseUrl}/perfil`);
  }

  atualizarNome(username: string): Observable<UserPerfil> {
    return this.http.patch<UserPerfil>(`${this.baseUrl}/atualizarNome`, { username });
  }
}
