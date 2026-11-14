import { HttpClient } from "@angular/common/http";
import { inject, Service } from "@angular/core";
import { API_URL } from "../../../core/http/api-url";
import { Observable } from "rxjs";
import { CursadaEstudianteResponse, FinalEstudianteResponse } from "./notas";

@Service()
export class NotasApi{

    private readonly http = inject(HttpClient)
    private readonly url = `${API_URL}/estudiante/notas`

    listarCursada(): Observable<CursadaEstudianteResponse[]>
    {
        return this.http.get<CursadaEstudianteResponse[]>(`${this.url}/cursadas`)
    }

    listarFinales(): Observable<FinalEstudianteResponse[]>
    {
        return this.http.get<FinalEstudianteResponse[]>(`${this.url}/finales`)
    }
}