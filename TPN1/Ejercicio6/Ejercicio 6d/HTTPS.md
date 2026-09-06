# HTTPS local

La aplicación está configurada para ejecutarse en `https://localhost:8443`.

Antes de iniciarla, generá un certificado de desarrollo desde una terminal con JDK instalado:

```powershell
keytool -genkeypair -alias mascotas-local -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore src/main/resources/keystore.p12 -storepass changeit -keypass changeit -dname "CN=localhost, OU=Desarrollo, O=IS2 Borbotones, L=Mendoza, ST=Mendoza, C=AR" -validity 3650
```

En producción no se debe usar este certificado: se debe configurar uno válido y proveer `SSL_KEY_STORE_PASSWORD` como variable de entorno.
