/*
 * Lista dinámica de contactos del formulario de proveedor (admin/proveedores/formulario).
 * "Agregar contacto" clona la fila de <template id="plantilla-contacto"> y "Quitar" borra la fila. Después de
 * cada cambio se renumeran los name (contactos[0].tipo, contactos[1].tipo...) para que Spring arme la lista
 * sin huecos. El ejemplo del campo valor cambia según el tipo elegido.
 */
(function () {
    var EJEMPLOS = {
        CORREO: 'ventas@proveedor.com.ar',
        CELULAR: '5492614123456',
        FIJO: '261 423-1234'
    };

    function renumerar(cuerpo) {
        cuerpo.querySelectorAll('.fila-contacto').forEach(function (fila, indice) {
            fila.querySelectorAll('[name]').forEach(function (campo) {
                campo.name = campo.name.replace(/^contactos\[[^\]]*\]/, 'contactos[' + indice + ']');
            });
        });
    }

    function actualizarEjemplo(fila) {
        var tipo = fila.querySelector('[data-tipo-medio]').value;
        fila.querySelector('[data-valor-contacto]').placeholder = 'Ej: ' + (EJEMPLOS[tipo] || '');
    }

    document.addEventListener('DOMContentLoaded', function () {
        var cuerpo = document.querySelector('[data-contactos]');
        var plantilla = document.getElementById('plantilla-contacto');
        var agregar = document.querySelector('[data-agregar-contacto]');
        if (!cuerpo || !plantilla || !agregar) {
            return;
        }

        agregar.addEventListener('click', function () {
            cuerpo.appendChild(plantilla.content.cloneNode(true));
            renumerar(cuerpo);
            var nueva = cuerpo.lastElementChild;
            actualizarEjemplo(nueva);
            nueva.querySelector('[data-tipo-medio]').focus();
        });

        cuerpo.addEventListener('click', function (evento) {
            var quitar = evento.target.closest('[data-quitar-contacto]');
            if (quitar) {
                quitar.closest('.fila-contacto').remove();
                renumerar(cuerpo);
            }
        });

        cuerpo.addEventListener('change', function (evento) {
            if (evento.target.matches('[data-tipo-medio]')) {
                actualizarEjemplo(evento.target.closest('.fila-contacto'));
            }
        });

        cuerpo.querySelectorAll('.fila-contacto').forEach(actualizarEjemplo);
    });
})();
