/*
 * Slider de la home (publico/inicio). Reemplaza al Slick1 de slick-custom.js del template: aquel mostraba las
 * capas de la diapositiva nueva recién al terminar el cambio, y el slider quedaba vacío un rato. Acá las capas
 * de la diapositiva que entra se animan apenas empieza el cambio, con los data-appear y data-delay de cada una.
 */
(function ($) {
    var slider = $('.zero-slider .slick1');
    if (slider.length === 0) {
        return;
    }
    var esperas = [];

    function ocultar(slide) {
        slide.find('.layer-slick1').each(function () {
            $(this).removeClass($(this).data('appear') + ' visible-true');
        });
    }

    function mostrar(slide) {
        esperas.forEach(clearTimeout);
        esperas = [];
        ocultar(slide);
        slide.find('.layer-slick1').each(function () {
            var capa = $(this);
            esperas.push(setTimeout(function () {
                capa.addClass(capa.data('appear') + ' visible-true');
            }, capa.data('delay') || 0));
        });
    }

    slider.on('init', function (evento, slick) {
        mostrar(slick.$slides.eq(0));
    });

    slider.on('beforeChange', function (evento, slick, actual, siguiente) {
        mostrar(slick.$slides.eq(siguiente));
    });

    slider.on('afterChange', function (evento, slick, actual) {
        slick.$slides.not(slick.$slides.eq(actual)).each(function () {
            ocultar($(this));
        });
    });

    slider.slick({
        pauseOnFocus: false,
        pauseOnHover: false,
        slidesToShow: 1,
        slidesToScroll: 1,
        fade: true,
        speed: 500,
        infinite: true,
        autoplay: true,
        autoplaySpeed: 6000,
        arrows: true,
        appendArrows: slider.closest('.wrap-slick1'),
        prevArrow: '<button class="arrow-slick1 prev-slick1" aria-label="Anterior"><i class="zmdi zmdi-caret-left"></i></button>',
        nextArrow: '<button class="arrow-slick1 next-slick1" aria-label="Siguiente"><i class="zmdi zmdi-caret-right"></i></button>'
    });
})(jQuery);
