import Axios from "axios";
import { apiRequest } from "./authConfig";

// React accede a las funciones de negocio SOLO a través de AWS API Gateway.
const API = process.env.REACT_APP_API_BASE_URL;

/** Crea un cliente Axios que agrega el access token de Entra ID en cada llamada. */
export function crearCliente(instance, account) {
  const cliente = Axios.create({ baseURL: API });
  cliente.interceptors.request.use(async (config) => {
    const token = await instance.acquireTokenSilent({ ...apiRequest, account });
    config.headers.Authorization = `Bearer ${token.accessToken}`;
    return config;
  });
  return cliente;
}

/** Decodifica el payload del access token para mostrar roles y scopes. */
export async function leerClaims(instance, account) {
  const token = await instance.acquireTokenSilent({ ...apiRequest, account });
  const payload = token.accessToken.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
  return JSON.parse(decodeURIComponent(escape(window.atob(payload))));
}

/** Mensaje legible de un error de Axios (incluye el código HTTP). */
export function mensajeError(error) {
  if (error.response) {
    const d = error.response.data;
    const texto = (d && (d.mensaje || d.error)) || error.response.statusText || "";
    return `HTTP ${error.response.status}: ${texto}`;
  }
  return error.message;
}
