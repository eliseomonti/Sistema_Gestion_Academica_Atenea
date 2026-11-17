import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, input, signal } from '@angular/core';
import {
  form,
  FormField,
  FormRoot,
  maxLength,
  minLength,
  required,
  validate,
} from '@angular/forms/signals';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { rolDeUrl, rutaLogin } from '../../core/seguridad/rol';
import { Sesion } from '../../core/seguridad/sesion';
import { Icono } from '../../shared/ui/icono/icono';
import { TITULOS } from '../login';

/** Los mismos límites que valida el backend (RestablecimientoPasswordRequest). */
const LARGO_MINIMO = 8;
const LARGO_MAXIMO = 72;

/**
 * A esta pantalla lleva el link del mail. El token llega como ?token= y el rol, si se pidió desde un
 * login o desde Mi cuenta, como ?rol=docente: se ven como input().
 */
@Component({
  selector: 'app-restablecer-password',
  imports: [FormRoot, FormField, RouterLink, Icono],
  templateUrl: './restablecer-password.html',
  styleUrl: '../login.css',
})
export class RestablecerPassword {
  private readonly sesion = inject(Sesion);

  readonly token = input<string>();
  readonly rol = input<string>();

  protected readonly titulo = computed(() => TITULOS[rolDeUrl(this.rol()) ?? 'ESTUDIANTE']);
  protected readonly login = computed(() => {
    const rol = rolDeUrl(this.rol());
    return rol ? rutaLogin(rol) : ['/login'];
  });
  /** Pedir otro link desde acá vuelve a llevar el rol. */
  protected readonly parametrosRol = computed(() => {
    const rol = rolDeUrl(this.rol());
    return rol ? { rol: rol.toLowerCase() } : {};
  });
  protected readonly largoMinimo = LARGO_MINIMO;
  protected readonly cambiada = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly mostrarNueva = signal(false);
  protected readonly mostrarConfirmacion = signal(false);

  private readonly passwords = signal({ passwordNueva: '', confirmacion: '' });
  protected readonly passwordForm = form(
    this.passwords,
    (campo) => {
      required(campo.passwordNueva, { message: 'Ingresá la contraseña nueva.' });
      minLength(campo.passwordNueva, LARGO_MINIMO, {
        message: `Tiene que tener al menos ${LARGO_MINIMO} caracteres.`,
      });
      maxLength(campo.passwordNueva, LARGO_MAXIMO, {
        message: `Tiene que tener como máximo ${LARGO_MAXIMO} caracteres.`,
      });
      required(campo.confirmacion, { message: 'Repetí la contraseña nueva.' });
      validate(campo.confirmacion, ({ value, valueOf }) =>
        value() === valueOf(campo.passwordNueva)
          ? null
          : { kind: 'distinta', message: 'No coincide con la contraseña nueva.' },
      );
    },
    { submission: { action: () => this.cambiar() } },
  );

  private async cambiar(): Promise<undefined> {
    this.error.set(null);
    try {
      await firstValueFrom(
        this.sesion.restablecerPassword({
          token: this.token() ?? '',
          passwordNueva: this.passwords().passwordNueva,
        }),
      );
      this.cambiada.set(true);
    } catch (error) {
      this.error.set(mensajeDeError(error));
    }
    return undefined;
  }
}

/** 409: el link no sirve (vencido, usado o inventado). 400: la contraseña no cumple el largo. */
function mensajeDeError(error: unknown): string {
  if (error instanceof HttpErrorResponse && (error.status === 409 || error.status === 400)) {
    return error.error?.errores?.[0]?.mensaje ?? error.error?.detail ?? 'Revisá los datos.';
  }
  if (error instanceof HttpErrorResponse && error.status === 0) {
    return 'No se pudo conectar con el servidor.';
  }
  return 'Ocurrió un error inesperado. Probá de nuevo más tarde.';
}
