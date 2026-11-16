import { HttpClient, HttpParams } from "@angular/common/http";
import { inject, Service } from "@angular/core";
import { API_URL } from "../../../core/http/api-url";
import { Observable } from "rxjs";
import { AltaExamenFinalRequest, ExamenFinalResponse, ModificacionExamenFinalRequest } from "./examen-final";

@Service()
export class ExamenesFinalesApi{

    private readonly http = inject(HttpClient)
    private readonly url = `${API_URL}/admin/examenes-finales`

    listar(periodoId: number): Observable<ExamenFinalResponse[]>
    {
        const params = new HttpParams().set('periodoId', periodoId)
        return this.http.get<ExamenFinalResponse[]>(this.url, { params });
    }

    obtener(examenFinalId: number): Observable<ExamenFinalResponse>
    {
        return this.http.get<ExamenFinalResponse>(`${this.url}/${examenFinalId}`);
    }

    alta(request: AltaExamenFinalRequest): Observable<ExamenFinalResponse>
    {
        return this.http.post<ExamenFinalResponse>(this.url, request);
    }

    modificar(examenFinalId: number, request: ModificacionExamenFinalRequest): Observable<ExamenFinalResponse>
    {
        return this.http.put<ExamenFinalResponse>(`${this.url}/${examenFinalId}`, request);
    }

    eliminar(examenFinalId: number): Observable<void>
    {
        return this.http.delete<void>(`${this.url}/${examenFinalId}`);
    }

}