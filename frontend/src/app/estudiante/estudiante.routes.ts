import { Routes } from '@angular/router';

// Acá se agregan las features de este actor, cada una con loadChildren.
const routes: Routes = [
    {path: '', pathMatch: 'full', loadComponent: () => import('./inicio/pages/inicio/inicio').then((m) => m.Inicio), title: 'Inicio'},
    {
        path: 'plan-estudio', loadComponent: () => import('./plan-estudio/pages/plan-estudio/plan-estudio').then((m) => m.PlanEstudio),
        children: [
            {path: '', pathMatch: 'full', redirectTo: 'materias'},
            {
                path: 'materias', loadComponent: () => import('./plan-estudio/pages/plan-materias/plan-materias').then((m) => m.PlanMaterias),
                title: 'Plan de estudio'
            },
            {
                path: 'correlatividades', loadComponent: () => import('./plan-estudio/pages/plan-correlatividades/plan-correlatividades').then((m) => m.PlanCorrelatividades),
                title: 'Correlatividades'
            },
        ]
    },
    {
        path: 'inscripciones-comision', loadComponent: () => import('./inscripciones-comision/pages/inscripcion-cursado/inscripcion-cursado').then((m) => m.InscripcionCursado),
        children: [
            {path: '', pathMatch: 'full', redirectTo: 'oferta'},
            {
                path: 'oferta', loadComponent: () => import('./inscripciones-comision/pages/oferta/oferta').then((m) => m.Oferta),
                title: 'Inscripcion a cursado'
            },
            {
                path: 'mis-inscripciones', loadComponent: () => import('./inscripciones-comision/pages/mis-inscripciones/mis-inscripciones').then((m) => m.MisInscripciones),
                title: 'Mis inscripciones a cursado'
            },
        ]
    },
    {
        path: 'inscripciones-final', loadComponent: () => import('./inscripciones-final/pages/inscripcion-finales/inscripcion-finales').then((m) => m.InscripcionFinales),
        children: [
            {path: '', pathMatch: 'full', redirectTo: 'mesas'},
            {
                path: 'mesas', loadComponent: () => import('./inscripciones-final/pages/mesas/mesas').then((m) => m.Mesas),
                title: 'inscripcion a finales'
            },
            {
                path: 'mis-inscripciones', loadComponent: () => import('./inscripciones-final/pages/mis-inscripciones/mis-inscripciones').then((m) => m.MisInscripcionesFinal),
                title: 'Mis inscripciones a finales'
            },
        ]
    },
    {
        path: 'notas', loadComponent: () => import('./notas/pages/notas/notas').then((m) => m.Notas),
        children: [
            {path: '', pathMatch: 'full', redirectTo: 'cursadas'},
            {
                path: 'cursadas', loadComponent: () => import('./notas/pages/notas-cursadas/notas-cursadas').then((m) => m.NotasCursadas),
                title: 'Mis notas de cursadas'
            },
            {
                path: 'finales', loadComponent: () => import('./notas/pages/notas-finales/notas-finales').then((m) => m.NotasFinales),
                title: 'Mis notas de finales'
            },
        ]
    }
    
];

export default routes;
