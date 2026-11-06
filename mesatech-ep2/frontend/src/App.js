import { useCallback, useEffect, useMemo, useState } from "react";
import { AuthenticatedTemplate, UnauthenticatedTemplate, useMsal } from "@azure/msal-react";
import { loginRequest } from "./authConfig";
import { crearCliente, leerClaims, mensajeError } from "./api";

const ESTADOS = ["EN_PROCESO", "RESUELTA", "CERRADA"];

function App() {
  const { instance, accounts } = useMsal();
  const cuenta = accounts[0];
  const api = useMemo(() => (cuenta ? crearCliente(instance, cuenta) : null), [instance, cuenta]);

  const [claims, setClaims] = useState(null);
  const [categoriasCatalogo, setCategoriasCatalogo] = useState([]);
  const [categoriasLocales, setCategoriasLocales] = useState([]);
  const [solicitudes, setSolicitudes] = useState([]);
  const [version, setVersion] = useState("v2");
  const [mensaje, setMensaje] = useState(null);

  const [nuevaCategoria, setNuevaCategoria] = useState({ nombre: "", descripcion: "" });
  const [nuevaSolicitud, setNuevaSolicitud] = useState({ titulo: "", descripcion: "", categoriaId: "" });

  const esAdmin = (claims?.roles || []).includes("Admin");
  const idsLocales = new Set(categoriasLocales.map((c) => c.categoriaId));

  const avisar = (tipo, texto) => setMensaje({ tipo, texto });

  const cargarCatalogo = useCallback(async () => {
    if (!api) return;
    try {
      const [cat, loc] = await Promise.all([
        api.get("/api/v1/catalogo/categorias"),
        api.get("/api/v1/solicitudes/categorias"),
      ]);
      setCategoriasCatalogo(cat.data);
      setCategoriasLocales(loc.data);
    } catch (e) {
      avisar("danger", "Error cargando categorías: " + mensajeError(e));
    }
  }, [api]);

  const cargarSolicitudes = useCallback(async () => {
    if (!api) return;
    try {
      const r = await api.get(`/api/${version}/solicitudes`);
      setSolicitudes(r.data);
    } catch (e) {
      avisar("danger", "Error cargando solicitudes: " + mensajeError(e));
    }
  }, [api, version]);

  useEffect(() => {
    if (!cuenta) return;
    leerClaims(instance, cuenta).then(setClaims).catch((e) => avisar("danger", "No fue posible obtener el access token: " + e.message));
  }, [instance, cuenta]);

  useEffect(() => { cargarCatalogo(); }, [cargarCatalogo]);
  useEffect(() => { cargarSolicitudes(); }, [cargarSolicitudes]);

  // Mientras haya categorías aún no sincronizadas, se consulta la copia local cada 3 s (procesamiento asíncrono)
  const pendientes = categoriasCatalogo.filter((c) => !idsLocales.has(c.id)).length;
  useEffect(() => {
    if (!api || pendientes === 0) return;
    const t = setInterval(cargarCatalogo, 3000);
    return () => clearInterval(t);
  }, [api, pendientes, cargarCatalogo]);

  const crearCategoria = async (e) => {
    e.preventDefault();
    try {
      const r = await api.post("/api/v1/catalogo/categorias", nuevaCategoria);
      avisar("success", `Categoría #${r.data.id} guardada en Catálogo y evento publicado (eventId ${r.data.eventId}). ` +
        "Aparecerá como disponible cuando Solicitudes consuma el mensaje.");
      setNuevaCategoria({ nombre: "", descripcion: "" });
    } catch (err) {
      const d = err.response?.data;
      if (err.response?.status === 503 && d?.categoriaId) {
        avisar("warning", `${d.mensaje} (categoriaId ${d.categoriaId}, eventId ${d.eventId})`);
      } else {
        avisar("danger", "No se creó la categoría: " + mensajeError(err));
      }
    }
    cargarCatalogo();
  };

  const crearSolicitud = async (e) => {
    e.preventDefault();
    try {
      const r = await api.post(`/api/${version}/solicitudes`, {
        ...nuevaSolicitud,
        categoriaId: Number(nuevaSolicitud.categoriaId),
      });
      avisar("success", `Solicitud #${r.data.id} registrada en RDS Solicitudes con categoría '${r.data.categoriaNombre}'.`);
      setNuevaSolicitud({ titulo: "", descripcion: "", categoriaId: "" });
    } catch (err) {
      avisar("danger", "No se creó la solicitud: " + mensajeError(err));
    }
    cargarSolicitudes();
  };

  const cambiarEstado = async (id, estado) => {
    try {
      await api.patch(`/api/${version}/solicitudes/${id}/estado`, { estado });
      avisar("success", `Solicitud #${id} cambió a ${estado}.`);
    } catch (err) {
      avisar("danger", `No se cambió el estado de #${id}: ` + mensajeError(err));
    }
    cargarSolicitudes();
  };

  return (
    <div className="container py-4">
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h1 className="h3 mb-0">MesaTech Cloud</h1>
        <AuthenticatedTemplate>
          <button className="btn btn-outline-danger btn-sm" onClick={() => instance.logoutRedirect()}>Cerrar sesión</button>
        </AuthenticatedTemplate>
      </div>

      <UnauthenticatedTemplate>
        <div className="alert alert-secondary">Inicie sesión con su cuenta de Microsoft Entra ID.</div>
        <button className="btn btn-primary" onClick={() => instance.loginRedirect(loginRequest)}>Iniciar sesión</button>
      </UnauthenticatedTemplate>

      <AuthenticatedTemplate>
        {cuenta && (
          <p className="text-muted small">
            {cuenta.name} · {cuenta.username} · Roles: <strong>{(claims?.roles || ["(sin rol)"]).join(", ")}</strong> · Scope: {claims?.scp}
          </p>
        )}

        {mensaje && (
          <div className={`alert alert-${mensaje.tipo} alert-dismissible`}>
            {mensaje.texto}
            <button type="button" className="btn-close" onClick={() => setMensaje(null)} />
          </div>
        )}

        {/* ---------------- CATÁLOGO ---------------- */}
        <div className="card mb-4">
          <div className="card-header d-flex justify-content-between">
            <strong>Categorías de atención (Catálogo)</strong>
            <button className="btn btn-sm btn-outline-secondary" onClick={cargarCatalogo}>Actualizar</button>
          </div>
          <div className="card-body">
            {esAdmin ? (
              <form className="row g-2 mb-3" onSubmit={crearCategoria}>
                <div className="col-md-4">
                  <input className="form-control" placeholder="Nombre" required value={nuevaCategoria.nombre}
                    onChange={(e) => setNuevaCategoria({ ...nuevaCategoria, nombre: e.target.value })} />
                </div>
                <div className="col-md-6">
                  <input className="form-control" placeholder="Descripción" value={nuevaCategoria.descripcion}
                    onChange={(e) => setNuevaCategoria({ ...nuevaCategoria, descripcion: e.target.value })} />
                </div>
                <div className="col-md-2 d-grid">
                  <button className="btn btn-primary">Crear categoría</button>
                </div>
              </form>
            ) : (
              <p className="small text-muted">Solo usuarios con rol Admin pueden crear categorías.</p>
            )}

            <table className="table table-sm align-middle">
              <thead>
                <tr><th>ID</th><th>Nombre</th><th>Publicación</th><th>eventId</th><th>Disponible en Solicitudes</th></tr>
              </thead>
              <tbody>
                {categoriasCatalogo.map((c) => (
                  <tr key={c.id}>
                    <td>{c.id}</td>
                    <td>{c.nombre}</td>
                    <td><span className={`badge bg-${c.estadoPublicacion === "PUBLICADA" ? "success" : "danger"}`}>{c.estadoPublicacion}</span></td>
                    <td className="small text-muted">{c.eventId}</td>
                    <td>
                      {idsLocales.has(c.id)
                        ? <span className="badge bg-success">Disponible</span>
                        : <span className="badge bg-warning text-dark">Pendiente de sincronizar…</span>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* ---------------- SOLICITUDES ---------------- */}
        <div className="card">
          <div className="card-header d-flex justify-content-between align-items-center">
            <strong>Solicitudes</strong>
            <div className="btn-group btn-group-sm">
              {["v1", "v2"].map((v) => (
                <button key={v} className={`btn btn-${version === v ? "dark" : "outline-dark"}`} onClick={() => setVersion(v)}>API {v}</button>
              ))}
            </div>
          </div>
          <div className="card-body">
            <form className="row g-2 mb-3" onSubmit={crearSolicitud}>
              <div className="col-md-3">
                <input className="form-control" placeholder="Título" required value={nuevaSolicitud.titulo}
                  onChange={(e) => setNuevaSolicitud({ ...nuevaSolicitud, titulo: e.target.value })} />
              </div>
              <div className="col-md-4">
                <input className="form-control" placeholder="Descripción" required value={nuevaSolicitud.descripcion}
                  onChange={(e) => setNuevaSolicitud({ ...nuevaSolicitud, descripcion: e.target.value })} />
              </div>
              <div className="col-md-3">
                <select className="form-select" required value={nuevaSolicitud.categoriaId}
                  onChange={(e) => setNuevaSolicitud({ ...nuevaSolicitud, categoriaId: e.target.value })}>
                  <option value="">Categoría (copia local)…</option>
                  {categoriasLocales.map((c) => <option key={c.categoriaId} value={c.categoriaId}>{c.categoriaId} · {c.nombre}</option>)}
                </select>
              </div>
              <div className="col-md-2 d-grid">
                <button className="btn btn-primary">Registrar</button>
              </div>
            </form>

            <table className="table table-sm align-middle">
              <thead>
                <tr>
                  <th>ID</th><th>Título</th><th>Categoría</th><th>Estado</th>
                  {version === "v2" && <><th>Solicitante</th><th>Creada</th></>}
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {solicitudes.map((s) => (
                  <tr key={s.id}>
                    <td>{s.id}</td>
                    <td>{s.titulo}</td>
                    <td>{version === "v2" ? `${s.categoriaId} · ${s.categoriaNombre}` : s.categoriaId}</td>
                    <td><span className="badge bg-secondary">{s.estado}</span></td>
                    {version === "v2" && <><td className="small">{s.solicitante}</td><td className="small">{s.fechaCreacion?.replace("T", " ").slice(0, 19)}</td></>}
                    <td>
                      {ESTADOS.map((e) => (
                        <button key={e} className="btn btn-outline-primary btn-sm me-1" onClick={() => cambiarEstado(s.id, e)}>{e}</button>
                      ))}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <p className="small text-muted mb-0">
              Los botones de estado requieren rol Admin. Resolver una solicitud que no está EN_PROCESO se rechaza (regla de la EP1).
            </p>
          </div>
        </div>
      </AuthenticatedTemplate>
    </div>
  );
}

export default App;
