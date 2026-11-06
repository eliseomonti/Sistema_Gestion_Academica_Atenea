import { HttpClient } from "@angular/common/http";
import { inject, Service } from "@angular/core";
import { API_URL } from "../../../core/http/api-url";
import { Observable } from "rxjs";
import { PersonaResponse } from "./persona";

@Service()
export class PersonasApi{
    private readonly http = inject(HttpClient);
  private readonly url = `${API_URL}/admin/personas`;


  buscarPorDni(dni: string): Observable<PersonaResponse> {
    return this.http.get<PersonaResponse>(`${this.url}/dni/${dni}`);
  }
}