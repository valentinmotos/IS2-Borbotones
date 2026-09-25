/*
 * Selects en cascada categoría → subcategoría (fragments/categoria :: cascada).
 * El select de categoría tiene data-subcategorias con el selector del de subcategoría. Ese select
 * trae todas las subcategorías agrupadas en <optgroup data-categoria="{id}">: el script guarda esos
 * grupos y, al elegir una categoría, deja solo sus subcategorías. Sin categoría se muestran todas.
 */
(function () {
    if (window.zeroCategoriaCascada) {
        return;
    }
    window.zeroCategoriaCascada = true;

    function iniciar(selectCategoria) {
        var selectSubCategoria = document.querySelector(selectCategoria.getAttribute('data-subcategorias'));
        if (!selectSubCategoria) {
            return;
        }
        var textoVacio = selectSubCategoria.options[0].text;
        var grupos = Array.prototype.map.call(selectSubCategoria.querySelectorAll('optgroup'), function (grupo) {
            return {
                categoria: grupo.getAttribute('data-categoria'),
                etiqueta: grupo.label,
                opciones: Array.prototype.map.call(grupo.children, function (opcion) {
                    return { valor: opcion.value, texto: opcion.text };
                })
            };
        });

        function categoriaDe(idSubCategoria) {
            var grupo = grupos.find(function (g) {
                return g.opciones.some(function (o) { return o.valor === idSubCategoria; });
            });
            return grupo ? grupo.categoria : '';
        }

        function reconstruir() {
            var categoria = selectCategoria.value;
            var seleccionada = selectSubCategoria.value;
            selectSubCategoria.innerHTML = '';
            selectSubCategoria.add(new Option(textoVacio, ''));
            grupos.forEach(function (grupo) {
                if (categoria && grupo.categoria !== categoria) {
                    return;
                }
                // Con una categoría elegida las opciones van sueltas; sin categoría, agrupadas.
                var destino = selectSubCategoria;
                if (!categoria) {
                    destino = document.createElement('optgroup');
                    destino.label = grupo.etiqueta;
                    destino.setAttribute('data-categoria', grupo.categoria);
                    selectSubCategoria.appendChild(destino);
                }
                grupo.opciones.forEach(function (opcion) {
                    destino.appendChild(new Option(opcion.texto, opcion.valor));
                });
            });
            var sigue = Array.prototype.some.call(selectSubCategoria.options, function (o) {
                return o.value === seleccionada;
            });
            selectSubCategoria.value = sigue ? seleccionada : '';
        }

        selectCategoria.addEventListener('change', reconstruir);
        // Elegir una subcategoría sin categoría completa la categoría.
        selectSubCategoria.addEventListener('change', function () {
            if (!selectCategoria.value && selectSubCategoria.value) {
                selectCategoria.value = categoriaDe(selectSubCategoria.value);
                reconstruir();
            }
        });
        reconstruir();
    }

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('select[data-subcategorias]').forEach(iniciar);
    });
})();
