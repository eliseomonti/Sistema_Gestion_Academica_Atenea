import { Routes } from '@angular/router';

// Acá se agregan las features de este actor, cada una con loadChildren.
const routes: Routes = [
  { path: 'carreras', loadChildren: () => import('./carreras/carreras.routes') },
  {path: 'docentes', loadChildren: () => import('./docentes/docentes.routes')},
];

export default routes;
