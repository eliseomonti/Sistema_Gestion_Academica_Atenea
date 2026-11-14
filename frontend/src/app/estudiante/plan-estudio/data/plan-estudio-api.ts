import { HttpClient } from "@angular/common/http";
import { inject, Service } from "@angular/core";
import { API_URL } from "../../../core/http/api-url";
import { Observable } from "rxjs";
import { PlanEstudianteResponse } from "./plan-estudio";

@Service()
export class PlanEstudioApi{
    
    private readonly http = inject(HttpClient);
    private readonly url = `${API_URL}/estudiante/plan-estudio`

    listarPlan(): Observable<PlanEstudianteResponse[]>
    {
        return this.http.get<PlanEstudianteResponse[]>(this.url);
    }
}