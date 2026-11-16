import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, input, signal } from '@angular/core';
import { email, form, FormField, FormRoot, required } from '@angular/forms/signals';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { rolDeUrl, rutaLogin } from '../../core/seguridad/rol';
import { Sesion } from '../../core/seguridad/sesion';

/**
 * Quien se olvidó la contraseña escribe su email y le llega un mail con un link. El rol llega como
 * ?rol=docente desde el login, para volver a ese login y para que el link del mail lo lleve.
 */
@Component({
  selector: 'app-recuperar-password',
  imports: [FormRoot, FormField, RouterLink],
  templateUrl: './recuperar-password.html',
  styleUrl: '../login.css',
})
export class RecuperarPassword {
  private readonly sesion = inject(Sesion);

  readonly rol = input<string>();

  private readonly rolValido = computed(() => rolDeUrl(this.rol()));
  protected readonly login = computed(() => {
    const rol = this.rolValido();
    return rol ? rutaLogin(rol) : ['/login'];
  });
  protected readonly enviado = signal(false);
  protected readonly error = signal<string | null>(null);

  private readonly datos = signal({ email: '' });
  protected readonly recuperarForm = form(
    this.datos,
    (campo) => {
      required(campo.email, { message: 'Ingresá tu email.' });
      email(campo.email, { message: 'Revisá el email: no tiene un formato válido.' });
    },
    { submission: { action: () => this.enviar() } },
  );

  private async enviar(): Promise<undefined> {
    this.error.set(null);
    try {
      await firstValueFrom(
        this.sesion.recuperarPassword({
          email: this.datos().email,
          rol: this.rolValido()?.toLowerCase(),
        }),
      );
      this.enviado.set(true);
    } catch (error) {
      this.error.set(
        error instanceof HttpErrorResponse && error.status === 0
          ? 'No se pudo conectar con el servidor.'
          : 'Ocurrió un error inesperado. Probá de nuevo más tarde.',
      );
    }
    return undefined;
  }
}
