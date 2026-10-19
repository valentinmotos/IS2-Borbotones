import React, { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { request } from './api';
import './consultorio.css';

const titles = { historias: 'Historias clínicas', pacientes: 'Pacientes', medicos: 'Médicos', usuarios: 'Usuarios' };
const today = () => { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`; };
const fullName = p => `${p.nombre} ${p.apellido}`;

function Field({ label, name, value, onChange, children, ...props }) {
  const id = `field-${name}`;
  return <div className="mb-3"><label className="form-label" htmlFor={id}>{label}</label>{children ? <select id={id} className="form-select" name={name} value={value ?? ''} onChange={onChange} {...props}>{children}</select> : <input id={id} className="form-control" name={name} value={value ?? ''} onChange={onChange} {...props}/>}</div>;
}

function Photo({ paciente, auth }) {
  const [src, setSrc] = useState(null);
  useEffect(() => {
    let alive = true, url;
    if (paciente.foto) request(`/pacientes/${paciente.id}/foto`, {image:true}, auth).then(blob => {
      url=URL.createObjectURL(blob); if(alive) setSrc(url); else URL.revokeObjectURL(url);
    }).catch(() => setSrc(null));
    return () => { alive=false; if(url) URL.revokeObjectURL(url); };
  }, [paciente.id, paciente.foto?.id, auth]);
  return src ? <img className="avatar" src={src} alt={`Foto de ${fullName(paciente)}`}/> : null;
}

function CatalogForm({ vista, edit, data, save, cancel, pending }) {
  const [values, setValues] = useState(vista==='historias' ? { pacienteId:edit.paciente?.id||'', usuarioId:edit.usuario?.id||'' } : { ...edit, clave:'' });
  const change=e=>setValues({...values,[e.target.name]:e.target.value});
  const submit=e=>{e.preventDefault(); const body= vista==='historias' ? values : vista==='usuarios' ? {nombreUsuario:values.nombreUsuario,clave:values.clave||null} : {nombre:values.nombre,apellido:values.apellido,documento:values.documento}; save(body);};
  return <section className="card panel"><h2>{edit.id?'Editar registro':'Nuevo registro'}</h2><form onSubmit={submit}>
    {['pacientes','medicos'].includes(vista) && ['nombre','apellido','documento'].map(name=><Field key={name} label={name.charAt(0).toUpperCase()+name.slice(1)} name={name} value={values[name]} onChange={change} required maxLength={name==='documento'?30:100}/>)}
    {vista==='usuarios' && <><Field label="Nombre de usuario" name="nombreUsuario" value={values.nombreUsuario} onChange={change} required maxLength={80}/><Field label="Contraseña" name="clave" value={values.clave} onChange={change} type="password" minLength={8} maxLength={100} required={!edit.id} autoComplete="new-password"/><p className="small text-muted">Mínimo 8 caracteres. Al editar, dejá vacío para conservarla.</p></>}
    {vista==='historias' && <><Field label="Paciente" name="pacienteId" value={values.pacienteId} onChange={change} required><option value="">Seleccionar paciente</option>{data.pacientes.map(p=><option key={p.id} value={p.id}>{fullName(p)} · {p.documento}</option>)}</Field><Field label="Usuario responsable" name="usuarioId" value={values.usuarioId} onChange={change} required><option value="">Seleccionar responsable</option>{data.usuarios.map(u=><option key={u.id} value={u.id}>{u.nombreUsuario}</option>)}</Field></>}
    <div className="actions"><button className="btn btn-primary" disabled={pending}>Guardar registro</button><button type="button" className="btn btn-light" onClick={cancel}>Cancelar</button></div>
  </form></section>;
}

function DetailForm({ edit, medicos, save, pending }) {
  const [values,setValues]=useState({ fechaHistoria:edit.fechaHistoria||today(), detalleHistoria:edit.detalleHistoria||'', medicoId:edit.medico?.id||'', tipo:'GENERAL' });
  const change=e=>setValues({...values,[e.target.name]:e.target.value});
  return <form onSubmit={e=>{e.preventDefault();save(values,edit.id);}}><h3>{edit.id?'Editar atención':'Registrar atención'}</h3>
    <Field label="Fecha" name="fechaHistoria" value={values.fechaHistoria} onChange={change} type="date" max={today()} required/>
    <Field label="Médico" name="medicoId" value={values.medicoId} onChange={change} required><option value="">Seleccionar médico</option>{medicos.map(m=><option key={m.id} value={m.id}>{fullName(m)}</option>)}</Field>
    <Field label="Tipo de atención" name="tipo" value={values.tipo} onChange={change}><option value="GENERAL">Consulta general · mínimo 10 caracteres</option><option value="CONTROL">Control · mínimo 20 caracteres</option></Field>
    <label className="form-label" htmlFor="detalleHistoria">Detalle y evolución</label><textarea id="detalleHistoria" className="form-control mb-3" name="detalleHistoria" value={values.detalleHistoria} onChange={change} rows={5} minLength={values.tipo==='CONTROL'?20:10} maxLength={10000} required/>
    <button className="btn btn-primary" disabled={pending}>Guardar atención</button>
  </form>;
}

function App() {
  const [auth,setAuth]=useState(null), [login,setLogin]=useState({usuario:'',clave:''});
  const [data,setData]=useState({pacientes:[],medicos:[],usuarios:[],historias:[]});
  const [vista,setVista]=useState('historias'), [edit,setEdit]=useState({}), [formKey,setFormKey]=useState(0);
  const [exp,setExp]=useState(null), [detail,setDetail]=useState({}), [detailKey,setDetailKey]=useState(0);
  const [error,setError]=useState(''), [success,setSuccess]=useState(''), [pending,setPending]=useState(false);
  const api=(path,options={})=>request(path,options,auth);
  async function refresh(selected=exp?.historia.id) {
    const pairs=await Promise.all(Object.keys(titles).map(async r=>[r,await api(`/${r}`)]));
    const next=Object.fromEntries(pairs); setData(next);
    if(selected && next.historias.some(h=>h.id===selected)) setExp(await api(`/historias/${selected}`)); else setExp(null);
  }
  useEffect(()=>{if(auth) { setPending(true); refresh().catch(e=>setError(e.message)).finally(()=>setPending(false)); }},[auth]);
  async function run(operation, message) {
    setError('');setSuccess('');setPending(true);
    try { await operation(); await refresh(); setSuccess(message); } catch(e) {setError(e.message);} finally {setPending(false);}
  }
  function resetForm(){setEdit({});setFormKey(k=>k+1);}
  async function enter(e) {
    e.preventDefault(); setError('');setPending(true);
    try {await request('/usuarios',{},login);setAuth({...login});setLogin({usuario:'',clave:''});} catch(e){setError(e.message);} finally{setPending(false);}
  }
  const footer=<footer>Consultorio · Ejercicio 4g · Interfaz React · Template gratuito <a href="https://bootswatch.com/flatly/">Flatly / Bootswatch</a> (MIT)</footer>;
  if(!auth) return <div className="login-page"><main className="login-card card p-4"><span className="brand-mark">+</span><p className="eyebrow">CONSULTORIO MÉDICO</p><h1>Tu espacio de atención</h1><p className="text-muted">Historias clínicas organizadas, seguimiento cercano.</p>{error&&<div role="alert" className="alert alert-danger">{error}</div>}<form onSubmit={enter}><Field label="Usuario" name="usuario" value={login.usuario} onChange={e=>setLogin({...login,usuario:e.target.value})} required autoComplete="username"/><Field label="Contraseña" name="clave" value={login.clave} onChange={e=>setLogin({...login,clave:e.target.value})} type="password" required autoComplete="current-password"/><button className="btn btn-primary w-100" disabled={pending}>{pending?'Conectando…':'Ingresar'}</button></form><p className="small text-muted mt-3 mb-0">Demostración: demo / Consultorio123!</p>{footer}</main></div>;
  return <><aside className="sidebar"><a className="brand" href="#" onClick={e=>e.preventDefault()}><span className="brand-mark">+</span> Consultorio</a><p className="eyebrow">ESPACIO DE TRABAJO</p><nav>{Object.keys(titles).map(r=><a key={r} href={`#${r}`} className={vista===r?'active':''} onClick={()=>{setVista(r);resetForm();setExp(null);setError('');setSuccess('');}}>{titles[r]}</a>)}</nav><div className="sidebar-bottom"><p>Atención con historia.<br/><small>React + API REST</small></p><button className="btn btn-outline-light btn-sm" onClick={()=>{setAuth(null);setData({pacientes:[],medicos:[],usuarios:[],historias:[]});setExp(null);setError('');setSuccess('');resetForm();}}>Cerrar sesión</button></div></aside>
    <main className="workspace"><header className="topbar"><span>CONSULTORIO / GESTIÓN CLÍNICA</span><span className="user-chip">{auth.usuario}</span></header><div className="page-heading"><div><p className="eyebrow">CUIDADO Y SEGUIMIENTO</p><h1>{titles[vista]}</h1><p className="text-muted">Toda la información para acompañar cada consulta.</p></div><span className="badge bg-success">Consultorio activo</span></div>
    {error&&<div className="alert alert-danger" role="alert">{error}</div>}{success&&<div className="alert alert-success" role="status">{success}</div>}{pending&&<p className="small text-muted" role="status">Procesando…</p>}
    <section className="stats">{[['Pacientes activos',data.pacientes.length],['Historias abiertas',data.historias.length],['Equipo médico',data.medicos.length]].map(([label,n])=><article key={label}><span>{label}</span><strong>{n}</strong></article>)}</section>
    <div className="content-grid"><section className="card panel"><div className="section-heading"><h2>Registros activos</h2><span className="small text-muted">Las bajas conservan el historial</span></div><div className="table-responsive"><table className="table align-middle"><thead><tr><th>{vista==='historias'?'Paciente':vista==='usuarios'?'Usuario':'Nombre y apellido'}</th><th>{vista==='historias'?'Responsable':vista==='usuarios'?'Estado':'Documento'}</th><th>Acciones</th></tr></thead><tbody>{data[vista].map(r=><tr key={r.id}><td>{vista==='pacientes'&&<Photo paciente={r} auth={auth}/>} {vista==='historias'?fullName(r.paciente):vista==='usuarios'?r.nombreUsuario:fullName(r)}</td><td>{vista==='historias'?r.usuario.nombreUsuario:vista==='usuarios'?'Activo':r.documento}</td><td><div className="actions">{vista==='historias'&&<button className="btn btn-primary btn-sm" disabled={pending} onClick={async()=>{try{setExp(await api(`/historias/${r.id}`));setDetail({});setDetailKey(k=>k+1);}catch(e){setError(e.message);}}}>Abrir</button>}<button className="btn btn-light btn-sm" disabled={pending} onClick={()=>{setEdit(r);setFormKey(k=>k+1);}}>Editar</button><button className="btn btn-outline-danger btn-sm" disabled={pending} onClick={()=>{if(confirm('¿Dar de baja este registro? En historias también se darán de baja sus atenciones.'))run(()=>api(`/${vista}/${r.id}`,{method:'DELETE'}),'Se realizó la baja lógica');}}>Baja</button></div></td></tr>)}</tbody></table>{!data[vista].length&&<p className="empty">Todavía no hay registros. Completá el formulario para comenzar.</p>}</div></section>
    <CatalogForm key={`${vista}-${formKey}`} vista={vista} edit={edit} data={data} pending={pending} cancel={resetForm} save={body=>run(async()=>{await api(`/${vista}${edit.id?`/${edit.id}`:''}`,{method:edit.id?'PUT':'POST',body});resetForm();},'Registro guardado')}/></div>
    {vista==='pacientes'&&<section className="card panel mt-4"><h2>Fotos de pacientes</h2><p className="text-muted">PNG o JPEG · hasta 2 MB</p>{data.pacientes.map(p=><div className="photo-row" key={p.id}><strong>{fullName(p)}</strong><form onSubmit={e=>{e.preventDefault();const form=e.currentTarget;const body=new FormData(form);run(async()=>{await api(`/pacientes/${p.id}/foto`,{method:'POST',body});form.reset();},'Foto actualizada');}}><label className="visually-hidden" htmlFor={`foto-${p.id}`}>Seleccionar foto de {fullName(p)}</label><input id={`foto-${p.id}`} className="form-control form-control-sm" type="file" name="archivo" accept="image/png,image/jpeg" required/><button className="btn btn-primary btn-sm" disabled={pending}>Subir foto</button></form>{p.foto&&<button className="btn btn-outline-danger btn-sm" disabled={pending} onClick={()=>run(()=>api(`/pacientes/${p.id}/foto`,{method:'DELETE'}),'Foto eliminada')}>Quitar foto</button>}</div>)}</section>}
    {exp&&<section className="card panel mt-4"><div className="section-heading"><div><p className="eyebrow">EXPEDIENTE DEL PACIENTE</p><h2>{fullName(exp.historia.paciente)}</h2><p className="text-muted">Documento {exp.historia.paciente.documento} · {exp.totalAtenciones} atenciones</p></div><button className="btn btn-light" onClick={()=>setExp(null)}>Cerrar expediente</button></div><div className="content-grid"><div>{exp.atenciones.map(d=><article className="timeline-item" key={d.id}><span className="badge bg-light text-primary">{d.fechaHistoria}</span><h3 className="mt-2">Dr/a. {fullName(d.medico)}</h3><p className="clinical-note">{d.detalleHistoria}</p><div className="actions"><button className="btn btn-light btn-sm" disabled={pending} onClick={()=>{setDetail(d);setDetailKey(k=>k+1);}}>Editar atención</button><button className="btn btn-outline-danger btn-sm" disabled={pending} onClick={()=>{if(confirm('¿Dar de baja esta atención?'))run(()=>api(`/historias/${exp.historia.id}/detalles/${d.id}`,{method:'DELETE'}),'Atención eliminada');}}>Baja</button></div></article>)}{!exp.atenciones.length&&<p className="empty">Sin atenciones registradas.</p>}</div><DetailForm key={`${exp.historia.id}-${detailKey}`} edit={detail} medicos={data.medicos} pending={pending} save={(body,id)=>run(async()=>{await api(`/historias/${exp.historia.id}/detalles${id?`/${id}`:''}`,{method:id?'PUT':'POST',body});setDetail({});setDetailKey(k=>k+1);},'Atención guardada')}/></div></section>}{footer}</main></>;
}

createRoot(document.getElementById('root')).render(<App/>);
