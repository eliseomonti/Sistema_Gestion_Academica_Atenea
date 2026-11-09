import { NombreIcono } from '../../shared/ui/icono/icono';
import { Rol } from '../seguridad/rol';

export interface ItemMenu {
  texto: string;
  /** URL completa de la pantalla, por ejemplo '/admin/estudiantes'. */
  ruta: string;
  icono: NombreIcono;
  /**
   * La opción se marca sólo en esa URL exacta. Sin esto, se marca también en las que empiezan
   * con ella: Carreras sigue marcada adentro de un plan. Inicio la necesita, porque todas las
   * URL del actor empiezan con la suya.
   */
  exacta?: boolean;
}

/**
 * Las opciones del menú de cada actor. Salvo Inicio, cada ruta es la de una feature, que lleva el
 * nombre del recurso de la API (/api/admin/estudiantes → /admin/estudiantes).
 */
export const MENUS: Record<Rol, ItemMenu[]> = {
  // Comisiones y períodos de inscripción no van acá: van a ser pestañas de la pantalla del plan
  // (Carreras → plan). Exámenes finales todavía no tiene un lugar decidido.
  ADMIN: [
    { texto: 'Inicio', ruta: '/admin', icono: 'casa', exacta: true },
    { texto: 'Carreras', ruta: '/admin/carreras', icono: 'libro' },
    { texto: 'Estudiantes', ruta: '/admin/estudiantes', icono: 'birrete' },
    { texto: 'Docentes', ruta: '/admin/docentes', icono: 'persona' },
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
