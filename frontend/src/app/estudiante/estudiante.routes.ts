import { Routes } from '@angular/router';

// Acá se agregan las features de este actor, cada una con loadChildren.
const routes: Routes = [
    {
        path: 'inscripciones-comision', loadComponent: () => import('./inscripciones-comision/pages/oferta/oferta').then((m) => m.Oferta), 
        title: 'Inscripcion a cursado'
    },
];

export default routes;
