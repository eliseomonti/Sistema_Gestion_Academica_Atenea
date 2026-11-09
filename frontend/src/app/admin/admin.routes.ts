import { Routes } from '@angular/router';

// Acá se agregan las features de este actor, cada una con loadChildren.
const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./inicio/pages/inicio/inicio').then((m) => m.Inicio),
    title: 'Inicio',
  },
  {
    path: 'carreras/:carreraId/planes/:planId',
    loadComponent: () =>
      import('./carreras/pages/plan-detalle/plan-detalle').then((m) => m.PlanDetalle),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'materias' },
      {
        path: 'materias',
        loadComponent: () =>
          import('./carreras/pages/materias-lista/materias-lista').then((m) => m.MateriasLista),
        title: 'Materias del plan',
      },
      {
        path: 'estudiantes',
        loadComponent: () =>
          import('./estudiantes/pages/estudiantes-lista/estudiantes-lista').then(
            (m) => m.EstudiantesLista,
          ),
        title: 'Estudiantes del plan',
        // Hija de la lista: se muestra en su <router-outlet>, como ventana encima de ella.
        children: [
          {
            path: 'nuevo',
            loadComponent: () =>
              import('./personas/pages/estudiante-inscripcion/estudiante-inscripcion').then(
                (m) => m.EstudianteInscripcion,
              ),
            title: 'Inscribir estudiante',
          },
        ],
      },
    ],
  },
  {
    // Va antes de 'docentes': las pantallas de alta son de personas, que comparte sus partes con el
    // alta de estudiantes.
    path: 'docentes/nuevo',
    loadComponent: () =>
      import('./personas/pages/docente-alta/docente-alta').then((m) => m.DocenteAlta),
    title: 'Docente Nuevo',
  },
  { path: 'carreras', loadChildren: () => import('./carreras/carreras.routes') },
  { path: 'docentes', loadChildren: () => import('./docentes/docentes.routes') },
  {
    // Acá y no en un estudiantes.routes.ts: la ventana de inscripción es de la feature personas, y
    // sólo este archivo puede cargar pantallas de otra feature.
    path: 'estudiantes',
    loadComponent: () =>
      import('./estudiantes/pages/estudiantes-lista/estudiantes-lista').then(
        (m) => m.EstudiantesLista,
      ),
    title: 'Estudiantes',
    children: [
      {
        path: 'nuevo',
        loadComponent: () =>
          import('./personas/pages/estudiante-inscripcion/estudiante-inscripcion').then(
            (m) => m.EstudianteInscripcion,
          ),
        title: 'Inscribir estudiante',
      },
    ],
  },
];

export default routes;
