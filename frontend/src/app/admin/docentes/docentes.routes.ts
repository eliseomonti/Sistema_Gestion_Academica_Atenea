import { Routes } from '@angular/router';
import { DocentesLista } from './pages/docentes-lista/docentes-lista';
import { DocenteAlta } from './pages/docente-alta/docente-alta';

const routes: Routes = [
    { path: '', component: DocentesLista, title: 'Docentes' },
    {path: 'nuevo', component: DocenteAlta, title: 'Docente Nuevo'},
];

export default routes;