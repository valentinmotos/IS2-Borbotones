@echo off
setlocal
set "ROOT=%~dp0"
title Iniciar Dulce Mayor

echo Iniciando Dulce Mayor...
echo.

where node >nul 2>&1
if errorlevel 1 (
  echo ERROR: No se encontro Node.js. Instala Node.js 22.12 o superior.
  goto :error
)
where npm.cmd >nul 2>&1
if errorlevel 1 (
  echo ERROR: No se encontro npm. Reinstala Node.js incluyendo npm.
  goto :error
)

echo Comprobando PostgreSQL local...
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$s=Get-Service -Name 'postgresql*' -ErrorAction SilentlyContinue | Select-Object -First 1; if (!$s) { exit 2 }; if ($s.Status -ne 'Running') { try { Start-Service -Name $s.Name -ErrorAction Stop } catch { exit 3 } }; exit 0"
if errorlevel 1 (
  echo ERROR: No se encontro un servicio PostgreSQL local activo. Instala PostgreSQL para Windows y verifica que exista la base 'dulcemayor'.
  echo El backend usa por defecto localhost:5432, usuario postgres y clave postgres.
  goto :error
)

powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$ok=$false; for($i=0; $i -lt 40; $i++){ $c=New-Object Net.Sockets.TcpClient; try { $c.Connect('localhost',5432); $ok=$true; break } catch { Start-Sleep -Seconds 2 } finally { $c.Dispose() } }; if(!$ok){exit 1}"
if errorlevel 1 (
  echo ERROR: PostgreSQL esta iniciado, pero no responde en localhost:5432.
  goto :error
)

:database_ready
if not exist "%ROOT%node_modules\.bin\vite.cmd" (
  echo Instalando dependencias de los clientes...
  pushd "%ROOT%"
  call npm.cmd ci
  if errorlevel 1 (
    popd
    echo ERROR: No se pudieron instalar las dependencias de Node.
    goto :error
  )
  popd
)

echo Abriendo ventanas para la API y los clientes...
start "Dulce Mayor - API" /D "%ROOT%backend" cmd /k mvnw.cmd spring-boot:run
start "Dulce Mayor - Clientes" /D "%ROOT%" cmd /k npm.cmd run dev

echo Esperando el inicio del panel...
timeout /t 10 /nobreak >nul
start "" "http://localhost:5173"
echo.
echo Listo. Panel admin: http://localhost:5173  ^|  Sitio publico: http://localhost:5174
echo Deja abiertas las ventanas de la API y los clientes mientras uses el sistema.
echo Puedes cerrar esta ventana.
pause
exit /b 0

:error
echo.
pause
exit /b 1
