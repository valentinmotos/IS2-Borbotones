const formulario = document.getElementById("formulario");
const error = document.getElementById("error");
const resultado = document.getElementById("resultado");

formulario.addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const ciudad = document.getElementById("ciudad").value;
    error.classList.add("d-none");
    resultado.classList.add("d-none");

    const respuesta = await fetch("/api/clima?ciudad=" + encodeURIComponent(ciudad));
    const datos = await respuesta.json();

    if (!respuesta.ok) {
        error.textContent = datos.message || "No se pudo obtener el clima";
        error.classList.remove("d-none");
        return;
    }

    document.getElementById("nombre").textContent = datos.ciudad + ", " + datos.pais;
    document.getElementById("icono").src = datos.icono;
    document.getElementById("temperatura").textContent = Math.round(datos.temperatura) + " °C";
    document.getElementById("descripcion").textContent = datos.descripcion;
    document.getElementById("sensacion").textContent = Math.round(datos.sensacionTermica) + " °C";
    document.getElementById("humedad").textContent = datos.humedad + " %";
    document.getElementById("viento").textContent = datos.viento + " m/s";
    resultado.classList.remove("d-none");
});
