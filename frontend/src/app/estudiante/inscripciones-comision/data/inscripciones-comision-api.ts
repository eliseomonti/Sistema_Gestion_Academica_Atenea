import { HttpClient, HttpParams } from "@angular/common/http";
import { inject, Service } from "@angular/core";
import { API_URL } from "../../../core/http/api-url";
import { Observable } from "rxjs";
import { InscripcionComisionRequest, InscripcionComisionResponse, OfertaMateriaResponse } from "./inscripcion-comision";

@Service()
export class InscripcionesComisionApi{
    private readonly http = inject(HttpClient);
    private readonly url = `${API_URL}/estudiante/inscripciones-comision`;


    disponibilidad(): Observable<OfertaMateriaResponse[]>
    {
        return this.http.get<OfertaMateriaResponse[]>(`${this.url}/oferta`);
    }

    inscribirse(request: InscripcionComisionRequest): Observable<InscripcionComisionResponse>
    {
        return this.http.post<InscripcionComisionResponse>(this.url, request);
    }

    listarInscripciones(anioLectivo?: number): Observable<InscripcionComisionResponse[]>
    {
        const params = anioLectivo? new HttpParams().set('anioLectivo', anioLectivo): undefined
        return this.http.get<InscripcionComisionResponse[]>(this.url, {params});
    }

    darDeBaja(inscripcionid: number): Observable<InscripcionComisionResponse>
    {
        return this.http.post<InscripcionComisionResponse>(`${this.url}/${inscripcionid}/baja`, null)
    }



}