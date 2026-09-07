# HTTPS local

La aplicacion queda configurada para iniciar en `https://localhost:8443`.

Antes de ejecutarla por primera vez, generar un certificado de desarrollo desde la raiz del proyecto:

```powershell
keytool -genkeypair -alias videojuegos-local -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore src/main/resources/keystore.p12 -storepass changeit -keypass changeit -dname "CN=localhost, OU=Desarrollo, O=IS2 Borbotones, L=Mendoza, ST=Mendoza, C=AR" -validity 3650
```

El navegador advertira que es un certificado autofirmado. En produccion se debe utilizar un certificado valido y entregar `SSL_KEY_STORE_PASSWORD` como secreto del entorno. Para pruebas automatizadas se desactiva SSL mediante la configuracion de test.
