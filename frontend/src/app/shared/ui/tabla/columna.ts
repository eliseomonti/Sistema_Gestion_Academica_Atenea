/** Una columna de `app-tabla`. */
export interface Columna<T> {
  /** Identifica la columna. Si no hay `valor`, la celda muestra `fila[clave]`. */
  clave: string;
  /** Texto del encabezado de la tabla. En celular no se muestra, salvo con `tituloEnLista`. */
  titulo: string;
  /** Para mostrar otra cosa que el campo tal cual, por ejemplo un enum traducido a texto. */
  valor?: (fila: T) => string | number;
  /**
   * En celular, el dato va en su propia línea, chico y en negrita, con el título de la columna
   * en gris adelante ("Código **01**"). Para datos que no se entienden solos, como números o
   * códigos. En la tabla de escritorio no cambia nada.
   */
  tituloEnLista?: boolean;
  /**
   * La columna que nombra a la fila: en celular es el título, aunque en escritorio vaya otra
   * columna primero, y con `enlace` lleva el link. Si ninguna
   * columna la tiene, es la primera.
   */
  principal?: boolean;
  /**
   * En celular no se muestra. Para datos que la lista ya muestra de otra forma, como el período
   * de una materia, que ahí va como subtítulo (ver `subagrupar` en `Tabla`).
   */
  ocultaEnLista?: boolean;
}
