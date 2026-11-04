import { NombreIcono } from '../../shared/ui/icono/icono';
import { Rol } from '../seguridad/rol';

export interface ItemMenu {
  texto: string;
  /** URL completa de la pantalla, por ejemplo '/admin/estudiantes'. */
  ruta: string;
  icono: NombreIcono;
}

/**
 * Las opciones del menú de cada actor. Cada ruta es la de una feature, que lleva el nombre del
 * recurso de la API (/api/admin/estudiantes → /admin/estudiantes).
 */
export const MENUS: Record<Rol, ItemMenu[]> = {
  ADMIN: [
    { texto: 'Estudiantes', ruta: '/admin/estudiantes', icono: 'birrete' },
    { texto: 'Docentes', ruta: '/admin/docentes', icono: 'persona' },
    { texto: 'Carreras', ruta: '/admin/carreras', icono: 'libro' },
    { texto: 'Comisiones', ruta: '/admin/comisiones', icono: 'capas' },
    { texto: 'Exámenes finales', ruta: '/admin/examenes-finales', icono: 'documento' },
    { texto: 'Períodos de inscripción', ruta: '/admin/periodos-inscripcion', icono: 'calendario' },
  ],
  DOCENTE: [
    { texto: 'Mis comisiones', ruta: '/docente/comisiones-materia', icono: 'capas' },
    { texto: 'Exámenes finales', ruta: '/docente/examenes-finales', icono: 'documento' },
  ],
  ESTUDIANTE: [
    { texto: 'Plan de estudio', ruta: '/estudiante/plan-estudio', icono: 'libro' },
    { texto: 'Inscripción a cursado', ruta: '/estudiante/inscripciones-comision', icono: 'editar' },
    {
      texto: 'Inscripción a finales',
      ruta: '/estudiante/inscripciones-final',
      icono: 'portapapeles',
    },
    { texto: 'Notas', ruta: '/estudiante/notas', icono: 'medalla' },
  ],
};
