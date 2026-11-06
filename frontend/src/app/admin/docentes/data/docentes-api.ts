import { HttpClient } from "@angular/common/http";
import { inject, Service } from "@angular/core";
import { API_URL } from "../../../core/http/api-url";

@Service()
export class DocentesApi{
    private readonly http = inject(HttpClient)
    private readonly url = `${API_URL}/admin/docentes`

    
}