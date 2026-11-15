import { HttpClient } from "@angular/common/http";
import { inject, Service } from "@angular/core";
import { API_URL } from "../../../core/http/api-url";
import { Observable } from "rxjs";
import { InscripcionFinalRequest, InscripcionFinalResponse, OfertaFinalResponse } from "./inscripcion-final";

@Service()
export class InscripcionFinalApi{

    private readonly http = inject(HttpClient)
    private readonly url = `${API_URL}/estudiante/inscripciones-final`

    listar(): Observable<InscripcionFinalResponse[]>
    {
        return this.http.get<InscripcionFinalResponse[]>(this.url)
    }

    oferta(): Observable<OfertaFinalResponse[]>
    {
        return this.http.get<OfertaFinalResponse[]>(`${this.url}/oferta`)
    }
    
    inscribirte(request: InscripcionFinalRequest): Observable<InscripcionFinalResponse>
    {
        return this.http.post<InscripcionFinalResponse>(this.url, request)
    }

    darDeBaja(inscripcionId: number): Observable<InscripcionFinalResponse>
    {
        return this.http.post<InscripcionFinalResponse>(`${this.url}/${inscripcionId}/baja`, null);
    }
}