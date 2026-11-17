import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, DestroyRef, inject, input, signal } from '@angular/core';
import { email, form, FormField, FormRoot, required } from '@angular/forms/signals';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { rolDeUrl, rutaLogin } from '../../core/seguridad/rol';
import { Sesion } from '../../core/seguridad/sesion';
import { Icono } from '../../shared/ui/icono/icono';
import { TITULOS } from '../login';

const ESPERA_REENVIO = 60;

/**
 * Quien se olvidó la contraseña escribe su email y le llega un mail con un link. El rol llega como
 * ?rol=docente desde el login, para volver a ese login y para que el link del mail lo lleve.
 */
@Component({
  selector: 'app-recuperar-password',
  imports: [FormRoot, FormField, RouterLink, Icono],
  templateUrl: './recuperar-password.html',
  styleUrl: '../login.css',
})
export class RecuperarPassword {
  private readonly sesion = inject(Sesion);
  private intervalo?: ReturnType<typeof setInterval>;

  readonly rol = input<string>();

  private readonly rolValido = computed(() => rolDeUrl(this.rol()));
  protected readonly titulo = computed(() => TITULOS[this.rolValido() ?? 'ESTUDIANTE']);
  protected readonly login = computed(() => {
    const rol = this.rolValido();
    return rol ? rutaLogin(rol) : ['/login'];
  });
  protected readonly enviado = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly segundosParaReenviar = signal(0);
  protected readonly textoReenviar = computed(() => {
    const segundos = this.segundosParaReenviar();
    return segundos > 0 ? `reenviá el link en ${segundos} s` : 'reenviá el link';
  });

  private readonly datos = signal({ email: '' });
  protected readonly recuperarForm = form(
    this.datos,
    (campo) => {
      required(campo.email, { message: 'Ingresá tu email.' });
      email(campo.email, { message: 'Formato inválido.' });
    },
    { submission: { action: () => this.enviar() } },
  );

  constructor() {
    inject(DestroyRef).onDestroy(() => clearInterval(this.intervalo));
  }

  private async enviar(): Promise<undefined> {
    this.error.set(null);
    try {
      await firstValueFrom(
        this.sesion.recuperarPassword({
          email: this.datos().email,
          rol: this.rolValido()?.toLowerCase(),
        }),
      );
      if (!this.enviado()) {
        this.enviado.set(true);
        this.esperarParaReenviar();
      }
    } catch (error) {
      this.error.set(
        error instanceof HttpErrorResponse && error.status === 0
          ? 'No se pudo conectar con el servidor.'
          : 'Ocurrió un error inesperado. Probá de nuevo más tarde.',
      );
    }
    return undefined;
  }

  protected reenviar(): void {
    this.esperarParaReenviar();
    void this.enviar();
  }

  private esperarParaReenviar(): void {
    clearInterval(this.intervalo);
    this.segundosParaReenviar.set(ESPERA_REENVIO);
    this.intervalo = setInterval(() => {
      this.segundosParaReenviar.update((segundos) => segundos - 1);
      if (this.segundosParaReenviar() === 0) {
        clearInterval(this.intervalo);
      }
    }, 1000);
  }
}
