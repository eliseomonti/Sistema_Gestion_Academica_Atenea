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
    path: 'comisiones-materia',
    loadComponent: () =>
      import('./comisiones-materia/pages/comisiones-materia-lista/comisiones-materia-lista').then(
        (m) => m.ComisionesMateriaLista,
      ),
    title: 'Mis comisiones',
  },
  {
    path: 'comisiones-materia/:comisionMateriaId',
    loadComponent: () =>
      import('./comisiones-materia/pages/comision-materia-detalle/comision-materia-detalle').then(
        (m) => m.ComisionMateriaDetalle,
      ),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'estudiantes' },
      {
        path: 'estudiantes',
        loadComponent: () =>
          import('./comisiones-materia/pages/estudiantes-lista/estudiantes-lista').then(
            (m) => m.EstudiantesLista,
          ),
        title: 'Estudiantes de la materia',
      },
      {
        // Sin pantalla propia: agrupa la lista y la planilla de una evaluación, para que la
        // pestaña Evaluaciones siga marcada en las dos y el ".." de la planilla vuelva a la lista.
        path: 'evaluaciones',
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./comisiones-materia/pages/evaluaciones-lista/evaluaciones-lista').then(
                (m) => m.EvaluacionesLista,
              ),
            title: 'Evaluaciones de la materia',
            // Hija de la lista: se muestra en su <router-outlet>, como ventana encima de ella.
            children: [
              {
                path: 'nueva',
                loadComponent: () =>
                  import('./comisiones-materia/pages/evaluacion-alta/evaluacion-alta').then(
                    (m) => m.EvaluacionAlta,
                  ),
                title: 'Nueva evaluación',
              },
            ],
          },
          {
            path: ':evaluacionId',
            loadComponent: () =>
              import('./evaluaciones/pages/evaluacion-notas/evaluacion-notas').then(
                (m) => m.EvaluacionNotas,
              ),
            title: 'Notas de la evaluación',
            // Hija de la planilla: se muestra en su <router-outlet>, como ventana encima de ella.
            children: [
              {
                path: 'editar',
                loadComponent: () =>
                  import('./evaluaciones/pages/evaluacion-edicion/evaluacion-edicion').then(
                    (m) => m.EvaluacionEdicion,
                  ),
                title: 'Editar evaluación',
              },
            ],
          },
        ],
      },
      {
        path: 'cierre',
        loadComponent: () =>
          import('./comisiones-materia/pages/cierre-cursada/cierre-cursada').then(
            (m) => m.CierreCursada,
          ),
        title: 'Cierre de la cursada',
      },
    ],
  },
  {
    path: 'examenes-finales',
    loadComponent: () =>
      import('./examenes-finales/pages/examenes-finales-lista/examenes-finales-lista').then(
        (m) => m.ExamenesFinalesLista,
      ),
    title: 'Exámenes finales',
  },
  {
    path: 'examenes-finales/:examenFinalId',
    loadComponent: () =>
      import('./examenes-finales/pages/examen-final-notas/examen-final-notas').then(
        (m) => m.ExamenFinalNotas,
      ),
    title: 'Notas de la mesa',
  },
];

export default routes;
