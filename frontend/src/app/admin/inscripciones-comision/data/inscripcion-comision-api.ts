import { HttpClient, HttpParams } from "@angular/common/http";
import { inject, Service } from "@angular/core";
import { API_URL } from "../../../core/http/api-url";
import { Observable } from "rxjs";
import { InscriptoComisionResponse } from "./inscripcion-comision";

@Service()
export class InscripcionesComisionApi{
    private readonly http = inject(HttpClient)
    private readonly url = `${API_URL}/admin/inscripciones-comision`

    listar(comisionMateriaId: number): Observable<InscriptoComisionResponse[]>
    {
        const params = new HttpParams().set('comisionMateriaId', comisionMateriaId);
        return this.http.get<InscriptoComisionResponse[]>(this.url, {params})
    }

    darDeBaja(inscripcionId: number): Observable<void>
    {
        return this.http.post<void>(`${this.url}/${inscripcionId}/baja`, null)
    }
}