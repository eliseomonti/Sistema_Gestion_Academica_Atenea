export interface LoginRequest {
  username: string;
  password: string;
}

export interface CambioPasswordRequest {
  passwordActual: string;
  passwordNueva: string;
}

/** El rol, escrito como en la URL ('docente'), va en el link del mail para volver a ese login. */
export interface RecuperacionPasswordRequest {
  email: string;
  rol?: string;
}

export interface LinkPasswordRequest {
  rol?: string;
}

/** El token es el que llegó en el link del mail. */
export interface RestablecimientoPasswordRequest {
  token: string;
  passwordNueva: string;
}

export interface RefreshRequest {
  refreshToken: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  /** Segundos que dura el access token. */
  expiresIn: number;
}
