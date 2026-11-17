import { Component, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { NotasApi } from '../../data/notas-api';
import { FinalEstudianteResponse } from '../../data/notas';
import { Etiqueta } from '../../../../shared/ui/etiqueta/etiqueta';
import { Celda } from '../../../../shared/ui/tabla/celda';
import { Columna } from '../../../../shared/ui/tabla/columna';
import { Tabla } from '../../../../shared/ui/tabla/tabla';

/** "2026-04-20" → "20/04/2026" */
function fecha(iso: string): string {
  return iso.slice(0, 10).split('-').reverse().join('/');
}

/** Los finales rendidos en una tabla, con el mismo estilo que Cursadas. Es la pestaña Finales de Mis notas. */
@Component({
  imports: [Etiqueta, Tabla, Celda],
  selector: 'app-notas-finales',
  styleUrl: './notas-finales.css',
  templateUrl: './notas-finales.html',
})
export class NotasFinales {

  private readonly api = inject(NotasApi)
  protected readonly finales = rxResource({stream: () => this.api.listarFinales()})

  protected readonly columnas: Columna<FinalEstudianteResponse>[] = [
    { clave: 'nombreMateria', titulo: 'Materia', principal: true },
    { clave: 'fecha', titulo: 'Fecha', valor: (final) => fecha(final.fecha) },
    { clave: 'nota', titulo: 'Nota', tituloEnLista: true }, // <ng-template appCelda>
    { clave: 'estado', titulo: 'Estado' }, // <ng-template appCelda>
  ];

  protected notaFinal(final: FinalEstudianteResponse): string
  {
    return final.nota !== null ? String(final.nota) : '—';
  }

  /** Aprobado en la etiqueta azul; lo demás, en texto gris, sin rojo. */
  protected estadoFinal(final: FinalEstudianteResponse): {texto: string; tipo: 'info' | null}
  {
    if(final.aprobado === true)
    {
      return {texto: 'Aprobado', tipo: 'info'};
    }
    if(final.condicion === 'AUSENTE')
    {
      return {texto: 'Ausente', tipo: null};
    }
    if(final.aprobado === false)
    {
      return {texto: 'Desaprobado', tipo: null};
    }
    return {texto: 'Sin nota', tipo: null};
  }
}
