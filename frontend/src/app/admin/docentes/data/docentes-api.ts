import { HttpClient, HttpParams } from "@angular/common/http";
import { inject, Service } from "@angular/core";
import { API_URL } from "../../../core/http/api-url";
import { DocenteListadoResponse, DocentesFiltro } from "./docente";
import { Observable } from "rxjs";
import { Pagina } from "../../../shared/models/pagina";
import { AltaPersonaResponse, DatosPersonalesRequest, PersonaResponse } from "../../personas/data/persona";

@Service()
export class DocentesApi{
    private readonly http = inject(HttpClient)
    private readonly url = `${API_URL}/admin/docentes`

    listarDocentes(filtro: DocentesFiltro = {}): Observable<Pagina<DocenteListadoResponse>> {
    let params = new HttpParams();
    for (const [clave, valor] of Object.entries(filtro)) {
      if (valor !== undefined && valor !== '') {
        params = params.set(clave, valor);
      }
    }
    return this.http.get<Pagina<DocenteListadoResponse>>(this.url, { params });
  }

  altaDocente(datos: DatosPersonalesRequest): Observable<AltaPersonaResponse> {
    return this.http.post<AltaPersonaResponse>(this.url, datos);
  }

  asignarRol(personaId: number): Observable<PersonaResponse> {
    return this.http.put<PersonaResponse>(`${this.url}/${personaId}`, null);
  }


}