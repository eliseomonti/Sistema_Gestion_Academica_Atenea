import { HttpClient, HttpParams } from "@angular/common/http";
import { inject, Service } from "@angular/core";
import {
    AltaComisionMateriaRequest,
    AltaComisionRequest,
    ComisionDetalleResponse,
    ComisionesFiltros,
    ComisionMateriaResponse,
    ComisionResponse,
    ModificacionComisionMateriaRequest,
    ModificacionComisionRequest,
} from "./comision";
import { Observable } from "rxjs";
import { API_URL } from "../../../core/http/api-url";

@Service()
export class ComisionesAPi{
    private readonly http = inject(HttpClient);
    private readonly url = `${API_URL}/admin/comisiones`

    listar(filtro: ComisionesFiltros): Observable<ComisionResponse[]>
    {
        let params = new HttpParams()
        for(const [clave,valor] of Object.entries(filtro))
        {
            if(valor !== undefined && valor !== '')
            {
                params = params.set(clave,valor);
            }
        }

        return this.http.get<ComisionResponse[]>(this.url, {params});
    }

    altaComision(request: AltaComisionRequest): Observable<ComisionResponse>
    {
        return this.http.post<ComisionResponse>(this.url,request);
    }

    modificarComision(comisionId: number, request: ModificacionComisionRequest): Observable<ComisionResponse>
    {
        return this.http.put<ComisionResponse>(`${this.url}/${comisionId}`, request);
    }

    borrarComision(comisionId: number): Observable<void>
    {
        return this.http.delete<void>(`${this.url}/${comisionId}`);
    }

    darDeBajaComision(comisionId: number): Observable<ComisionResponse>
    {
        return this.http.post<ComisionResponse>(`${this.url}/${comisionId}/baja`,null);
    }

    obtenerComision(comisionId: number): Observable<ComisionDetalleResponse>
    {
        return this.http.get<ComisionDetalleResponse>(`${this.url}/${comisionId}`)
    }

    // Las materias de la comisión se leen con obtenerComision: estos endpoints sólo las modifican.

    agregarMateria(comisionId: number, request: AltaComisionMateriaRequest): Observable<ComisionMateriaResponse>
    {
        return this.http.post<ComisionMateriaResponse>(`${this.url}/${comisionId}/materias`, request);
    }

    modificarMateria(comisionId: number, comisionMateriaId: number,
                     request: ModificacionComisionMateriaRequest): Observable<ComisionMateriaResponse>
    {
        return this.http.put<ComisionMateriaResponse>(
            `${this.url}/${comisionId}/materias/${comisionMateriaId}`, request);
    }

    /** Si la materia ya tiene inscripciones, evaluaciones o encuestas, el backend responde 409. */
    quitarMateria(comisionId: number, comisionMateriaId: number): Observable<void>
    {
        return this.http.delete<void>(`${this.url}/${comisionId}/materias/${comisionMateriaId}`);
    }
}