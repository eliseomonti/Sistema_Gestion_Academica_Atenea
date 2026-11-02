import { Rol } from '../seguridad/rol';

export interface ItemMenu {
  texto: string;
  /** URL completa de la pantalla, por ejemplo '/admin/estudiantes'. */
  ruta: string;
}

/**
 * Las opciones del menú de cada actor. Cada ruta es la de una feature, que lleva el nombre del
 * recurso de la API (/api/admin/estudiantes → /admin/estudiantes).
 */
export const MENUS: Record<Rol, ItemMenu[]> = {
  ADMIN: [
    { texto: 'Estudiantes', ruta: '/admin/estudiantes' },
    { texto: 'Docentes', ruta: '/admin/docentes' },
    { texto: 'Carreras', ruta: '/admin/carreras' },
    { texto: 'Comisiones', ruta: '/admin/comisiones' },
    { texto: 'Exámenes finales', ruta: '/admin/examenes-finales' },
    { texto: 'Períodos de inscripción', ruta: '/admin/periodos-inscripcion' },
  ],
  DOCENTE: [
    { texto: 'Mis comisiones', ruta: '/docente/comisiones-materia' },
    { texto: 'Exámenes finales', ruta: '/docente/examenes-finales' },
  ],
  ESTUDIANTE: [
    { texto: 'Plan de estudio', ruta: '/estudiante/plan-estudio' },
    { texto: 'Inscripción a cursado', ruta: '/estudiante/inscripciones-comision' },
    { texto: 'Inscripción a finales', ruta: '/estudiante/inscripciones-final' },
    { texto: 'Notas', ruta: '/estudiante/notas' },
  ],
};
