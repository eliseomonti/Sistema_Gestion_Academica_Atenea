import { afterNextRender, Component, computed, DestroyRef, ElementRef, inject, input, linkedSignal, numberAttribute, signal, viewChild } from '@angular/core';
import { ExamenFinalResponse } from '../../data/examen-final';
import { rxResource } from '@angular/core/rxjs-interop';
import { CarrerasApi } from '../../../carreras/data/carreras-api';
import { ExamenesFinalesApi } from '../../data/examen-final-api';
import { AulasApi } from '../../../aulas/data/aulas-api';
import { DocentesApi } from '../../../docentes/data/docentes-api';
import { ActivatedRoute, Router } from '@angular/router';
import { firstValueFrom, map } from 'rxjs';
import { form, FormField, FormRoot, required } from '@angular/forms/signals';
import { DocenteListadoResponse } from '../../../docentes/data/docente';
import { HttpErrorResponse } from '@angular/common/http';
import { ConfirmarDialog } from '../../../../shared/ui/confirmar-dialog/confirmar-dialog';
import { Icono } from '../../../../shared/ui/icono/icono';


function valoresIniciales(mesa: ExamenFinalResponse | undefined)
{
  return {
    materiaId: mesa ? String(mesa.materiaId) : '',
    fecha: mesa?.fecha ?? '',
    horaInicio: mesa?.horaInicio.slice(0, 5) ?? '',
    horaFin: mesa?.horaFin.slice(0, 5) ?? '',
    aulaId: mesa?.aulaId ? String(mesa.aulaId) : '',
    docentes: mesa?.docentes ?? [],
  };
}

@Component({
  imports: [FormRoot, FormField, ConfirmarDialog, Icono],
  selector: 'app-mesa',
  styleUrl: './mesa.css',
  templateUrl: './mesa.html',
})
export class Mesa {

  private readonly api = inject(ExamenesFinalesApi)
  private readonly carrerasApi = inject(CarrerasApi)
  private readonly aulasApi = inject(AulasApi)
  private readonly docentesApi = inject(DocentesApi)
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute)
  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');

  readonly carreraId = input.required({ transform: numberAttribute })
  readonly planId = input.required({ transform: numberAttribute })
  readonly periodoId = input.required({ transform: numberAttribute });

  readonly examenFinalId = input<number | undefined, string | undefined>(undefined, {
    transform: (valor) => (valor === undefined ? undefined : Number(valor))
  });

  protected readonly editando = computed(() => this.examenFinalId() !== undefined);

  protected readonly mensaje = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly cargando = signal(false);
  protected readonly confirmarBorrar = signal(false);

  protected readonly mesa = rxResource({
    params: () => this.examenFinalId(),
    stream: ({ params }) => this.api.obtener(params),
  });

  protected readonly materias = rxResource({
    params: () => ({ carreraId: this.carreraId(), planId: this.planId() }),
    stream: ({ params }) => this.carrerasApi.listarMaterias(params.carreraId, params.planId),
  });

  protected readonly aulas = rxResource({ stream: () => this.aulasApi.listar() })

  private readonly datos = linkedSignal(() => valoresIniciales(this.mesa.value()))

  protected readonly tribunal = computed(() => this.datos().docentes);

  private readonly busqueda = signal('')
  private espera?: ReturnType<typeof setTimeout>;
  private readonly encontrados = rxResource({
    params: () => {
      const texto = this.busqueda().trim()
      return texto.length >= 2 ? texto : undefined;
    },
    stream: ({ params }) =>
      this.docentesApi.listarDocentes({ buscar: params, size: 10 }).pipe(map((pagina) => pagina.content)),
  })


  protected readonly opcionesDocentes = computed(() => {
    const elegidos = this.tribunal().map((d) => d.personaId);
    return (this.encontrados.value() ?? []).filter((d) => !elegidos.includes(d.personaId));
  });

  protected readonly mesaForm = form(
    this.datos,
    (campo) => {
      required(campo.materiaId, { message: 'Elegí la materia' });
      required(campo.fecha, { message: 'Elegí la fecha' });
      required(campo.horaInicio, { message: 'Poné la hora de inicio' });
      required(campo.horaFin, { message: 'Poné la hora de fin' });
    },
    {
      submission: {
        action: async () => {
          await this.guardar();
          return undefined;
        },
      },
    },
  );

  constructor()
  {
    afterNextRender(() => this.dialogo().nativeElement.showModal());
    inject(DestroyRef).onDestroy(() => clearTimeout(this.espera));
  }

  protected buscarDocente(texto: string): void
  {
    clearTimeout(this.espera);
    this.espera = setTimeout(() => this.busqueda.set(texto), 300);
  }

  protected agregarDocente(docente: DocenteListadoResponse): void
  {
    this.datos.update((datos) => ({
      ...datos,
      docentes: [...datos.docentes, { personaId: docente.personaId, apellido: docente.apellido, nombre: docente.nombre }],
    }));
    clearTimeout(this.espera);
    this.busqueda.set('');
  }

  protected quitarDocente(personaId: number): void
  {
    this.datos.update((datos) => ({
      ...datos,
      docentes: datos.docentes.filter((d) => d.personaId !== personaId),
    }));
  }


  private async guardar(): Promise<void>
  {
    this.error.set(null);
    this.cargando.set(true);
    const datos = this.datos();
    const cambios = {
      fecha: datos.fecha,
      horaInicio: datos.horaInicio,
      horaFin: datos.horaFin,
      aulaId: datos.aulaId === '' ? null : Number(datos.aulaId),
      docenteIds: datos.docentes.map((d) => d.personaId),
    };
    const id = this.examenFinalId();
    try {
      if (id === undefined) {
        const mesa = await firstValueFrom(this.api.alta({
          periodoId: this.periodoId(),
          materiaId: Number(datos.materiaId),
          ...cambios,
        }));
        this.mensaje.set(`Se creó la mesa de ${mesa.nombreMateria}`)
      } else {
        const mesa = await firstValueFrom(this.api.modificar(id, cambios));
        this.mensaje.set(`Se guardaron los cambios de ${mesa.nombreMateria}`)
      }
    } catch (error) {
      this.error.set(mensajeDeError(error));
    }
    this.cargando.set(false)
  }

  protected async borrar(): Promise<void>
  {
    const id = this.examenFinalId()
    if (id === undefined) {
      return;
    }
    this.error.set(null);
    this.cargando.set(true);
    try {
      await firstValueFrom(this.api.eliminar(id))
      this.mensaje.set('Se borró la mesa')
    } catch (error) {
      this.error.set(mensajeDeError(error));
    }
    this.confirmarBorrar.set(false);
    this.cargando.set(false);
  }

  protected cerrar(): void
  {
    this.dialogo().nativeElement.close() 
  }

  protected volver(): void
  {
    void this.router.navigate(['..'], { relativeTo: this.ruta })
  }


}

function mensajeDeError(error: unknown): string
{
  if (error instanceof HttpErrorResponse && error.status !== 0 && error.status !== 500) {
    return error.error?.errores?.[0]?.mensaje ?? error.error?.detail ?? 'Ocurrió un error';
  }
  return 'Ocurrió un error';
}
