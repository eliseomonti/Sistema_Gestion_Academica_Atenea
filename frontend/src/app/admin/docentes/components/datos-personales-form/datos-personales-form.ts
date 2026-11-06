import { Component, input, output, signal } from '@angular/core';
import { email, form, FormField, FormRoot, pattern, required } from '@angular/forms/signals';
import { DatosPersonalesRequest } from '../../../personas/data/persona';

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

  private readonly datos = signal({
    nombre: '',
    apellido: '',
    telefono: '',
    fechaNacimiento: '',
    email: '',
  });

  protected readonly personaForm = form(
    this.datos, (campo) => {
      required(campo.nombre, {message: 'Ingresa el nombre'});
      required(campo.apellido, { message: 'Ingresa el apellido'});
      required(campo.telefono, { message: 'Ingresa el telefono'});
      pattern(campo.telefono, /^\d{10}$/, { message: 'El telefono tiene que tener 10 numeros: sin 0 el codigo del area y el numero sin el 15'});
      required(campo.fechaNacimiento, { message: 'Ingresa la fecha de nacimiento'});
      required(campo.email, { message: 'Ingresa el email.' });
      email(campo.email, { message: 'El email no tiene un formato valido'});
    },
    {
      submission: 
      {
        action: async () => {
          this.guardar.emit({...this.datos(), dni: this.dni()});
          return undefined;
        },
      },
    },
  )
}
