import { HttpClient } from "@angular/common/http";
import { inject, Service } from "@angular/core";
import { API_URL } from "../../../core/http/api-url";
import { Observable } from "rxjs";
import { DatosPersonalesRequest, PersonaResponse } from "./persona";

@Service()
export class PersonasApi{
    private readonly http = inject(HttpClient);
  private readonly url = `${API_URL}/admin/personas`;


  buscarPorDni(dni: string): Observable<PersonaResponse> {
    return this.http.get<PersonaResponse>(`${this.url}/dni/${dni}`);
  }

  obtener(personaId: number): Observable<PersonaResponse>
  {
    return this.http.get<PersonaResponse>(`${this.url}/${personaId}`);
  }

  modificar(personaId: number, datos: DatosPersonalesRequest): Observable<PersonaResponse>
  {
    return this.http.put<PersonaResponse>(`${this.url}/${personaId}`, datos);
  }

  darDeBajaPersona(personaId: number): Observable<PersonaResponse>
  {
    return this.http.post<PersonaResponse>(`${this.url}/${personaId}/baja`, null);
  }

}