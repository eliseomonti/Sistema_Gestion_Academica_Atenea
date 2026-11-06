import { Component, input, output, signal } from '@angular/core';
import { form, FormField, FormRoot, pattern, required } from '@angular/forms/signals';

@Component({
  imports: [FormRoot, FormField],
  selector: 'app-dni-paso',
  styleUrl: './dni-paso.css',
  templateUrl: './dni-paso.html',
})
export class DniPaso {
  
  readonly buscando = input(false);
  readonly buscar = output<string>();

  private readonly datos = signal({dni: ''});
  protected readonly dniForm = form(
    this.datos,
    (campo) => {
      required(campo.dni, {message: 'Ingresa el dni'});
      
      pattern(campo.dni, /^[1-9]\d{6,7}$/, {message: 'El dni debe tener entre 7 y 8 digitos, sin puntos ni 0 adelante'});
      
    },
    {
      submission:
      {
        action: async () => {
          this.buscar.emit(this.datos().dni);
          return undefined;
        }
      }
    }  
  )



}
