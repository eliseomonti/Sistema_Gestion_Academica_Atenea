import { Component, input, output } from '@angular/core';

@Component({
  imports: [],
  selector: 'app-confirmar-dialog',
  styleUrl: './confirmar-dialog.css',
  templateUrl: './confirmar-dialog.html',
})
export class ConfirmarDialog {

  readonly mensaje = input.required<string>();
  readonly confirmarTexto = input('confirmar');
  readonly procesando = input(false)
  readonly confirmar = output();
  readonly cancelar = output(); 


}
