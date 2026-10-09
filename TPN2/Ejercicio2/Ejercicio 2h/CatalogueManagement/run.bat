@echo off
cd /d "%~dp0"
start "Catalogue Management API" cmd /k ""%~dp0run-backend.bat""
if not exist "frontend\node_modules" (
  call npm --prefix frontend install
  if errorlevel 1 exit /b 1
)
cd /d "%~dp0frontend"
call npm run dev
