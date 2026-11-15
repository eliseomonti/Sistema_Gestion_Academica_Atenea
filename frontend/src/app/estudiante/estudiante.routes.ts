import { Routes } from '@angular/router';

// Acá se agregan las features de este actor, cada una con loadChildren.
const routes: Routes = [
    {path: '', pathMatch: 'full', loadComponent: () => import('./inicio/pages/inicio/inicio').then((m) => m.Inicio), title: 'Inicio'},
    {
        path: 'plan-estudio', loadComponent: () => import('./plan-estudio/pages/plan-estudio/plan-estudio').then((m) => m.PlanEstudio), 
        title: 'Plan de estudio'
    },
    {
        path: 'inscripciones-comision', loadComponent: () => import('./inscripciones-comision/pages/oferta/oferta').then((m) => m.Oferta), 
        title: 'Inscripcion a cursado'
    },
    {path: 'inscripciones-final', loadComponent: () => import('./inscripciones-final/pages/mesas/mesas').then((m) => m.Mesas), 
    title: 'inscripcion a finales' },
    
];

export default routes;
