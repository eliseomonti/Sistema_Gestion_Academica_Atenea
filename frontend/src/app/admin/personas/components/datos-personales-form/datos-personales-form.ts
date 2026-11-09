import { Component, input, output, linkedSignal } from '@angular/core';
import {
  email,
  form,
  FormField,
  FormRoot,
  maxDate,
  minDate,
  pattern,
  required,
} from '@angular/forms/signals';
import { DatosPersonalesRequest, PersonaResponse} from '../../data/persona';

/** Las mismas que valida el backend (PersonaService): acá es para avisar antes de guardar. */
const EDAD_MINIMA = 16;
const EDAD_MAXIMA = 100;

@Component({
  imports: [FormRoot, FormField],
  selector: 'app-datos-personales-form',
  styleUrl: './datos-personales-form.css',
  templateUrl: './datos-personales-form.html',
})
export class DatosPersonalesForm {

  readonly dni = input.required<string>();
  readonly guardando = input(false);
  readonly guardar = output<DatosPersonalesRequest>();
  readonly inicial = input<PersonaResponse | null>(null);

  /**
   * La fecha de nacimiento es un Date (null mientras está vacía): así [formField] la toma del
   * calendario del navegador, y minDate y maxDate limitan las fechas que se pueden elegir.
   */
  private readonly datos = linkedSignal(() => {
    const persona = this.inicial();
    return {
      nombre: persona?.nombre ?? '',
      apellido: persona?.apellido ?? '',
      telefono: persona?.telefono ?? '',
      fechaNacimiento: persona ? new Date(persona.fechaNacimiento) : null,
      email: persona?.email ?? '',
    };
  });

  protected readonly personaForm = form(
    this.datos, (campo) => {
      required(campo.nombre, {message: 'Ingresa el nombre'});
      required(campo.apellido, { message: 'Ingresa el apellido'});
      required(campo.telefono, { message: 'Ingresa el telefono'});
      pattern(campo.telefono, /^\d{10}$/, { message: 'El telefono tiene que tener 10 numeros: sin 0 el codigo del area y el numero sin el 15'});
      required(campo.fechaNacimiento, { message: 'Ingresa la fecha de nacimiento'});
      // Nacido hasta hace 16 años, y después del día en que cumpliría 101.
      maxDate(campo.fechaNacimiento, haceAnios(EDAD_MINIMA), {
        message: `Tiene que tener al menos ${EDAD_MINIMA} años.`,
      });
      minDate(campo.fechaNacimiento, unDiaDespues(haceAnios(EDAD_MAXIMA + 1)), {
        message: `No puede tener más de ${EDAD_MAXIMA} años: revisá el año.`,
      });
      required(campo.email, { message: 'Ingresa el email.' });
      email(campo.email, { message: 'El email no tiene un formato valido'});
    },
    {
      submission:
      {
        action: async () => {
          this.guardar.emit({
            ...this.datos(),
            dni: this.dni(),
            // 2000-05-23, el formato de LocalDate en el backend. Llega acá sólo si es válida.
            fechaNacimiento: this.datos().fechaNacimiento?.toISOString().slice(0, 10) ?? '',
          });
          return undefined;
        },
      },
    },
  )
}

/**
 * Hoy hace `anios` años, a la medianoche UTC: así viene la fecha del calendario del navegador
 * (valueAsDate), y así se comparan bien.
 */
function haceAnios(anios: number): Date {
  const hoy = new Date();
  return new Date(Date.UTC(hoy.getFullYear() - anios, hoy.getMonth(), hoy.getDate()));
}

function unDiaDespues(fecha: Date): Date {
  return new Date(fecha.getTime() + 24 * 60 * 60 * 1000);
}
