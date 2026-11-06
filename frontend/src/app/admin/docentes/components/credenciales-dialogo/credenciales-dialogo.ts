import { Component, input, output } from '@angular/core';
import { AltaPersonaResponse } from '../../../personas/data/persona';

@Component({
  imports: [],
  selector: 'app-credenciales-dialogo',
  styleUrl: './credenciales-dialogo.css',
  templateUrl: './credenciales-dialogo.html',
})
export class CredencialesDialogo {

  readonly credenciales = input.required<AltaPersonaResponse>();
  readonly cerrar = output();


}
