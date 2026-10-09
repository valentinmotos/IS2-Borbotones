# Ejercicio 2e: migración de proveedores desde TXT

Extiende el ejercicio 2d con la importación de proveedores desde un archivo de texto.
Conserva las funciones de préstamos, envíos automáticos y reportes PDF y Excel.

## Ejecutar

Requiere Java 17 o superior. Desde esta carpeta, iniciar en dos terminales:

```powershell
.\mvnw.ps1 -pl servidor spring-boot:run
```

```powershell
.\mvnw.ps1 -pl cliente spring-boot:run
```

Abrir `http://localhost:8080`. El servidor usa el puerto 8000.

## Importar proveedores

En el menú, abrir **Migración**, seleccionar el archivo TXT y presionar **Importar proveedores**.
El archivo debe estar codificado en UTF-8 y tener un proveedor por renglón, con los campos
separados por punto y coma:

```text
Lionel;Escaloneta;293851450;Perú;345;
Pablo;Perez;293451460;San Martín;1050;
Esteban;Peralta;291251480;Benavente;121;
```

Se permiten líneas vacías y un punto y coma final opcional. Los cinco valores deben estar
completos y el número de domicilio debe ser numérico. Si una línea no cumple el formato,
se informa su número y no se importa parcialmente el archivo. Los proveedores se guardan
en la tabla `Proveedor` de SQLite.

## API

`POST /api/migracion`, con `multipart/form-data` y el campo `archivo`, importa el TXT y
devuelve la cantidad de proveedores guardados.

El menú **Migración** del cliente ofrece la carga y muestra el resultado o el error de formato.
Los reportes PDF y Excel del ejercicio 2d siguen disponibles en las secciones Personas y Libros.
