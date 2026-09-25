/*
 * Selects en cascada país → provincia → departamento → localidad (fragments/ubicacion :: cascada).
 * Cada contenedor [data-ubicacion-cascada] tiene sus selects marcados con data-ubicacion y, al editar,
 * data-seleccionado con el id a precargar. Las opciones salen de los endpoints de /api/ubicacion.
 * Si el formulario tiene un campo [data-ubicacion-cp], se completa con el código postal de la localidad.
 */
(function () {
    if (window.zeroUbicacionCascada) {
        return;
    }
    window.zeroUbicacionCascada = true;

    var NIVELES = ['pais', 'provincia', 'departamento', 'localidad'];
    var RUTAS = {
        pais: '/paises',
        provincia: '/provincias?pais=',
        departamento: '/departamentos?provincia=',
        localidad: '/localidades?departamento='
    };
    var TEXTOS = {
        pais: 'Seleccionar país',
        provincia: 'Seleccionar provincia',
        departamento: 'Seleccionar departamento',
        localidad: 'Seleccionar localidad'
    };

    function iniciar(contenedor) {
        var api = contenedor.getAttribute('data-api');
        var selects = NIVELES
            .map(function (nivel) { return contenedor.querySelector('[data-ubicacion="' + nivel + '"]'); })
            .filter(function (select) { return select !== null; });
        var formulario = contenedor.closest('form') || document;
        var codigoPostal = formulario.querySelector('[data-ubicacion-cp]');

        function vaciar(desde) {
            for (var i = desde; i < selects.length; i++) {
                var nivel = selects[i].getAttribute('data-ubicacion');
                selects[i].innerHTML = '';
                selects[i].add(new Option(TEXTOS[nivel], ''));
            }
            actualizarCodigoPostal();
        }

        function actualizarCodigoPostal() {
            if (!codigoPostal) {
                return;
            }
            var ultimo = selects[selects.length - 1];
            var opcion = ultimo.options[ultimo.selectedIndex];
            codigoPostal.value = opcion && opcion.getAttribute('data-codigo-postal') || '';
        }

        function cargar(indice, idPadre) {
            var select = selects[indice];
            var nivel = select.getAttribute('data-ubicacion');
            var url = api + RUTAS[nivel] + (indice === 0 ? '' : encodeURIComponent(idPadre));
            vaciar(indice);
            return fetch(url, { headers: { Accept: 'application/json' } })
                .then(function (respuesta) {
                    if (!respuesta.ok) {
                        throw new Error(respuesta.status);
                    }
                    return respuesta.json();
                })
                .then(function (opciones) {
                    opciones.forEach(function (dato) {
                        var opcion = new Option(dato.nombre, dato.id);
                        if (dato.codigoPostal) {
                            opcion.setAttribute('data-codigo-postal', dato.codigoPostal);
                        }
                        select.add(opcion);
                    });
                    // La precarga se usa una sola vez: después manda lo que elija el usuario.
                    var seleccionado = select.getAttribute('data-seleccionado');
                    select.removeAttribute('data-seleccionado');
                    if (seleccionado && opciones.some(function (dato) { return dato.id === seleccionado; })) {
                        select.value = seleccionado;
                        return alCambiar(indice);
                    }
                })
                .catch(function () {
                    select.options[0].text = 'No se pudieron cargar las opciones';
                });
        }

        function alCambiar(indice) {
            if (indice === selects.length - 1) {
                actualizarCodigoPostal();
                return Promise.resolve();
            }
            if (!selects[indice].value) {
                vaciar(indice + 1);
                return Promise.resolve();
            }
            return cargar(indice + 1, selects[indice].value);
        }

        selects.forEach(function (select, indice) {
            select.addEventListener('change', function () { alCambiar(indice); });
        });
        cargar(0, null);
    }

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('[data-ubicacion-cascada]').forEach(iniciar);
    });
})();
