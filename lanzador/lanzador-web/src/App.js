import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { AuthenticatedTemplate, UnauthenticatedTemplate, useMsal } from "@azure/msal-react";
import Axios from "axios";
import { apiRequest, loginRequest } from "./authConfig";

const MAX_CANTIDAD = 100;
const MIN_INTERVALO = 200;
const RABBIT_URL = process.env.REACT_APP_RABBIT_URL;

const hora = () => new Date().toLocaleTimeString("es-CL", { hour12: false }) + "." + String(new Date().getMilliseconds()).padStart(3, "0");
const esperar = (ms) => new Promise((r) => setTimeout(r, ms));

function App() {
  const { instance, accounts } = useMsal();
  const cuenta = accounts[0];

  // Cliente Axios: llama al backend local (proxy de desarrollo, mismo origen) con el token de Entra ID
  const api = useMemo(() => {
    if (!cuenta) return null;
    const c = Axios.create();
    c.interceptors.request.use(async (config) => {
      const t = await instance.acquireTokenSilent({ ...apiRequest, account: cuenta });
      config.headers.Authorization = `Bearer ${t.accessToken}`;
      return config;
    });
    return c;
  }, [instance, cuenta]);

  const [usuario, setUsuario] = useState(null);
  const [errorUsuario, setErrorUsuario] = useState(null);
  const [cantidad, setCantidad] = useState(10);
  const [intervalo, setIntervalo] = useState(1000);
  const [prefijo, setPrefijo] = useState("");
  const [ejecutando, setEjecutando] = useState(false);
  const [progreso, setProgreso] = useState({ actual: 0, total: 0 });
  const [registros, setRegistros] = useState([]);
  const [consumidas, setConsumidas] = useState(new Set());
  const [errorConsumo, setErrorConsumo] = useState(null);
  const detener = useRef(false);
  const seq = useRef(0);

  // Usuario y roles validados por el backend
  useEffect(() => {
    if (!api) return;
    api.get("/api/usuario")
      .then((r) => { setUsuario(r.data); setErrorUsuario(null); })
      .catch((e) => setErrorUsuario(`HTTP ${e.response?.status || "?"}: no se pudo validar el token en el lanzador`));
  }, [api]);

  // Copia local de Solicitudes (EC2 B): marca qué disparos ya fueron consumidos
  const revisarConsumo = useCallback(async () => {
    if (!api) return;
    try {
      const r = await api.get("/api/solicitudes/categorias");
      setConsumidas(new Set(r.data.map((c) => c.categoriaId)));
      setErrorConsumo(null);
    } catch (e) {
      setErrorConsumo(`No se pudo leer Solicitudes (EC2 B): HTTP ${e.response?.status || "?"}`);
    }
  }, [api]);

  const pendientesConsumo = registros.filter((r) => r.categoriaId && r.estado === "PUBLICADA" && !consumidas.has(r.categoriaId)).length;
  useEffect(() => {
    if (!api) return;
    revisarConsumo();
    const t = setInterval(revisarConsumo, 2000);
    return () => clearInterval(t);
  }, [api, revisarConsumo]);

  const agregar = (reg) => setRegistros((prev) => [reg, ...prev].slice(0, 500));

  const dispararUna = useCallback(async () => {
    seq.current += 1;
    const n = seq.current;
    const inicio = performance.now();
    const cuerpo = prefijo.trim() ? { nombre: `${prefijo.trim()}-${String(n).padStart(4, "0")}-${Date.now() % 100000}` } : {};
    try {
      const r = await api.post("/api/disparar", cuerpo);
      agregar({ n, hora: hora(), http: r.status, ms: Math.round(performance.now() - inicio),
        nombre: r.data.nombre, categoriaId: r.data.id, eventId: r.data.eventId, estado: r.data.estadoPublicacion });
    } catch (e) {
      const d = e.response?.data || {};
      agregar({ n, hora: hora(), http: e.response?.status || "ERR", ms: Math.round(performance.now() - inicio),
        nombre: d.nombre || "—", categoriaId: d.categoriaId || null, eventId: d.eventId || "",
        estado: d.estadoPublicacion || d.error || (e.response?.status === 403 ? "SIN_PERMISO (403)" : e.response?.status === 401 ? "SIN_TOKEN (401)" : "ERROR"),
        detalle: d.mensaje || d.detalle || e.message });
    }
  }, [api, prefijo]);

  const ejecutarDemo = async () => {
    const total = Math.min(Math.max(parseInt(cantidad, 10) || 1, 1), MAX_CANTIDAD);
    const pausa = Math.max(parseInt(intervalo, 10) || MIN_INTERVALO, MIN_INTERVALO);
    detener.current = false;
    setEjecutando(true);
    setProgreso({ actual: 0, total });
    for (let i = 1; i <= total; i++) {
      if (detener.current) break;
      await dispararUna();
      setProgreso({ actual: i, total });
      if (i < total) await esperar(pausa);
    }
    setEjecutando(false);
  };

  const ok = registros.filter((r) => r.estado === "PUBLICADA").length;
  const errores = registros.length - ok;
  const consumidasDeEstaSesion = registros.filter((r) => r.categoriaId && consumidas.has(r.categoriaId)).length;
  const esAdmin = (usuario?.roles || []).includes("Admin");

  return (
    <>
      <div className="barra py-3 mb-4">
        <div className="container d-flex justify-content-between align-items-center">
          <div>
            <div className="h4 mb-0 fw-bold">MesaTech · Lanzador de solicitudes</div>
            <div className="small">Proyecto local con Microsoft Entra ID · dispara eventos hacia la EP2 en AWS (RabbitMQ)</div>
          </div>
          <AuthenticatedTemplate>
            <button className="btn btn-light btn-sm" onClick={() => instance.logoutRedirect()}>Cerrar sesión</button>
          </AuthenticatedTemplate>
        </div>
      </div>

      <div className="container pb-5">
        <UnauthenticatedTemplate>
          <div className="tarjeta p-4 text-center">
            <h2 className="mb-3">Iniciar sesión</h2>
            <p>Para disparar solicitudes debe autenticarse con su cuenta del tenant en Microsoft Entra ID.</p>
            <button className="btn btn-navy px-4" onClick={() => instance.loginRedirect(loginRequest)}>Iniciar sesión con Microsoft</button>
          </div>
        </UnauthenticatedTemplate>

        <AuthenticatedTemplate>
          <div className="row g-3 mb-3">
            <div className="col-lg-5">
              <div className="tarjeta p-3 h-100">
                <h2>Usuario autenticado</h2>
                {errorUsuario && <div className="alert alert-danger py-2 small">{errorUsuario}</div>}
                {usuario && (
                  <div className="small">
                    <div><strong>{usuario.nombre}</strong> · {usuario.usuario}</div>
                    <div>Scope: <code>{usuario.scopes}</code></div>
                    <div>Roles: {(usuario.roles || []).length ? usuario.roles.map((r) => <span key={r} className="badge bg-success me-1">{r}</span>) : <span className="badge bg-warning text-dark">sin rol</span>}</div>
                    <div className="text-muted mt-1">Token validado por el lanzador · expira {usuario.expira?.replace("T", " ").slice(0, 19)}</div>
                    {!esAdmin && <div className="alert alert-warning py-1 px-2 mt-2 mb-0">Sin rol Admin: los disparos responderán 403.</div>}
                  </div>
                )}
              </div>
            </div>
            <div className="col-lg-7">
              <div className="row g-2 h-100">
                {[["Enviadas", registros.length], ["ACK broker", ok], ["Errores", errores], ["Consumidas", consumidasDeEstaSesion], ["En cola / proceso", pendientesConsumo]].map(([l, n]) => (
                  <div className="col" key={l}><div className="dato h-100"><div className="n">{n}</div><div className="l">{l}</div></div></div>
                ))}
              </div>
            </div>
          </div>

          <div className="row g-3 mb-3">
            <div className="col-lg-4">
              <div className="tarjeta p-3 h-100">
                <h2>Solicitud individual</h2>
                <p className="small mb-2">Crea una categoría en Catálogo (EC2 A): se guarda en su RDS y se publica <code>categoria.creada</code> en RabbitMQ.</p>
                <label className="form-label small mb-1">Prefijo del nombre (opcional)</label>
                <input className="form-control form-control-sm mb-3" placeholder="Demo (automático)" value={prefijo} disabled={ejecutando}
                  onChange={(e) => setPrefijo(e.target.value.replace(/[^A-Za-z0-9-]/g, "").slice(0, 30))} />
                <button className="btn btn-teal w-100" disabled={ejecutando} onClick={dispararUna}>Enviar 1 solicitud</button>
              </div>
            </div>
            <div className="col-lg-8">
              <div className="tarjeta p-3 h-100">
                <h2>Modo demo</h2>
                <div className="row g-2 align-items-end">
                  <div className="col-sm-4">
                    <label className="form-label small mb-1">Cantidad (1 a {MAX_CANTIDAD})</label>
                    <input type="number" min="1" max={MAX_CANTIDAD} className="form-control" value={cantidad} disabled={ejecutando} onChange={(e) => setCantidad(e.target.value)} />
                  </div>
                  <div className="col-sm-4">
                    <label className="form-label small mb-1">Intervalo entre envíos (ms, mín. {MIN_INTERVALO})</label>
                    <input type="number" min={MIN_INTERVALO} step="100" className="form-control" value={intervalo} disabled={ejecutando} onChange={(e) => setIntervalo(e.target.value)} />
                  </div>
                  <div className="col-sm-4 d-grid">
                    {!ejecutando
                      ? <button className="btn btn-navy" onClick={ejecutarDemo}>Ejecutar</button>
                      : <button className="btn btn-danger" onClick={() => { detener.current = true; }}>Detener</button>}
                  </div>
                </div>
                {progreso.total > 0 && (
                  <div className="mt-3">
                    <div className="progress" style={{ height: 18 }}>
                      <div className="progress-bar" style={{ width: `${(progreso.actual / progreso.total) * 100}%`, background: "var(--teal)" }}>
                        {progreso.actual} / {progreso.total}
                      </div>
                    </div>
                  </div>
                )}
                <div className="small text-muted mt-2">
                  Observe en paralelo: panel de RabbitMQ (Ready / Unacked){RABBIT_URL && <> — <a href={RABBIT_URL} target="_blank" rel="noreferrer">abrir cola</a></>} y <code>journalctl -u mesatech-solicitudes -f</code> en la EC2 B.
                </div>
              </div>
            </div>
          </div>

          <div className="tarjeta p-3">
            <div className="d-flex justify-content-between align-items-center mb-2">
              <h2 className="mb-0">Log en tiempo real</h2>
              <div>
                <button className="btn btn-outline-secondary btn-sm me-2" onClick={revisarConsumo}>Actualizar consumo</button>
                <button className="btn btn-outline-secondary btn-sm" disabled={ejecutando} onClick={() => { setRegistros([]); setProgreso({ actual: 0, total: 0 }); }}>Limpiar</button>
              </div>
            </div>
            {errorConsumo && <div className="alert alert-warning py-1 small">{errorConsumo}</div>}
            <div className="log">
              <table className="table table-sm table-striped align-middle">
                <thead>
                  <tr><th>#</th><th>Hora</th><th>HTTP</th><th>ms</th><th>Nombre</th><th>categoriaId</th><th>eventId</th><th>Publicación</th><th>Consumo (EC2 B)</th></tr>
                </thead>
                <tbody>
                  {registros.length === 0 && <tr><td colSpan="9" className="text-center text-muted py-3">Sin envíos todavía</td></tr>}
                  {registros.map((r) => (
                    <tr key={`${r.n}-${r.hora}`} title={r.detalle || ""}>
                      <td>{r.n}</td>
                      <td>{r.hora}</td>
                      <td><span className={`badge ${String(r.http).startsWith("2") ? "bg-success" : "bg-danger"}`}>{r.http}</span></td>
                      <td>{r.ms}</td>
                      <td>{r.nombre}</td>
                      <td>{r.categoriaId ?? "—"}</td>
                      <td>{r.eventId ? r.eventId.slice(0, 8) + "…" : "—"}</td>
                      <td><span className={`badge ${r.estado === "PUBLICADA" ? "bg-success" : "bg-danger"}`}>{r.estado}</span></td>
                      <td>
                        {r.estado !== "PUBLICADA" ? <span className="text-muted">—</span>
                          : consumidas.has(r.categoriaId) ? <span className="badge bg-success">Consumida</span>
                          : <span className="badge bg-warning text-dark">En cola…</span>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </AuthenticatedTemplate>
      </div>
    </>
  );
}

export default App;
