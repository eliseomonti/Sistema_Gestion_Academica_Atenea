import { afterEveryRender, afterNextRender, Component, ElementRef, inject, input, signal, viewChild } from '@angular/core';
import { PersonasApi } from '../../data/personas-api';
import { ActivatedRoute, Router } from '@angular/router';
import { rxResource } from '@angular/core/rxjs-interop';
import { firstValueFrom } from 'rxjs';
import { DatosPersonalesRequest } from '../../data/persona';
import { HttpErrorResponse } from '@angular/common/http';
import { DatosPersonalesForm } from '../../components/datos-personales-form/datos-personales-form';

@Component({
  imports: [DatosPersonalesForm],
  selector: 'app-persona-edicion',
  styleUrl: './persona-edicion.css',
  templateUrl: './persona-edicion.html',
})
export class PersonaEdicion {

  private readonly personasApi = inject(PersonasApi);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);

  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');
  readonly personaId = input.required<string>();

  protected readonly guardado = signal(false)
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly persona = rxResource({
    params: () => Number(this.personaId()),
    stream: ({params}) => this.personasApi.obtener(params)
  }
  )

  constructor()
  {
    afterNextRender (() => this.dialogo().nativeElement.showModal());
  }
  
  protected async guardar(datos: DatosPersonalesRequest): Promise<void> {
    this.error.set(null);
    this.cargando.set(true);
    try{
      await firstValueFrom(this.personasApi.modificar(Number(this.personaId()),datos))
      this.guardado.set(true);
    }catch (error)
    {
      this.error.set(mensajeDeError(error))
    }
    this.cargando.set(false);
  
  }


  protected cerrar(): void
  {
    this.dialogo().nativeElement.close();
  }

  protected volver(): void
  {
    void this.router.navigate(['..'], {relativeTo: this.ruta});
  }

}

 function mensajeDeError (error: unknown): string
  {
    if(error instanceof HttpErrorResponse && error.status != 0 && error.status !== 500)
    {
      return error.error.errores?.[0].mensaje ?? error.error.detail ?? 'Ocurrio un error'
    }
    return 'ocurrio un error'
    
  }