import { useCallback, useEffect, useMemo, useState } from "react";
import { Alert, DataTable, FormField } from "../../../../../packages/design/components.jsx";
import DireccionField from "../../../../../packages/address/DireccionField.jsx";
import { api } from "../../../lib/api.js";
import PuntosVentaMap from "../components/PuntosVentaMap.jsx";
import "../styles.css";

const vacio = { nombreComercio: "", minoristaId: "", departamentoId: "", localidadId: "", calle: "", numeracion: "", barrio: "", manzanaPiso: "", casaDepartamento: "", referencia: "", latitud: null, longitud: null };
export default function PuntosVentaPage() {
  const [puntos, setPuntos] = useState([]);
  const [departamentos, setDepartamentos] = useState([]);
  const [minoristas, setMinoristas] = useState([]);
  const [zona, setZona] = useState("");
  const [form, setForm] = useState(vacio);
  const [cargando, setCargando] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [aviso, setAviso] = useState(null);

  const cargar = useCallback(async (signal) => {
    setCargando(true);
    try {
      const [p, d] = await Promise.all([
        api.get("/puntos-de-venta", { params: zona ? { departamentoId: zona } : {}, signal }).then(r => r.data),
        api.get("/departamentos", { signal }).then(r => r.data),
      ]);
      if (signal?.aborted) return;
      setPuntos(p); setDepartamentos(d);
      if (!zona && d.length) setZona(d[0].id);
    } catch (e) { if (!signal?.aborted) setAviso({ tone: "danger", text: e.message }); }
    finally { if (!signal?.aborted) setCargando(false); }
  }, [zona]);

  useEffect(() => {
    const controller = new AbortController();
    queueMicrotask(() => { if (!controller.signal.aborted) void cargar(controller.signal); });
    return () => controller.abort();
  }, [cargar]);
  useEffect(() => {
    const controller = new AbortController();
    api.get("/puntos-de-venta/minoristas", { signal: controller.signal })
      .then(r => setMinoristas(r.data)).catch(e => { if (!controller.signal.aborted) setAviso({ tone: "danger", text: e.message }); });
    return () => controller.abort();
  }, []);

  async function guardar(event) {
    event.preventDefault(); setGuardando(true); setAviso(null);
    try {
      if (form.latitud == null || form.longitud == null) throw new Error("Marcá la ubicación del comercio en el mapa.");
      const datos = { ...form };
      delete datos.departamentoId;
      await api.post("/puntos-de-venta", { ...datos, latitud: Number(form.latitud), longitud: Number(form.longitud) });
      setAviso({ tone: "success", text: `Se agregó ${form.nombreComercio.trim()} al mapa de la zona.` });
      setForm({ ...vacio, departamentoId: zona });
      await cargar();
    } catch (e) { setAviso({ tone: "danger", text: e.message }); }
    finally { setGuardando(false); }
  }

  const puntosZona = useMemo(() => puntos.filter(p => !zona || p.idDepartamento === zona), [puntos, zona]);
  const columnas = [
    { key: "nombreComercio", label: "Comercio" },
    { key: "direccion", label: "Dirección", render: p => `${p.calle} ${p.numeracion}, ${p.localidad}` },
    { key: "minorista", label: "Minorista", render: p => p.nombreMinorista },
    { key: "mapa", label: "Ubicación", render: p => <a href={`https://www.openstreetmap.org/?mlat=${p.latitud}&mlon=${p.longitud}#map=17/${p.latitud}/${p.longitud}`} target="_blank" rel="noreferrer">Abrir mapa</a> },
  ];

  return <>
    <header className="dm-page-header"><div><h1>Puntos de venta</h1><p>Registrá comercios y consultá los puntos ubicados en cada zona.</p></div></header>
    {aviso && <div className="pv-status-layer"><Alert tone={aviso.tone} onDismiss={() => setAviso(null)}>{aviso.text}</Alert></div>}
    <section className="design-content pv-layout" aria-label="Puntos de venta por zona">
      <div className="pv-list">
        <div className="pv-zone"><label htmlFor="pv-zona">Zona</label><select id="pv-zona" className="dm-input" value={zona} onChange={e => setZona(e.target.value)}><option value="">Todas las zonas</option>{departamentos.map(d => <option key={d.id} value={d.id}>{d.nombre}</option>)}</select></div>
        <PuntosVentaMap puntos={puntosZona} />
        <DataTable caption="Comercios de la zona seleccionada" columns={columnas} rows={puntosZona} rowKey={p => p.id} empty={cargando ? "Cargando puntos de venta…" : "Todavía no hay comercios en esta zona."} />
      </div>
      <form className="design-form" onSubmit={guardar}>
        <h2>Nuevo punto de venta</h2>
        <FormField label="Nombre del comercio" name="nombreComercio" value={form.nombreComercio} required maxLength={160} onChange={e => setForm({ ...form, nombreComercio: e.target.value })} />
        <label className="dm-field"><span>Minorista responsable</span><select className="dm-input" value={form.minoristaId} onChange={e => setForm({ ...form, minoristaId: e.target.value })} required><option value="">Seleccioná un minorista</option>{minoristas.map(m => <option key={m.id} value={m.id}>{m.apellido}, {m.nombre} · DNI {m.numeroDocumento}</option>)}</select>{minoristas.length === 0 && <small>Primero debe existir un minorista para asociar al comercio.</small>}</label>
        <DireccionField api={api} value={form} onChange={value => setForm(value)} />
        <div className="dm-actions"><button className="dm-button dm-button-primary" disabled={guardando || cargando || !minoristas.length}>{guardando ? "Guardando…" : "Guardar punto de venta"}</button></div>
      </form>
    </section>
  </>;
}
