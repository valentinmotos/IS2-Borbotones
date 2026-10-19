# ShopEase - Ejercicio 3d

Aplicacion React + Spring Boot preparada para ejecucion local. El README del autor se conserva en README-original.md.

## Requisitos y arranque

- Java 17 o superior (verificado en esta maquina con Java 26.0.2).
- Maven 3.9 y Node.js con npm disponibles en PATH.
- Internet para instalar dependencias por primera vez.

Desde esta carpeta, en PowerShell:

```powershell
.\iniciar.ps1
```

El script instala dependencias si faltan, compila y prueba el backend, y deja los dos procesos en segundo plano con ventanas ocultas. Si PowerShell bloquea scripts, ejecutar: powershell -ExecutionPolicy Bypass -File .\iniciar.ps1

- Interfaz: http://localhost:3000
- API: http://localhost:8080/api/products
- Swagger: http://localhost:8080/swagger-ui/index.html
- Administracion: http://localhost:3000/admin (iniciar sesion como administrador).
- Logs: .runtime/backend.log y .runtime/ui.log (y sus archivos de errores).

Para detener, usar los PID de .runtime/processes.json y Stop-Process -Id <PID>. Detener tambien el proceso Node hijo del servidor React si sigue ocupando el puerto 3000.

Tambien puede iniciarse manualmente, en dos terminales:

```powershell
cd backend
mvn spring-boot:run
```

```powershell
cd UI
npm ci
npm start
```

## Cuentas y compra de ejemplo

| Perfil | Email | Clave |
| --- | --- | --- |
| Cliente | demo@shopease.local | Demo123! |
| Administrador | admin@shopease.local | Admin123! |

El perfil local crea ocho productos, dos categorias, talles, stock y direcciones de ejemplo. Los datos se guardan en backend/data y las imagenes subidas en backend/uploads; no se versionan.

Recorrido: ingresar, ir a Men o Women, abrir un producto, seleccionar talle, agregar al carrito, ir a checkout y elegir Cash on delivery. El pedido queda registrado en la cuenta. El servidor calcula precios y descuenta stock. Estas cuentas y la clave JWT local son solo para desarrollo.

El registro tambien funciona: el codigo de verificacion se escribe en el log del backend cuando se usa el perfil local, sin enviar correos reales.

## Servicios externos opcionales

El arranque local no requiere PostgreSQL, Google, Stripe ni CDN.

- PostgreSQL: activar SPRING_PROFILES_ACTIVE=postgres y configurar DB_URL, DB_USER, DB_PASSWORD y JWT_KEY (Base64, al menos 32 bytes decodificados). Este perfil no crea usuarios ni catalogo de demostracion.
- Google: combinar perfiles local,google o postgres,google y definir GOOGLE_OAUTH2_CLIENT_ID y GOOGLE_OAUTH2_CLIENT_SECRET. En UI/.env.local definir REACT_APP_GOOGLE_ENABLED=true.
- Stripe: STRIPE_SECRET en backend y REACT_APP_STRIPE_KEY en UI/.env.local. Usar credenciales de prueba. La compra local se verifica con pago contra entrega; la integracion real requiere credenciales propias.
- Correo: MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD. El perfil local escribe codigos en logs; fuera del perfil local se usa SMTP.
- CDN: CDN_ZONE, CDN_KEY, CDN_HOST y REACT_APP_CDN_URL. Localmente las subidas de administracion se guardan en disco.
- URL API de la interfaz: REACT_APP_API_BASE_URL (ejemplo en UI/.env.example). Reiniciar React tras cambiar variables.

## Verificacion

```powershell
mvn -f backend/pom.xml test
cd UI
npm test -- --watchAll=false --runInBand
npm run build
```

La prueba de integracion usa H2 en memoria y cubre catalogo, detalle, login, perfil, compra contra entrega, calculo de importe y permisos de escritura. La prueba de interfaz cubre filtros de la API sin categoryId=undefined.

El proyecto original usa Create React App y contiene advertencias de dependencias antiguas y de lint. Esta adaptacion permite su ejecucion local; no representa una preparacion para produccion ni una auditoria integral de seguridad.
