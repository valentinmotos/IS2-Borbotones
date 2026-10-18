/*
 * Selección de talle sin recargar en el detalle del producto (publico/producto-detalle).
 * Cada talle es un producto distinto: el botón trae en data-* su id, código, precio, stock, oferta e imagen.
 * Al elegirlo se actualizan esos datos, el producto que se agrega al carrito y la URL. Sin JavaScript,
 * cada talle sigue siendo un link a su propio detalle.
 */
(function () {
    var botones = document.querySelectorAll('[data-talle]');
    if (botones.length < 2) {
        return;
    }

    function textoStock(stock) {
        return stock === 1 ? 'Última unidad disponible' : stock + ' unidades disponibles';
    }

    function seleccionar(boton) {
        var datos = boton.dataset;
        var url = boton.getAttribute('href');
        var stock = parseInt(datos.stock, 10);

        botones.forEach(function (otro) {
            otro.classList.toggle('active', otro === boton);
            if (otro === boton) {
                otro.setAttribute('aria-current', 'true');
            } else {
                otro.removeAttribute('aria-current');
            }
        });

        document.getElementById('detalleCodigo').textContent = 'Código ' + datos.codigo;
        document.getElementById('detallePrecio').textContent = datos.precio;
        document.getElementById('detalleStock').textContent = textoStock(stock);
        document.getElementById('detalleOferta').hidden = datos.oferta !== 'true';
        document.getElementById('detalleImagen').src = datos.imagen;

        document.querySelectorAll('[data-producto-id]').forEach(function (campo) {
            campo.value = datos.id;
        });
        document.querySelectorAll('[data-producto-url]').forEach(function (campo) {
            campo.value = url;
        });
        document.querySelectorAll('[data-producto-cantidad]').forEach(function (campo) {
            campo.max = stock;
            if (parseInt(campo.value, 10) > stock) {
                campo.value = stock;
            }
        });

        history.replaceState(null, '', url);
    }

    botones.forEach(function (boton) {
        boton.addEventListener('click', function (evento) {
            evento.preventDefault();
            seleccionar(boton);
        });
    });
})();
