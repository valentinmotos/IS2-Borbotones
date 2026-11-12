import { useCallback, useEffect, useState } from "react";
import { Plus } from "lucide-react";
import {
  Alert,
  ConfirmDialog,
  DataTable,
  FormField,
} from "../../../../../packages/design/components.jsx";
import ImageUploader from "../../../components/ImageUploader.jsx";
import { api } from "../../../lib/api.js";
import { crearCrudApi } from "../../../lib/crudApi.js";
import "../styles.css";

const empresaApi = crearCrudApi("/empresas");
const sucursalApi = crearCrudApi("/sucursales");
const pagosApi = crearCrudApi("/formas-de-pago");
const etiquetasPago = {
  EFECTIVO: "Efectivo",
  TRANSFERENCIA: "Transferencia",
  BILLETERA_VIRTUAL: "Billetera virtual",
};
const sucursalVacia = {
  nombre: "",
  tipo: "SUCURSAL",
  idMayorista: "",
  idLocalidad: "",
  calle: "",
  numeracion: "",
  barrio: "",
  manzanaPiso: "",
  casaDepartamento: "",
  referencia: "",
  latitud: "-32.8895",
  longitud: "-68.8458",
  idImagen: "",
};

export default function EmpresaPage() {
  const [empresas, setEmpresas] = useState([]);
  const [sucursales, setSucursales] = useState([]);
  const [formas, setFormas] = useState([]);
  const [localidades, setLocalidades] = useState([]);
  const [empresaId, setEmpresaId] = useState("");
  const [razonSocial, setRazonSocial] = useState("");
  const [cuit, setCuit] = useState("");
  const [editandoSucursal, setEditandoSucursal] = useState(null);
  const [sucursal, setSucursal] = useState(sucursalVacia);
  const [archivo, setArchivo] = useState(null);
  const [imageVersion, setImageVersion] = useState(0);
  const [pagoEditando, setPagoEditando] = useState(null);
  const [pagoTipo, setPagoTipo] = useState("EFECTIVO");
  const [pagoObservacion, setPagoObservacion] = useState("");
  const [confirmacion, setConfirmacion] = useState(null);
  const [aviso, setAviso] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [guardando, setGuardando] = useState(false);

  const cargar = useCallback(async (signal) => {
    try {
      const [empresasResp, sucursalesResp, formasResp, localidadesResp] =
        await Promise.all([
          empresaApi.listar({ signal }),
          sucursalApi.listar({ signal }),
          api
            .get("/formas-de-pago/administracion", { signal })
            .then((r) => r.data),
          crearCrudApi("/localidades").listar({ signal }),
        ]);
      if (signal?.aborted) return;
      setEmpresas(empresasResp);
      setSucursales(sucursalesResp);
      setFormas(formasResp);
      setLocalidades(localidadesResp);
      const empresa = empresasResp[0];
      setEmpresaId(empresa?.id ?? "");
      setRazonSocial(empresa?.razonSocial ?? "");
      setCuit(empresa?.cuit ?? "");
      if (empresa)
        setSucursal((actual) =>
          actual.idMayorista ? actual : { ...actual, idMayorista: empresa.id },
        );
    } catch (error) {
      if (!signal?.aborted) setAviso({ tono: "danger", texto: error.message });
    } finally {
      if (!signal?.aborted) setCargando(false);
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    queueMicrotask(() => {
      if (!controller.signal.aborted) void cargar(controller.signal);
    });
    return () => controller.abort();
  }, [cargar]);

  async function guardarEmpresa(event) {
    event.preventDefault();
    setGuardando(true);
    try {
      const datos = { razonSocial: razonSocial.trim(), cuit: cuit.trim() };
      const guardada = empresaId
        ? await empresaApi.modificar(empresaId, datos)
        : await empresaApi.crear(datos);
      setAviso({
        tono: "success",
        texto: `Guardaste ${guardada.razonSocial}.`,
      });
      await cargar();
    } catch (error) {
      setAviso({ tono: "danger", texto: error.message });
    } finally {
      setGuardando(false);
    }
  }

  function editarSucursal(item) {
    setEditandoSucursal(item);
    setSucursal({
      nombre: item.nombre,
      tipo: item.tipo,
      idMayorista: item.idMayorista,
      idLocalidad: item.idLocalidad,
      calle: item.calle,
      numeracion: item.numeracion,
      barrio: item.barrio ?? "",
      manzanaPiso: item.manzanaPiso ?? "",
      casaDepartamento: item.casaDepartamento ?? "",
      referencia: item.referencia ?? "",
      latitud: String(item.latitud),
      longitud: String(item.longitud),
      idImagen: item.idImagen ?? "",
    });
    setArchivo(null);
  }

  function limpiarSucursal() {
    setEditandoSucursal(null);
    setSucursal({ ...sucursalVacia, idMayorista: empresaId });
    setArchivo(null);
    setImageVersion((version) => version + 1);
  }

  async function guardarSucursal(event) {
    event.preventDefault();
    setGuardando(true);
    try {
      let idImagen = sucursal.idImagen;
      if (archivo) {
        const formulario = new FormData();
        formulario.append("archivo", archivo);
        formulario.append("tipoImagen", "SUCURSAL");
        const respuesta = await api.post("/imagenes", formulario);
        idImagen = respuesta.data.id;
      }
      const datos = {
        ...sucursal,
        idImagen,
        latitud: Number(sucursal.latitud),
        longitud: Number(sucursal.longitud),
      };
      const guardada = editandoSucursal
        ? await sucursalApi.modificar(editandoSucursal.id, datos)
        : await sucursalApi.crear(datos);
      setAviso({ tono: "success", texto: `Guardaste ${guardada.nombre}.` });
      limpiarSucursal();
      await cargar();
    } catch (error) {
      setAviso({ tono: "danger", texto: error.message });
    } finally {
      setGuardando(false);
    }
  }

  async function guardarForma(event) {
    event.preventDefault();
    setGuardando(true);
    try {
      const datos = {
        tipoPago: pagoTipo,
        observacion: pagoObservacion.trim() || null,
      };
      await (pagoEditando
        ? pagosApi.modificar(pagoEditando.id, datos)
        : pagosApi.crear(datos));
      setPagoEditando(null);
      setPagoTipo("EFECTIVO");
      setPagoObservacion("");
      setAviso({ tono: "success", texto: "La forma de pago quedó guardada." });
      await cargar();
    } catch (error) {
      setAviso({ tono: "danger", texto: error.message });
    } finally {
      setGuardando(false);
    }
  }

  async function cambiarEstadoForma(forma) {
    try {
      if (forma.activo) await pagosApi.eliminar(forma.id);
      else await api.patch(`/formas-de-pago/${forma.id}/activar`);
      setAviso({
        tono: "success",
        texto: `${etiquetasPago[forma.tipoPago]} ${forma.activo ? "desactivada" : "activada"}.`,
      });
      await cargar();
    } catch (error) {
      setAviso({ tono: "danger", texto: error.message });
    }
  }

  async function confirmarBaja() {
    const { api: recurso, item, etiqueta, tipo } = confirmacion;
    setConfirmacion(null);
    try {
      await recurso.eliminar(item.id);
      if (item.id === editandoSucursal?.id) limpiarSucursal();
      if (tipo === "empresa") {
        setEmpresaId("");
        setRazonSocial("");
        setCuit("");
        setSucursal((actual) => ({ ...actual, idMayorista: "" }));
      }
      setAviso({ tono: "success", texto: `Diste de baja ${etiqueta}.` });
      await cargar();
    } catch (error) {
      setAviso({ tono: "danger", texto: error.message });
    }
  }

  const sucursalColumnas = [
    { key: "nombre", label: "Sucursal" },
    {
      key: "tipo",
      label: "Tipo",
      render: (item) =>
        item.tipo === "SEDE_CENTRAL" ? "Sede central" : "Sucursal",
    },
    {
      key: "direccion",
      label: "Dirección",
      render: (item) => `${item.calle} ${item.numeracion}, ${item.localidad}`,
    },
    {
      key: "foto",
      label: "Foto",
      render: (item) =>
        item.urlImagen ? (
          <a href={item.urlImagen} target="_blank" rel="noreferrer">
            Ver foto
          </a>
        ) : (
          "Sin foto"
        ),
    },
    {
      key: "ubicacion",
      label: "Mapa",
      render: (item) => (
        <a
          href={`https://www.openstreetmap.org/?mlat=${item.latitud}&mlon=${item.longitud}#map=16/${item.latitud}/${item.longitud}`}
          target="_blank"
          rel="noreferrer"
        >
          Ver ubicación
        </a>
      ),
    },
    {
      key: "acciones",
      label: "Acciones",
      render: (item) => (
        <div className="abm-row-actions">
          <button
            className="dm-button"
            type="button"
            onClick={() => editarSucursal(item)}
          >
            Editar
          </button>
          <button
            className="dm-button dm-button-danger"
            type="button"
            onClick={() =>
              setConfirmacion({
                api: sucursalApi,
                item,
                etiqueta: `«${item.nombre}»`,
              })
            }
          >
            Dar de baja
          </button>
        </div>
      ),
    },
  ];
  const pagoColumnas = [
    {
      key: "tipoPago",
      label: "Forma de pago",
      render: (item) => etiquetasPago[item.tipoPago],
    },
    {
      key: "observacion",
      label: "Alias o detalle",
      render: (item) => item.observacion || "—",
    },
    {
      key: "estado",
      label: "Estado",
      render: (item) => (item.activo ? "Activa" : "Inactiva"),
    },
    {
      key: "acciones",
      label: "Acciones",
      render: (item) => (
        <div className="abm-row-actions">
          {item.activo && (
            <button
              className="dm-button"
              type="button"
              onClick={() => {
                setPagoEditando(item);
                setPagoTipo(item.tipoPago);
                setPagoObservacion(item.observacion ?? "");
              }}
            >
              Editar
            </button>
          )}
          <button
            className={`dm-button ${item.activo ? "dm-button-danger" : "dm-button-primary"}`}
            type="button"
            onClick={() => cambiarEstadoForma(item)}
          >
            {item.activo ? "Desactivar" : "Activar"}
          </button>
        </div>
      ),
    },
  ];

  const coordenadas = `${Number(sucursal.latitud) || 0},${Number(sucursal.longitud) || 0}`;
  const [lat, lon] = coordenadas.split(",");
  const margen = 0.008;
  const mapa = `https://www.openstreetmap.org/export/embed.html?bbox=${Number(lon) - margen}%2C${Number(lat) - margen}%2C${Number(lon) + margen}%2C${Number(lat) + margen}&layer=mapnik&marker=${lat}%2C${lon}`;

  return (
    <div className="empresa-page">
      <header className="page-head">
        <div>
          <span className="eyebrow">Configuración</span>
          <h1>Empresa y sucursales</h1>
          <p className="lead">
            Administrá los datos institucionales, las sucursales con su
            ubicación y los medios de pago disponibles.
          </p>
        </div>
        <div className="page-head-actions">
          <a
            className="dm-button dm-button-primary"
            href="#sucursal-formulario"
          >
            <Plus size={16} aria-hidden="true" />
            Nueva sucursal
          </a>
        </div>
      </header>
      {aviso && (
        <Alert tone={aviso.tono} onDismiss={() => setAviso(null)}>
          {aviso.texto}
        </Alert>
      )}
      {cargando && (
        <p className="muted" role="status">
          Cargando información…
        </p>
      )}

      <form
        className="panel"
        onSubmit={guardarEmpresa}
        noValidate
        aria-labelledby="empresa-titulo"
      >
        <div className="panel-head">
          <h2 id="empresa-titulo">Datos de la empresa</h2>
          <p>Razón social y CUIT de la empresa mayorista.</p>
        </div>
        <div className="form-grid empresa-datos">
          <FormField
            label="Razón social"
            name="razonSocial"
            value={razonSocial}
            maxLength={160}
            onChange={(e) => setRazonSocial(e.target.value)}
          />
          <FormField
            label="CUIT"
            name="cuit"
            hint="Ingresalo con o sin guiones."
            value={cuit}
            maxLength={13}
            onChange={(e) => setCuit(e.target.value)}
          />
          <div className="form-actions">
            <button
              className="dm-button dm-button-primary"
              disabled={guardando}
            >
              {guardando ? "Guardando…" : "Guardar empresa"}
            </button>
            {empresaId && (
              <button
                className="dm-button dm-button-danger"
                type="button"
                onClick={() =>
                  setConfirmacion({
                    api: empresaApi,
                    item: { id: empresaId },
                    etiqueta: `la empresa «${razonSocial}»`,
                    tipo: "empresa",
                  })
                }
              >
                Dar de baja empresa
              </button>
            )}
          </div>
        </div>
      </form>

      <section
        id="sucursales"
        className="panel"
        aria-labelledby="sucursales-titulo"
      >
        <div className="panel-head">
          <h2 id="sucursales-titulo">Sucursales</h2>
          {!cargando && (
            <p>
              {sucursales.length === 1
                ? "1 sucursal activa"
                : `${sucursales.length} sucursales activas`}
            </p>
          )}
        </div>
        <DataTable
          caption="Sucursales activas"
          columns={sucursalColumnas}
          rows={sucursales}
          rowKey={(item) => item.id}
          empty={
            cargando
              ? "Cargando sucursales…"
              : "Todavía no hay sucursales. Cargá la primera con el formulario de abajo."
          }
        />
      </section>

      <form
        id="sucursal-formulario"
        className="panel"
        onSubmit={guardarSucursal}
        noValidate
        aria-labelledby="sucursal-formulario-titulo"
      >
        <div className="panel-head">
          <h2 id="sucursal-formulario-titulo">
            {editandoSucursal
              ? `Editando: ${editandoSucursal.nombre}`
              : "Nueva sucursal"}
          </h2>
          <p>
            {editandoSucursal
              ? "Modificá los datos y guardá los cambios."
              : "Completá los datos, la dirección y la ubicación en el mapa."}
          </p>
        </div>
        <div className="empresa-bloques">
          <fieldset className="empresa-bloque">
            <legend>Datos generales</legend>
            <div className="form-grid">
              <FormField
                label="Nombre"
                name="nombre"
                value={sucursal.nombre}
                maxLength={120}
                onChange={(e) =>
                  setSucursal({ ...sucursal, nombre: e.target.value })
                }
              />
              <label className="dm-field">
                <span>Tipo</span>
                <select
                  className="dm-input"
                  value={sucursal.tipo}
                  onChange={(e) =>
                    setSucursal({ ...sucursal, tipo: e.target.value })
                  }
                >
                  <option value="SEDE_CENTRAL">Sede central</option>
                  <option value="SUCURSAL">Sucursal</option>
                </select>
              </label>
              <label className="dm-field">
                <span>Empresa</span>
                <select
                  className="dm-input"
                  value={sucursal.idMayorista}
                  onChange={(e) =>
                    setSucursal({ ...sucursal, idMayorista: e.target.value })
                  }
                  required
                >
                  <option value="">Seleccioná la empresa</option>
                  {empresas.map((e) => (
                    <option value={e.id} key={e.id}>
                      {e.razonSocial}
                    </option>
                  ))}
                </select>
              </label>
            </div>
          </fieldset>
          <fieldset className="empresa-bloque">
            <legend>Dirección</legend>
            <div className="form-grid">
              <label className="dm-field">
                <span>Localidad</span>
                <select
                  className="dm-input"
                  value={sucursal.idLocalidad}
                  onChange={(e) =>
                    setSucursal({ ...sucursal, idLocalidad: e.target.value })
                  }
                  required
                >
                  <option value="">Seleccioná la localidad</option>
                  {localidades.map((l) => (
                    <option value={l.id} key={l.id}>
                      {l.nombre} · {l.departamentoNombre}
                    </option>
                  ))}
                </select>
              </label>
              <FormField
                label="Calle"
                name="calle"
                value={sucursal.calle}
                maxLength={160}
                onChange={(e) =>
                  setSucursal({ ...sucursal, calle: e.target.value })
                }
              />
              <FormField
                label="Numeración"
                name="numeracion"
                value={sucursal.numeracion}
                maxLength={30}
                onChange={(e) =>
                  setSucursal({ ...sucursal, numeracion: e.target.value })
                }
              />
              <FormField
                label="Barrio"
                name="barrio"
                value={sucursal.barrio}
                maxLength={120}
                onChange={(e) =>
                  setSucursal({ ...sucursal, barrio: e.target.value })
                }
              />
              <FormField
                label="Piso o manzana"
                name="manzanaPiso"
                value={sucursal.manzanaPiso}
                maxLength={80}
                onChange={(e) =>
                  setSucursal({ ...sucursal, manzanaPiso: e.target.value })
                }
              />
              <FormField
                label="Casa o departamento"
                name="casaDepartamento"
                value={sucursal.casaDepartamento}
                maxLength={80}
                onChange={(e) =>
                  setSucursal({ ...sucursal, casaDepartamento: e.target.value })
                }
              />
              <div className="span-all">
                <FormField
                  label="Referencia"
                  name="referencia"
                  value={sucursal.referencia}
                  maxLength={500}
                  onChange={(e) =>
                    setSucursal({ ...sucursal, referencia: e.target.value })
                  }
                />
              </div>
            </div>
          </fieldset>
          <fieldset className="empresa-bloque">
            <legend>Ubicación y foto</legend>
            <div className="empresa-media">
              <div className="empresa-mapa">
                <div className="form-grid">
                  <FormField
                    label="Latitud"
                    name="latitud"
                    type="number"
                    step="any"
                    value={sucursal.latitud}
                    onChange={(e) =>
                      setSucursal({ ...sucursal, latitud: e.target.value })
                    }
                  />
                  <FormField
                    label="Longitud"
                    name="longitud"
                    type="number"
                    step="any"
                    value={sucursal.longitud}
                    onChange={(e) =>
                      setSucursal({ ...sucursal, longitud: e.target.value })
                    }
                  />
                </div>
                <iframe
                  className="empresa-mapa-vista"
                  title="Vista previa de la ubicación de la sucursal"
                  src={mapa}
                  loading="lazy"
                />
                <p className="muted small">
                  El mapa se actualiza cuando cambiás la latitud o la longitud.
                </p>
              </div>
              <ImageUploader
                key={`${editandoSucursal?.id ?? "nueva-sucursal"}-${imageVersion}`}
                label="Foto de la sucursal"
                onFileSelected={setArchivo}
                imageUrl={
                  editandoSucursal && sucursal.idImagen
                    ? `/api/imagenes/${sucursal.idImagen}`
                    : ""
                }
              />
            </div>
          </fieldset>
        </div>
        <div className="form-actions empresa-form-pie">
          <button
            className="dm-button dm-button-primary"
            disabled={guardando || !empresas.length || !localidades.length}
          >
            {guardando ? "Guardando…" : "Guardar sucursal"}
          </button>
          {editandoSucursal && (
            <button
              className="dm-button"
              type="button"
              onClick={limpiarSucursal}
            >
              Cancelar
            </button>
          )}
        </div>
      </form>

      <section className="design-content" aria-labelledby="pagos-titulo">
        <div className="design-register">
          <h2 id="pagos-titulo">Formas de pago</h2>
          <DataTable
            caption="Formas de pago y estado"
            columns={pagoColumnas}
            rows={formas}
            rowKey={(item) => item.id}
            empty={
              cargando
                ? "Cargando formas de pago…"
                : "No hay formas de pago. Agregá una con el formulario."
            }
          />
        </div>
        <form className="design-form" onSubmit={guardarForma} noValidate>
          <h2>
            {pagoEditando ? "Editar forma de pago" : "Agregar forma de pago"}
          </h2>
          <label className="dm-field">
            <span>Tipo</span>
            <select
              className="dm-input"
              value={pagoTipo}
              onChange={(e) => setPagoTipo(e.target.value)}
            >
              <option value="EFECTIVO">Efectivo</option>
              <option value="TRANSFERENCIA">Transferencia</option>
              <option value="BILLETERA_VIRTUAL">Billetera virtual</option>
            </select>
          </label>
          <FormField
            label={
              pagoTipo === "TRANSFERENCIA"
                ? "Alias bancario"
                : "Detalle (opcional)"
            }
            name="observacion"
            value={pagoObservacion}
            maxLength={255}
            onChange={(e) => setPagoObservacion(e.target.value)}
          />
          <div className="dm-actions">
            <button
              className="dm-button dm-button-primary"
              disabled={guardando}
            >
              {guardando ? "Guardando…" : "Guardar forma de pago"}
            </button>
            {pagoEditando && (
              <button
                className="dm-button"
                type="button"
                onClick={() => {
                  setPagoEditando(null);
                  setPagoTipo("EFECTIVO");
                  setPagoObservacion("");
                }}
              >
                Cancelar
              </button>
            )}
          </div>
        </form>
      </section>

      <ConfirmDialog
        open={confirmacion !== null}
        title={
          confirmacion?.tipo === "empresa"
            ? "Dar de baja empresa"
            : "Dar de baja sucursal"
        }
        confirmLabel="Dar de baja"
        onConfirm={confirmarBaja}
        onCancel={() => setConfirmacion(null)}
      >
        <p>¿Querés dar de baja {confirmacion?.etiqueta}?</p>
      </ConfirmDialog>
    </div>
  );
}
