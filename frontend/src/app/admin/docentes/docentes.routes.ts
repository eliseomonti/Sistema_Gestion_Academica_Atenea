import { Routes } from '@angular/router';
import { DocentesLista } from './pages/docentes-lista/docentes-lista';

// El alta (/admin/docentes/nuevo) está en admin.routes.ts: su pantalla es de la feature personas.
const routes: Routes = [
    { path: '', component: DocentesLista, title: 'Docentes' },
];

export default routes;