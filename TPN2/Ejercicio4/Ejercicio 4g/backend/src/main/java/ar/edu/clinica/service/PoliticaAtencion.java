package ar.edu.clinica.service;
import ar.edu.clinica.api.Solicitudes.Detalle;
/** Strategy: dos reglas intercambiables, sin alterar el algoritmo Template Method. */
public interface PoliticaAtencion { String tipo(); void validar(Detalle datos); }
