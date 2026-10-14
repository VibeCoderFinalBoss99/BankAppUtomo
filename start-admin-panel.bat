@echo off
setlocal
title UTOMO BANK - Control Panel Server
echo ========================================================
echo   UTOMO BANK - Back-Office Control Panel
echo ========================================================
echo.
set "ADMIN_ROOT=%~dp0admin-panel"
set "PORT="
for /f %%P in ('powershell -NoProfile -Command "$p=8181; while (Get-NetTCPConnection -LocalPort $p -State Listen -ErrorAction SilentlyContinue) { $p++ }; $p"') do set "PORT=%%P"
if not defined PORT set "PORT=8181"
echo Menjalankan web server lokal di port %PORT%...
echo URL: http://localhost:%PORT%/index.html
echo.
pushd "%ADMIN_ROOT%"
start "UTOMO Bank Admin Panel" "http://localhost:%PORT%/index.html"
python -m http.server %PORT% --bind 127.0.0.1
popd
pause
