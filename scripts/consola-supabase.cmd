@echo off
title AutoDrive Motors JAO - Supabase
cd /d "%~dp0.."
pwsh.exe -NoExit -NoProfile -ExecutionPolicy Bypass -File "%~dp0abrir-consola-supabase.ps1"
if errorlevel 1 (
  echo.
  echo No se pudo iniciar pwsh; se intenta con Windows PowerShell...
  powershell.exe -NoExit -NoProfile -ExecutionPolicy Bypass -File "%~dp0abrir-consola-supabase.ps1"
)
