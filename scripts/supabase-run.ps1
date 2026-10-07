[CmdletBinding()]
param(
    [string]$EnvFile = (Join-Path (Split-Path -Parent $PSScriptRoot) '.env'),
    [switch]$SoloDiagnostico,
    [string[]]$MavenArgs = @(),
    [int]$Puerto = 0
)

$ErrorActionPreference = 'Stop'

function Escribir-Titulo($texto) {
    Write-Host ''
    Write-Host "== $texto ==" -ForegroundColor Cyan
}

function Obtener-Causa($excepcion) {
    $actual = $excepcion
    while ($actual.InnerException) { $actual = $actual.InnerException }
    return $actual.Message
}

$raiz = Split-Path -Parent $PSScriptRoot
Push-Location $raiz
try {
    Escribir-Titulo "Archivo de variables"
    if (-not (Test-Path -LiteralPath $EnvFile)) {
        Write-Host "No se encontró $EnvFile" -ForegroundColor Red
        Write-Host 'Copie .env.example como .env y complete los valores reales:' -ForegroundColor Yellow
        Write-Host '  Copy-Item .env.example .env' -ForegroundColor Yellow
        exit 1
    }
    Write-Host "Leyendo $EnvFile" -ForegroundColor Green

    $obligatorias = @('SUPABASE_DB_URL', 'SUPABASE_DB_USER', 'SUPABASE_DB_PASSWORD',
                      'AUTODRIVE_ADMIN_USER', 'AUTODRIVE_ADMIN_PASSWORD')
    $valores = @{}

    foreach ($linea in Get-Content -LiteralPath $EnvFile) {
        $texto = $linea.Trim()
        if ($texto -eq '' -or $texto.StartsWith('#')) { continue }
        $separador = $texto.IndexOf('=')
        if ($separador -lt 1) { continue }
        $clave = $texto.Substring(0, $separador).Trim()
        $valor = $texto.Substring($separador + 1).Trim()
        if ($valor.Length -ge 2 -and
            (($valor.StartsWith('"') -and $valor.EndsWith('"')) -or
             ($valor.StartsWith("'") -and $valor.EndsWith("'")))) {
            $valor = $valor.Substring(1, $valor.Length - 2)
        }
        $valores[$clave] = $valor
    }

    Escribir-Titulo "Variables obligatorias"
    $faltantes = @()
    foreach ($clave in $obligatorias) {
        $valor = $valores[$clave]
        if ([string]::IsNullOrWhiteSpace($valor) -or $valor -match '<[^>]+>') {
            $faltantes += $clave
            Write-Host ("  {0,-26} PENDIENTE" -f $clave) -ForegroundColor Red
        } else {
            $pista = if ($clave -match 'PASSWORD') { '(oculta)' } else { $valor }
            Write-Host ("  {0,-26} {1}" -f $clave, $pista) -ForegroundColor Green
        }
    }
    if ($faltantes.Count -gt 0) {
        Write-Host ''
        Write-Host "Complete en .env: $($faltantes -join ', ')" -ForegroundColor Yellow
        Write-Host 'La contraseña de Supabase la entrega el dueño del proyecto por un canal privado.' -ForegroundColor Yellow
        exit 1
    }

    foreach ($par in $valores.GetEnumerator()) {
        Set-Item -Path "env:$($par.Key)" -Value $par.Value
    }
    if (-not $valores.ContainsKey('SPRING_PROFILES_ACTIVE')) {
        $env:SPRING_PROFILES_ACTIVE = 'supabase'
    }

    Escribir-Titulo "Perfil activo"
    Write-Host "SPRING_PROFILES_ACTIVE = $env:SPRING_PROFILES_ACTIVE"
    if ($env:SPRING_PROFILES_ACTIVE -ne 'supabase') {
        Write-Host 'Advertencia: el perfil no es "supabase"; se usaría H2 en memoria y se perderían los datos.' -ForegroundColor Yellow
    }

    Escribir-Titulo "Prueba de red contra la base de datos"
    $uri = [System.Uri]($env:SUPABASE_DB_URL -replace '^jdbc:', '')
    $hostBase = $uri.Host
    $puertoBaseDatos = if ($uri.Port -gt 0) { $uri.Port } else { 5432 }
    Write-Host "Destino: $hostBase`:$puertoBaseDatos" -ForegroundColor Green

    $cliente = New-Object System.Net.Sockets.TcpClient
    try {
        $tarea = $cliente.ConnectAsync($hostBase, $puertoBaseDatos)
        if (-not $tarea.Wait(8000)) {
            throw "Tiempo de espera agotado (8 s) al conectar con $hostBase`:$puertoBaseDatos"
        }
        Write-Host 'Conexion TCP establecida.' -ForegroundColor Green
    } catch {
        Write-Host "No se pudo conectar: $(Obtener-Causa $_.Exception)" -ForegroundColor Red
        Write-Host ''
        Write-Host 'Revise en este orden:' -ForegroundColor Yellow
        Write-Host '  1. Que la contraseña y la referencia del proyecto sean correctas.'
        Write-Host '  2. Que el proyecto Supabase no esté pausado (los proyectos gratuitos se pausan por inactividad).'
        Write-Host '  3. Si su red no tiene IPv6, use la URL del pooler (host ...pooler.supabase.com).'
        Write-Host '  4. Si usa el pooler de transacciones (puerto 6543), agregue &prepareThreshold=0 a la URL.'
        Write-Host '  5. Firewall, antivirus o red corporativa que bloquee el puerto 5432/6543.'
        exit 1
    } finally {
        $cliente.Dispose()
    }

    if ($SoloDiagnostico) {
        Escribir-Titulo "Diagnostico terminado (-SoloDiagnostico)"
        Write-Host 'Variables y red correctas. Ejecute sin -SoloDiagnostico para arrancar la aplicacion.' -ForegroundColor Green
        exit 0
    }

    Escribir-Titulo "Arranque de la aplicacion"
    $argumentosMaven = @($MavenArgs)
    if ($Puerto -gt 0) {
        $argumentosMaven += "-Dspring-boot.run.arguments=--server.port=$Puerto"
        Write-Host "El servidor escuchará en el puerto $Puerto" -ForegroundColor Green
    }
    Write-Host 'En el primer arranque Flyway aplica las migraciones; despues Hibernate valida el esquema.' -ForegroundColor Gray
    Write-Host 'Si falla con "missing column" o "wrong column type", ejecute antes:' -ForegroundColor Gray
    Write-Host '  Database\02_scripts\03_verificar_esquema_supabase.sql  en el SQL Editor de Supabase' -ForegroundColor Gray
    Write-Host ''

    & mvn @argumentosMaven 'spring-boot:run'
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
