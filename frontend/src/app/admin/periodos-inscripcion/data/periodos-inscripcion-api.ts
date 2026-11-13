import { HttpClient, HttpParams } from "@angular/common/http";
import { inject, Service } from "@angular/core";
import { API_URL } from "../../../core/http/api-url";
import { AltaPeriodoRequest, ModificacionPeriodoRequest, PeriodoInscripcionResponse, PeriodosFiltro } from "./periodo-inscripcion";
import { Observable } from "rxjs";


@Service()
export class PeriodoInscripcionApi{
    private readonly http = inject(HttpClient);
    private readonly url = `${API_URL}/admin/periodos-inscripcion`;


    listar(filto: PeriodosFiltro): Observable<PeriodoInscripcionResponse[]>
    {
        let params = new HttpParams()
        for(const[clave,valor] of Object.entries(filto))
        {
            if(valor !== undefined && valor !== '')
            {
                params = params.set(clave,valor)
            }
        }
        return this.http.get<PeriodoInscripcionResponse[]>(this.url, {params})
    }

    obtenerPeriodo(periodoId: number): Observable<PeriodoInscripcionResponse>
    {
        return this.http.get<PeriodoInscripcionResponse>(`${this.url}/${periodoId}`)
    }

    altaPeriodo(request: AltaPeriodoRequest): Observable<PeriodoInscripcionResponse>
    {
        return this.http.post<PeriodoInscripcionResponse>(`${this.url}`, request)
    }

    modificarPeriodo(periodoId: number, request: ModificacionPeriodoRequest): Observable<PeriodoInscripcionResponse>
    {
        return this.http.put<PeriodoInscripcionResponse>(`${this.url}/${periodoId}`, request)
    }

    habilitar(periodoId: number): Observable<PeriodoInscripcionResponse>
    {
        return this.http.post<PeriodoInscripcionResponse>(`${this.url}/${periodoId}/habilitar`,null)
    }

    deshabilitar(periodoId: number): Observable<PeriodoInscripcionResponse>
    {
        return this.http.post<PeriodoInscripcionResponse>(`${this.url}/${periodoId}/deshabilitar`,null)
    }

    borrarPeriodo(periodoId: number): Observable<void>
    {
        return this.http.delete<void>(`${this.url}/${periodoId}`);
    }




}