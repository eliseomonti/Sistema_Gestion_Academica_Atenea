import { HttpClient } from "@angular/common/http";
import { inject, Service } from "@angular/core";
import { API_URL } from "../../../core/http/api-url";
import { Observable } from "rxjs";
import { InscripcionFinalResponse } from "./inscripcion-final";

@Service()
export class InscripcionFinalApi{

    private readonly http = inject(HttpClient)
    private readonly url = `${API_URL}/estudiante/inscripciones-final`

    listar(): Observable<InscripcionFinalResponse[]>
    {
        return this.http.get<InscripcionFinalResponse[]>(this.url)
    }
}