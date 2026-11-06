import { Component, inject, signal } from '@angular/core';
import { PersonasApi } from '../../../personas/data/personas-api';
import { DocentesApi } from '../../data/docentes-api';
import { Router } from '@angular/router';
import { AltaPersonaResponse, DatosPersonalesRequest, PersonaResponse } from '../../../personas/data/persona';
import { firstValueFrom } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { DniPaso } from '../../components/dni-paso/dni-paso';
import { DatosPersonalesForm } from '../../components/datos-personales-form/datos-personales-form';
import { CredencialesDialogo } from '../../components/credenciales-dialogo/credenciales-dialogo';

@Component({
  imports: [DniPaso, DatosPersonalesForm, CredencialesDialogo],
  selector: 'app-docente-alta',
  styleUrl: './docente-alta.css',
  templateUrl: './docente-alta.html',
})
export class DocenteAlta {

  private readonly personasApi = inject(PersonasApi);
  private readonly docentesApi = inject(DocentesApi);
  private readonly router = inject(Router);

  protected readonly dniNuevo = signal<string | null>(null);
  protected readonly personaExistente = signal<PersonaResponse | null>(null);
  protected readonly credenciales = signal<AltaPersonaResponse | null>(null);
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);


  protected async buscar(dni: string): Promise<void> {
    this.error.set(null);
    this.personaExistente.set(null);
    this.cargando.set(true);
    try {
      this.personaExistente.set(await firstValueFrom(this.personasApi.buscarPorDni(dni)));
    } catch (error) {
      if (error instanceof HttpErrorResponse && error.status === 404) {
        this.dniNuevo.set(dni);
      } else {
        this.error.set(mensajeDeError(error));
      }
    }
    this.cargando.set(false);
  }

  protected async guardar(datos: DatosPersonalesRequest): Promise<void> {
    this.error.set(null);
    this.cargando.set(true);
    try {
      this.credenciales.set(await firstValueFrom(this.docentesApi.altaDocente(datos)));
    } catch (error) {
      this.error.set(mensajeDeError(error));
    }
    this.cargando.set(false);
  }

  protected async asignarRol(personaId: number): Promise<void> {
  this.error.set(null);
  this.cargando.set(true);
  try {
    await firstValueFrom(this.docentesApi.asignarRol(personaId));
    this.volverAlListado();
  } catch (error) {
    this.error.set(mensajeDeError(error));
  }
  this.cargando.set(false);
}

  protected volverAlListado(): void {
    this.router.navigate(['/admin/docentes']);
  }

}


function mensajeDeError(error: unknown): string {
  if (error instanceof HttpErrorResponse && error.status !== 0 && error.status !== 500) {
    return error.error?.errores?.[0]?.mensaje ?? error.error?.detail;
  }
return 'Ocurrio un error';
}