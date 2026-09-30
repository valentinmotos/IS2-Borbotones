/* Filtra los selectores de ubicación usando solo las opciones renderizadas por el servidor. */
(function () {
    if (window.zeroUbicacionCascada) return;
    window.zeroUbicacionCascada = true;

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('[data-ubicacion-cascada]').forEach(function (contenedor) {
            var selects = Array.from(contenedor.querySelectorAll('[data-ubicacion]'));
            var opciones = selects.map(function (select) {
                return Array.from(select.options).slice(1).map(function (opcion) { return opcion.cloneNode(true); });
            });
            var seleccionados = selects.map(function (select) {
                return select.getAttribute('data-seleccionado') || select.value;
            });
            var formulario = contenedor.closest('form') || document;
            var codigoPostal = formulario.querySelector('[data-ubicacion-cp]');

            function actualizarCodigoPostal() {
                if (!codigoPostal) return;
                var ultimo = selects[selects.length - 1];
                var opcion = ultimo.options[ultimo.selectedIndex];
                codigoPostal.value = opcion && opcion.getAttribute('data-codigo-postal') || '';
            }

            function filtrarDesde(indice) {
                for (var i = indice; i < selects.length; i++) {
                    var select = selects[i];
                    var padre = i === 0 ? null : selects[i - 1].value;
                    select.length = 1;
                    if (i === 0 || padre) {
                        opciones[i].forEach(function (opcion) {
                            if (i === 0 || opcion.getAttribute('data-padre') === padre) {
                                select.add(opcion.cloneNode(true));
                            }
                        });
                    }
                    if (seleccionados[i] && Array.from(select.options).some(function (opcion) {
                        return opcion.value === seleccionados[i];
                    })) select.value = seleccionados[i];
                    else select.value = '';
                }
                actualizarCodigoPostal();
            }

            selects.forEach(function (select, indice) {
                select.addEventListener('change', function () {
                    seleccionados = selects.map(function (actual, posicion) {
                        return posicion <= indice ? actual.value : '';
                    });
                    filtrarDesde(indice + 1);
                });
            });
            filtrarDesde(0);
        });
    });
})();
