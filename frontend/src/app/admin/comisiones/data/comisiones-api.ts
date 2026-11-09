import { HttpClient, HttpParams } from "@angular/common/http";
import { inject, Service } from "@angular/core";
import { ComisionesFiltros, ComisionResponse } from "./comision";
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
}