import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import {
  form,
  FormField,
  FormRoot,
  maxLength,
  minLength,
  required,
  validate,
} from '@angular/forms/signals';
import { firstValueFrom } from 'rxjs';
import { NOMBRES_ROL } from '../core/seguridad/rol';
import { Sesion } from '../core/seguridad/sesion';
import { Icono } from '../shared/ui/icono/icono';

/** Los mismos límites que valida el backend (CambioPasswordRequest). */
const LARGO_MINIMO = 8;
const LARGO_MAXIMO = 72;

const VACIO = { passwordActual: '', passwordNueva: '', confirmacion: '' };

/**
 * La cuenta del usuario logueado, con cualquier rol. Los datos salen del access token: si cambian,
 * se ven actualizados en el próximo refresh.
 */
@Component({
  selector: 'app-cuenta',
  imports: [FormRoot, FormField, Icono],
  templateUrl: './cuenta.html',
  styleUrl: './cuenta.css',
})
export class Cuenta {
  private readonly sesion = inject(Sesion);

  protected readonly nombre = this.sesion.nombreCompleto;
  protected readonly usuario = this.sesion.usuario;
  /** 'Docente, Estudiante'. */
  protected readonly roles = computed(() =>
    this.sesion
      .roles()
      .map((rol) => NOMBRES_ROL[rol])
      .join(', '),
  );

  protected readonly largoMinimo = LARGO_MINIMO;
  protected readonly error = signal<string | null>(null);
  protected readonly cambiada = signal(false);
  /** Cada campo tiene su botón para ver lo que se escribió. */
  protected readonly mostrarActual = signal(false);
  protected readonly mostrarNueva = signal(false);
  protected readonly mostrarConfirmacion = signal(false);

  private readonly passwords = signal({ ...VACIO });
  protected readonly passwordForm = form(
    this.passwords,
    (campo) => {
      required(campo.passwordActual, { message: 'Ingresá tu contraseña actual.' });
      required(campo.passwordNueva, { message: 'Ingresá la contraseña nueva.' });
      minLength(campo.passwordNueva, LARGO_MINIMO, {
        message: `Tiene que tener al menos ${LARGO_MINIMO} caracteres.`,
      });
      maxLength(campo.passwordNueva, LARGO_MAXIMO, {
        message: `Tiene que tener como máximo ${LARGO_MAXIMO} caracteres.`,
      });
      required(campo.confirmacion, { message: 'Repetí la contraseña nueva.' });
      // valueOf lee otro campo: la confirmación tiene que ser igual a la nueva.
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
    this.cambiada.set(false);
    try {
      const { passwordActual, passwordNueva } = this.passwords();
      await firstValueFrom(this.sesion.cambiarPassword({ passwordActual, passwordNueva }));
      this.cambiada.set(true);
      // Vacía los campos y les saca el "tocado", para que no aparezcan los errores de vacío.
      this.passwordForm().reset({ ...VACIO });
    } catch (error) {
      this.error.set(mensajeDeError(error));
    }
    return undefined;
  }
}

/** 409: la actual no es correcta o la nueva es igual. 400: la nueva no cumple el largo. */
function mensajeDeError(error: unknown): string {
  if (error instanceof HttpErrorResponse && (error.status === 409 || error.status === 400)) {
    return error.error?.errores?.[0]?.mensaje ?? error.error?.detail ?? 'Revisá los datos.';
  }
  if (error instanceof HttpErrorResponse && error.status === 0) {
    return 'No se pudo conectar con el servidor.';
  }
  return 'Ocurrió un error inesperado. Probá de nuevo más tarde.';
}
