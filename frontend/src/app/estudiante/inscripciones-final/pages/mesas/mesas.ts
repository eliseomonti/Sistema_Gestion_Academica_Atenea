import { Component, computed, inject, signal } from '@angular/core';
import { InscripcionFinalApi } from '../../data/inscripciones-final-api';
import { rxResource } from '@angular/core/rxjs-interop';
import { firstValueFrom } from 'rxjs';
import { OfertaFinalResponse } from '../../data/inscripcion-final';
import { HttpErrorResponse } from '@angular/common/http';
import { Icono } from '../../../../shared/ui/icono/icono';
import { Aviso } from '../../../../shared/ui/aviso/aviso';

const DIAS = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'];

/** Las mesas de una materia, para mostrarlas en una sola tarjeta. */
interface MateriaConMesas {
  materiaId: number;
  nombreMateria: string;
  mesas: OfertaFinalResponse[];
}

@Component({
  imports: [Icono, Aviso],
  selector: 'app-mesas',
  styleUrl: './mesas.css',
  templateUrl: './mesas.html',
})
export class Mesas {

  private readonly api = inject(InscripcionFinalApi);
  protected readonly oferta = rxResource({ stream: () => this.api.oferta()})

  protected readonly mesas = computed(() => 
    [...(this.oferta.value() ?? [])].sort((a,b) => (a.fecha + a.horaInicio).localeCompare(b.fecha + b.horaInicio))
  )

  /** Una tarjeta por materia, en el orden de su primera mesa. */
  protected readonly materias = computed<MateriaConMesas[]>(() => {
    const porMateria = new Map<number, MateriaConMesas>();
    for (const mesa of this.mesas()) {
      const materia = porMateria.get(mesa.materiaId)
        ?? { materiaId: mesa.materiaId, nombreMateria: mesa.nombreMateria, mesas: [] };
      materia.mesas.push(mesa);
      porMateria.set(mesa.materiaId, materia);
    }
    return [...porMateria.values()];
  })


  protected readonly inscribiendo = signal<number | null>(null)
  /** El error de la última inscripción, que se muestra en la fila de esa mesa. */
  protected readonly aviso = signal<{examenFinalId: number; texto: string; esError: boolean} | null>(null)
  /** El éxito se avisa con un cartel flotante. */
  protected readonly exito = signal<string | null>(null)

  /** "Jue 20/11/2026" */
  protected dia(mesa: OfertaFinalResponse): string
  {
    const [anio, mes, dia] = mesa.fecha.slice(0, 10).split('-');
    const diaSemana = new Date(Number(anio), Number(mes) - 1, Number(dia)).getDay();
    return `${DIAS[diaSemana]} ${dia}/${mes}/${anio}`;
  }

  /** "08:30 a 10:30" */
  protected horas(mesa: OfertaFinalResponse): string
  {
    return `${mesa.horaInicio.slice(0, 5)} a ${mesa.horaFin.slice(0, 5)}`;
  }

   protected docentes(mesa: OfertaFinalResponse): string
  {
    if (mesa.docentes.length === 0) {
      return 'Tribunal a confirmar';
    }
    return mesa.docentes.map((d) => `${d.nombre.charAt(0)}. ${d.apellido}`).join(', ');
  }


  protected async inscribir(mesa: OfertaFinalResponse): Promise<void>
  {
    const id = mesa.examenFinalId;
    this.aviso.set(null);
    this.exito.set(null);
    this.inscribiendo.set(id);
    try 
    {
      await firstValueFrom(this.api.inscribirte({ examenFinalId: id }));
      this.exito.set('Te inscribiste a la mesa de ' + mesa.nombreMateria);
      this.oferta.reload();
    }catch(error)
    {
      this.aviso.set({ examenFinalId: id, texto: mensajeDeError(error), esError: true });
      if(error instanceof HttpErrorResponse && error.status === 409)
      {
        this.oferta.reload();
      }
    }
    this.inscribiendo.set(null);
  }

}

function mensajeDeError(error: unknown): string
{
  if (error instanceof HttpErrorResponse && error.status !== 0 && error.status !== 500) {
    return error.error?.errores?.[0]?.mensaje ?? error.error?.detail ?? 'Ocurrió un error';
  }
  return 'Ocurrió un error';
}