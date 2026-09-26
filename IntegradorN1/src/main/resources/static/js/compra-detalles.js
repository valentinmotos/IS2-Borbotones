/*
 * Tabla dinámica de productos del formulario de compra a proveedor (admin/compras/formulario).
 * "Agregar producto" clona el renglón de <template id="plantilla-detalle"> y "Quitar" lo borra. Después de cada
 * cambio se renumeran los name (detalles[0].productoId, detalles[1].productoId...) para que Spring arme la lista
 * sin huecos. El select de producto de cada renglón se convierte en un select con buscador (Select2 del
 * template), y el subtotal y el total se recalculan en vivo. "Agregar producto" se habilita recién cuando todos los
 * renglones tienen producto, cantidad y precio de costo. El total definitivo lo calcula el servidor.
 * Sin JavaScript el formulario funciona igual, con selects comunes.
 */
(function ($) {
    var formatoMoneda = new Intl.NumberFormat('es-AR', {
        style: 'currency',
        currency: 'ARS',
        maximumFractionDigits: 2
    });

    // Sin mayúsculas ni tildes, así "cALZA" encuentra "Calza" y "atletica" encuentra "Atlética".
    function normalizar(texto) {
        return texto.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();
    }

    function palabras(texto) {
        return normalizar(texto).split(/[^a-z0-9]+/).filter(function (palabra) {
            return palabra !== '';
        });
    }

    // Cada palabra buscada tiene que ser el comienzo de alguna palabra de la opción, en cualquier orden:
    // "remera l" encuentra "REM-DRY-H-L · Remera Zero Dry Fit Hombre (talle L)" y no el talle M.
    function buscarPorPalabras(params, opcion) {
        var buscadas = palabras(params.term || '');
        if (buscadas.length === 0) {
            return opcion;
        }
        var delProducto = palabras(opcion.text || '');
        var coincide = buscadas.every(function (buscada) {
            return delProducto.some(function (palabra) {
                return palabra.indexOf(buscada) === 0;
            });
        });
        return coincide ? opcion : null;
    }

    function activarBuscador(select) {
        $(select).select2({
            width: '100%',
            dropdownAutoWidth: true,
            placeholder: 'Buscar por código o nombre',
            matcher: buscarPorPalabras,
            language: {
                noResults: function () {
                    return 'No hay productos que coincidan.';
                }
            }
        });
    }

    function renumerar(cuerpo) {
        cuerpo.querySelectorAll('.fila-detalle').forEach(function (fila, indice) {
            fila.querySelectorAll('[name]').forEach(function (campo) {
                campo.name = campo.name.replace(/^detalles\[[^\]]*\]/, 'detalles[' + indice + ']');
            });
        });
    }

    function numero(campo) {
        var valor = parseFloat(campo.value);
        return isNaN(valor) ? 0 : valor;
    }

    function recalcular(cuerpo, total) {
        var suma = 0;
        cuerpo.querySelectorAll('.fila-detalle').forEach(function (fila) {
            var subtotal = numero(fila.querySelector('[data-cantidad]')) * numero(fila.querySelector('[data-precio]'));
            fila.querySelector('[data-subtotal]').textContent = formatoMoneda.format(subtotal);
            suma += subtotal;
        });
        total.textContent = formatoMoneda.format(suma);
    }

    function renglonCompleto(fila) {
        return fila.querySelector('[data-producto]').value !== ''
            && numero(fila.querySelector('[data-cantidad]')) >= 1
            && numero(fila.querySelector('[data-precio]')) > 0;
    }

    // Con un renglón a medio cargar no se puede agregar otro: se deshabilita el botón y se muestra el aviso.
    function actualizarAgregar(cuerpo, agregar, aviso) {
        var filas = Array.prototype.slice.call(cuerpo.querySelectorAll('.fila-detalle'));
        var todosCompletos = filas.every(renglonCompleto);
        agregar.disabled = !todosCompletos;
        if (aviso) {
            aviso.hidden = todosCompletos;
        }
    }

    document.addEventListener('DOMContentLoaded', function () {
        var cuerpo = document.querySelector('[data-detalles]');
        var plantilla = document.getElementById('plantilla-detalle');
        var agregar = document.querySelector('[data-agregar-detalle]');
        var total = document.querySelector('[data-total]');
        var aviso = document.querySelector('[data-aviso-incompleto]');
        if (!cuerpo || !plantilla || !agregar || !total) {
            return;
        }
        var conBuscador = $ && $.fn && $.fn.select2;
        var actualizar = function () {
            actualizarAgregar(cuerpo, agregar, aviso);
        };

        agregar.addEventListener('click', function () {
            cuerpo.appendChild(plantilla.content.cloneNode(true));
            renumerar(cuerpo);
            recalcular(cuerpo, total);
            actualizar();
            var select = cuerpo.lastElementChild.querySelector('[data-producto]');
            if (conBuscador) {
                activarBuscador(select);
                $(select).select2('open');
            } else {
                select.focus();
            }
        });

        cuerpo.addEventListener('click', function (evento) {
            var quitar = evento.target.closest('[data-quitar-detalle]');
            if (quitar) {
                var fila = quitar.closest('.fila-detalle');
                if (conBuscador) {
                    $(fila.querySelector('[data-producto]')).select2('destroy');
                }
                fila.remove();
                renumerar(cuerpo);
                recalcular(cuerpo, total);
                actualizar();
            }
        });

        cuerpo.addEventListener('input', function (evento) {
            if (evento.target.matches('[data-cantidad], [data-precio]')) {
                recalcular(cuerpo, total);
                actualizar();
            }
        });

        // Select común (sin Select2). Select2 avisa con su propio evento, más abajo.
        cuerpo.addEventListener('change', function (evento) {
            if (evento.target.matches('[data-producto]')) {
                actualizar();
            }
        });

        // Al elegir un producto, el foco pasa a la cantidad del mismo renglón.
        if (conBuscador) {
            $(cuerpo).on('select2:select', '[data-producto]', function () {
                actualizar();
                $(this).closest('.fila-detalle').find('[data-cantidad]').trigger('focus');
            });
            cuerpo.querySelectorAll('[data-producto]').forEach(activarBuscador);
        }

        renumerar(cuerpo);
        recalcular(cuerpo, total);
        actualizar();
    });
})(window.jQuery);
