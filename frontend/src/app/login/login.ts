import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, input, signal } from '@angular/core';
import { form, FormField, FormRoot, required } from '@angular/forms/signals';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Rol, rutaInicio } from '../core/seguridad/rol';
import { Sesion } from '../core/seguridad/sesion';
import { Icono } from '../shared/ui/icono/icono';

const TITULOS: Record<Rol, string> = {
  ADMIN: 'Módulo de administración académica',
  DOCENTE: 'Módulo de autogestión de docentes',
  ESTUDIANTE: 'Módulo de autogestión de estudiantes',
};

/** Una sola pantalla para los tres actores. El rol llega desde `data` de la ruta. */
@Component({
  selector: 'app-login',
  imports: [FormRoot, FormField, Icono],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private readonly sesion = inject(Sesion);
  private readonly router = inject(Router);

  readonly rol = input.required<Rol>();

  protected readonly titulo = computed(() => TITULOS[this.rol()]);
  protected readonly error = signal<string | null>(null);
  protected readonly mostrarPassword = signal(false);

  private readonly credenciales = signal({ username: '', password: '' });
  protected readonly loginForm = form(
    this.credenciales,
    (campo) => {
      required(campo.username, { message: 'Ingresá tu usuario.' });
      required(campo.password, { message: 'Ingresá tu contraseña.' });
    },
    { submission: { action: () => this.ingresar() } },
  );

  private async ingresar(): Promise<undefined> {
    this.error.set(null);
    try {
      const tieneRol = await firstValueFrom(this.sesion.iniciar(this.rol(), this.credenciales()));
      if (tieneRol) {
        await this.router.navigate(rutaInicio(this.rol()));
      } else {
        this.error.set('Este usuario no tiene acceso a esta sección.');
      }
    } catch (error) {
      this.error.set(mensajeDeError(error));
    }
    return undefined;
  }
}

function mensajeDeError(error: unknown): string {
  if (error instanceof HttpErrorResponse && error.status === 401) {
    return 'Usuario o contraseña incorrectos.';
  }
  if (error instanceof HttpErrorResponse && error.status === 0) {
    return 'No se pudo conectar con el servidor.';
  }
  return 'Ocurrió un error inesperado. Probá de nuevo más tarde.';
}
