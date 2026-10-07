$ErrorActionPreference = 'Continue'

$raiz = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $raiz

$javaHome = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
if (Test-Path $javaHome) { $env:JAVA_HOME = $javaHome }
$mavenBin = Join-Path $env:USERPROFILE '.tools\apache-maven-3.10.0\bin'
if (Test-Path $mavenBin) { $env:Path = "$mavenBin;$env:Path" }
if ($env:JAVA_HOME) { $env:Path = "$env:JAVA_HOME\bin;$env:Path" }

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
try { $Host.UI.RawUI.WindowTitle = 'AutoDrive Motors JAO - Supabase' } catch { }

$versionJava = (& java -version 2>&1 | Select-Object -First 1)
$versionMaven = (& mvn -v 2>&1 | Select-Object -First 1)

Write-Host ''
Write-Host '======================================================================' -ForegroundColor Cyan
Write-Host '  AutoDrive Motors JAO - consola de trabajo (perfil Supabase)' -ForegroundColor Cyan
Write-Host '======================================================================' -ForegroundColor Cyan
Write-Host ''
Write-Host "  Carpeta      : $raiz"
Write-Host "  Java         : $versionJava"
Write-Host "  Maven        : $versionMaven"
Write-Host "  Archivo .env : $(if (Test-Path (Join-Path $raiz '.env')) { 'presente (ignorado por Git)' } else { 'FALTA: copie .env.example como .env' })"
Write-Host ''
Write-Host '  Comandos utiles' -ForegroundColor Yellow
Write-Host '  ------------------------------------------------------------------'
Write-Host '  1) Comprobar variables y red, sin arrancar la aplicacion:'
Write-Host '       .\scripts\supabase-run.ps1 -SoloDiagnostico' -ForegroundColor Green
Write-Host ''
Write-Host '  2) Arrancar la aplicacion contra Supabase:'
Write-Host '       .\scripts\supabase-run.ps1 -Puerto 8099' -ForegroundColor Green
Write-Host '     Para detenerla: Ctrl + C'
Write-Host ''
Write-Host '  3) En OTRA consola, prueba funcional completa (20 verificaciones):'
Write-Host '       $env:AUTODRIVE_ADMIN_PASSWORD = Read-Host "Clave admin" -MaskInput' -ForegroundColor Green
Write-Host '       .\scripts\prueba-api-supabase.ps1 -BaseUrl http://127.0.0.1:8099' -ForegroundColor Green
Write-Host ''
Write-Host '  4) Pruebas unitarias con el perfil dev (H2 en memoria):'
Write-Host '       mvn clean test' -ForegroundColor Green
Write-Host ''
Write-Host '  Panel web: http://127.0.0.1:8099   (usuario administrador: admin)' -ForegroundColor Yellow
Write-Host ''
Write-Host '  Aviso: si Supabase responde "(ECIRCUITBREAKER) too many'
Write-Host '  authentication failures", no reintente en bucle: espere unos'
Write-Host '  minutos y haga un solo intento.'
Write-Host ''
