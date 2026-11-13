import { HttpErrorResponse } from '@angular/common/http';
import { afterNextRender, Component, ElementRef, inject, input, linkedSignal, signal, viewChild } from '@angular/core';
import { ComisionesAPi } from '../../data/comisiones-api';
import { ActivatedRoute, Router } from '@angular/router';
import { Turno } from '../../../../shared/models/turno';
import { rxResource } from '@angular/core/rxjs-interop';
import { form, FormField, FormRoot, min, required } from '@angular/forms/signals';
import { firstValueFrom } from 'rxjs';
import { TurnoPipe } from '../../../../shared/pipes/turno-pipe';
import { ConfirmarDialog } from '../../../../shared/ui/confirmar-dialog/confirmar-dialog';
import { Icono } from '../../../../shared/ui/icono/icono';


@Component({
  imports: [FormRoot, FormField, TurnoPipe, ConfirmarDialog, Icono],
  selector: 'app-comision-edicion',
  styleUrl: './comision-edicion.css',
  templateUrl: './comision-edicion.html',
})
export class ComisionEdicion {

  private readonly api = inject(ComisionesAPi);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);
  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo')
  readonly comisionId = input.required<string>()
  
  protected readonly turnos: Turno[] = ['MANANA', 'MEDIA_MANANA', 'TARDE', 'NOCHE']

  protected readonly mensaje = signal<string | null>(null)
  protected readonly confirmarBorrado = signal(false)
  protected readonly confirmarBaja = signal(false)
  // Si se borró, al cerrar se vuelve a la lista: el detalle de esta comisión ya no existe.
  private readonly borrada = signal(false)
  protected readonly cargando = signal(false)
  protected readonly error = signal<string | null>(null)

  protected readonly comision = rxResource({
    params: () => Number(this.comisionId()),
    stream: ({params}) => this.api.obtenerComision(params)
  })

  private readonly datos = linkedSignal(() => {
    const comision = this.comision.value();
    return {
      nroComision: comision?.nroComision ?? 1,
      turno: comision?.turno ?? '',
    };
  });

  protected readonly comisionForm = form(
    this.datos, (campo) => {
      required(campo.turno, { message: 'Elegi el turno'});
      min(campo.nroComision, 1, {message: 'El numero tiene que ser mayor a 0'});
    },
    {
      submission: {
        action: async () => {
          await this.guardar();
          return undefined;
        },
      }
    } 
  )

  constructor()
  {
    afterNextRender(() => this.dialogo().nativeElement.showModal())
  }

  private async guardar(): Promise<void>
  {
    this.error.set(null);
    this.cargando.set(true);
    try {
      await firstValueFrom(this.api.modificarComision(Number(this.comisionId()), {
        nroComision: this.datos().nroComision,
        turno: this.datos().turno as Turno,
      }));
      this.mensaje.set('Se guardo el cambio');
    } catch (error) {
      this.error.set(mensajeDeError(error));
    }
    this.cargando.set(false);
  }

  protected async borrar(): Promise<void>
  {
    this.error.set(null)
    this.cargando.set(true)
    try
    {
      await firstValueFrom(this.api.borrarComision(Number(this.comisionId())));
      this.borrada.set(true)
      this.mensaje.set('Se borro la comision')
    }catch(error)
    {
      if (error instanceof HttpErrorResponse && error.status === 409)
      {
        this.error.set('La comisión tiene un historial y no se puede borrar, podes darla de baja')
        this.confirmarBaja.set(true)
      }else
      {
        this.error.set(mensajeDeError(error))
      }
    }

    this.confirmarBorrado.set(false)
    this.cargando.set(false)
  }

  protected async darDeBaja(): Promise<void>
  {
    this.error.set(null);
    this.cargando.set(true);
    try
    {
      await firstValueFrom(this.api.darDeBajaComision(Number(this.comisionId())))
      this.mensaje.set('Se dio de baja la comision')
    }catch(error)
    {
      this.error.set(mensajeDeError(error))
    }

    this.confirmarBaja.set(false)
    this.cargando.set(false)

  }



  /** "1.er", "2.º", "3.er"...: así se abrevian los ordinales en castellano. */
  protected ordinal(numero: number): string
  {
    return numero === 1 || numero === 3 ? `${numero}.er` : `${numero}.º`;
  }

  protected cerrar(): void
  {
    this.dialogo().nativeElement.close();
  }

  protected volver(): void
  {
    // La ventana está en comisiones/:comisionId/editar: ".." es el detalle y "../.." la lista.
    void this.router.navigate([this.borrada() ? '../..' : '..'], { relativeTo: this.ruta });
  }

}


function mensajeDeError(error: unknown): string
{
  if (error instanceof HttpErrorResponse && error.status !== 0 && error.status !== 500) {
    return error.error?.errores?.[0]?.mensaje ?? error.error?.detail ?? 'Ocurrio un error';
  }
  return 'ocurrio un error'
}
