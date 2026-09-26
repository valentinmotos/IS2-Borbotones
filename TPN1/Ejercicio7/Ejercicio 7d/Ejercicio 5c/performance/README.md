# Prueba de rendimiento con JMeter

`mascotas-smoke.jmx` implementa los elementos mostrados en la playlist: un plan de prueba, un grupo de hilos, valores HTTP reutilizables, samplers, assertions y listeners de análisis.

Escenario: 5 usuarios virtuales, incorporados durante 5 segundos, recorren una vez las páginas públicas `/`, `/login` y `/registro`. Cada petición debe devolver HTTP 200 y tardar como máximo 2 segundos. No se envían formularios ni se crean, modifican o eliminan datos.

## Ejecución

1. Iniciar MySQL y la aplicación: `./mvnw.cmd spring-boot:run`.
2. Abrir `mascotas-smoke.jmx` con Apache JMeter 5.6 o superior.
3. Si el servidor no corre en `localhost:8080`, editar las variables `HOST` y `PORT` del plan.
4. Ejecutar el grupo de hilos y revisar `Informe resumido` y `Ver árbol de resultados`.

Para una corrida no gráfica que guarde resultados:

```powershell
jmeter -n -t .\performance\mascotas-smoke.jmx -l .\performance\resultado.jtl -e -o .\performance\reporte
```

No ejecutar listeners visuales durante pruebas de alta carga: se incluyen para el análisis didáctico de esta prueba de humo.
