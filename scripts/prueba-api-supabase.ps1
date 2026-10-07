param(
    [string]$BaseUrl = 'http://127.0.0.1:8099',
    [string]$Usuario = 'admin',
    [string]$Contrasena = $env:AUTODRIVE_ADMIN_PASSWORD
)

if ([string]::IsNullOrWhiteSpace($Contrasena)) {
    Write-Host 'Falta la contrasena del administrador.' -ForegroundColor Yellow
    Write-Host 'Pase -Contrasena "..." o defina la variable de entorno AUTODRIVE_ADMIN_PASSWORD.' -ForegroundColor Yellow
    exit 1
}

$ErrorActionPreference = 'Stop'
$sesion = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$resultados = @()

function Pedir {
    param(
        [string]$Uri,
        [string]$Metodo = 'GET',
        $Cuerpo = $null,
        $Cabeceras = $null,
        [switch]$SinRedireccion
    )
    $parametros = @{
        Uri = $Uri; Method = $Metodo; WebSession = $sesion
        UseBasicParsing = $true; TimeoutSec = 30
    }
    if ($Cuerpo) { $parametros['Body'] = $Cuerpo }
    if ($Cabeceras) { $parametros['Headers'] = $Cabeceras }
    if ($SinRedireccion) { $parametros['MaximumRedirection'] = 0 }
    try {
        $r = Invoke-WebRequest @parametros
        return [pscustomobject]@{ Codigo = [int]$r.StatusCode; Cuerpo = $r.Content; Cabeceras = $r.Headers }
    } catch {
        $respuesta = $_.Exception.Response
        if ($null -eq $respuesta) {
            return [pscustomobject]@{ Codigo = 0; Cuerpo = $_.Exception.Message; Cabeceras = @{} }
        }
        $codigo = [int]$respuesta.StatusCode
        $texto = ''
        try {
            $lector = New-Object System.IO.StreamReader($respuesta.GetResponseStream())
            $texto = $lector.ReadToEnd()
            $lector.Dispose()
        } catch { }
        if (-not $texto -and $_.ErrorDetails) { $texto = $_.ErrorDetails.Message }
        return [pscustomobject]@{ Codigo = $codigo; Cuerpo = $texto; Cabeceras = $respuesta.Headers }
    }
}

function Registrar($nombre, $esperado, $obtenido, $detalle) {
    $ok = $esperado -eq $obtenido
    $script:resultados += [pscustomobject]@{
        Prueba = $nombre; Esperado = $esperado; Obtenido = $obtenido
        Estado = if ($ok) { 'OK' } else { 'FALLA' }; Detalle = $detalle
    }
    $color = if ($ok) { 'Green' } else { 'Red' }
    Write-Host ("  {0,-46} esperado {1,-4} obtenido {2,-4} {3}" -f $nombre, $esperado, $obtenido, $detalle) -ForegroundColor $color
}

Write-Host ''
Write-Host "== 1. Autenticacion ==" -ForegroundColor Cyan
$paginaLogin = Pedir -Uri "$BaseUrl/login"
if ($paginaLogin.Cuerpo -match 'name="_csrf"\s+value="([^"]+)"') { $token = $Matches[1] }
elseif ($paginaLogin.Cuerpo -match 'value="([^"]+)"\s+name="_csrf"') { $token = $Matches[1] }
elseif ($paginaLogin.Cuerpo -match 'content="([^"]+)"\s+name="_csrf"') { $token = $Matches[1] }
else { throw 'No se encontro el token CSRF en /login' }
Write-Host "  Token CSRF obtenido ($($token.Length) caracteres)"

$cuerpoLogin = @{ username = $Usuario; password = $Contrasena; _csrf = $token }
$r = Pedir -Uri "$BaseUrl/login" -Metodo 'POST' -Cuerpo $cuerpoLogin -SinRedireccion
$ubicacion = "$($r.Cabeceras['Location'])"
$loginOk = ($r.Codigo -eq 302 -and $ubicacion -notmatch 'error')
$resultados += [pscustomobject]@{
    Prueba = 'POST /login con administrador'; Esperado = '302 sin error'; Obtenido = "$($r.Codigo) $ubicacion"
    Estado = if ($loginOk) { 'OK' } else { 'FALLA' }; Detalle = ''
}
Write-Host ("  {0,-46} {1} {2}" -f 'POST /login con administrador', $r.Codigo, $ubicacion) -ForegroundColor $(if ($loginOk) { 'Green' } else { 'Red' })
if (-not $loginOk) { Write-Host 'No se pudo iniciar sesion; se detiene la prueba.' -ForegroundColor Red; exit 1 }

Write-Host ''
Write-Host "== 2. Sesion y CSRF para la API ==" -ForegroundColor Cyan
$r = Pedir -Uri "$BaseUrl/api/sesion/csrf"
$tokenApi = ($r.Cuerpo | ConvertFrom-Json).token
Registrar 'GET /api/sesion/csrf' 200 $r.Codigo "token de $($tokenApi.Length) caracteres"
$cabeceras = @{ 'X-CSRF-TOKEN' = $tokenApi; 'Content-Type' = 'application/json' }

Write-Host ''
Write-Host "== 3. Lecturas sobre datos reales de Supabase ==" -ForegroundColor Cyan
foreach ($recurso in 'clientes', 'vehiculos', 'ventas', 'mantenimientos') {
    $r = Pedir -Uri "$BaseUrl/api/$recurso"
    $cantidad = 0
    try { $cantidad = @($r.Cuerpo | ConvertFrom-Json).Count } catch { $cantidad = -1 }
    Registrar "GET /api/$recurso" 200 $r.Codigo "$cantidad registros"
}

$r = Pedir -Uri "$BaseUrl/api/reportes/resumen"
Registrar 'GET /api/reportes/resumen' 200 $r.Codigo $r.Cuerpo.Trim()

$r = Pedir -Uri "$BaseUrl/api/vehiculos/disponibles"
$cantidadDisp = 0
try { $cantidadDisp = @($r.Cuerpo | ConvertFrom-Json).Count } catch { }
Registrar 'GET /api/vehiculos/disponibles' 200 $r.Codigo "$cantidadDisp disponibles"

Write-Host ''
Write-Host "== 4. Escritura real en Supabase ==" -ForegroundColor Cyan
$sufijo = ([int]((Get-Date).TimeOfDay.TotalMilliseconds) % 10000).ToString('D4')
$clienteNuevo = @{
    nombre = 'Prueba'; apellido = 'Conexion'; documento = "TEST-$sufijo"
    email = "prueba.$sufijo@example.test"; telefono = '3000000000'
} | ConvertTo-Json
$r = Pedir -Uri "$BaseUrl/api/clientes" -Metodo 'POST' -Cuerpo $clienteNuevo -Cabeceras $cabeceras
$clienteId = if ($r.Codigo -eq 201) { ($r.Cuerpo | ConvertFrom-Json).id } else { 0 }
Registrar 'POST /api/clientes' 201 $r.Codigo "id=$clienteId"

$placa = "TEST$sufijo"
$vehiculoNuevo = @{
    placa = $placa; marca = 'Prueba'; modelo = 'Conexion'
    anio = 2024; precioCop = 50000000; estado = 'DISPONIBLE'
} | ConvertTo-Json
$r = Pedir -Uri "$BaseUrl/api/vehiculos" -Metodo 'POST' -Cuerpo $vehiculoNuevo -Cabeceras $cabeceras
$vehiculoId = if ($r.Codigo -eq 201) { ($r.Cuerpo | ConvertFrom-Json).id } else { 0 }
Registrar 'POST /api/vehiculos' 201 $r.Codigo "id=$vehiculoId placa=$placa"

$ventaNueva = @{ clienteId = $clienteId; vehiculoId = $vehiculoId } | ConvertTo-Json
$r = Pedir -Uri "$BaseUrl/api/ventas" -Metodo 'POST' -Cuerpo $ventaNueva -Cabeceras $cabeceras
$ventaId = if ($r.Codigo -eq 201) { ($r.Cuerpo | ConvertFrom-Json).id } else { 0 }
Registrar 'POST /api/ventas' 201 $r.Codigo "id=$ventaId"

Write-Host ''
Write-Host "== 5. Reglas de negocio y errores ==" -ForegroundColor Cyan
$r = Pedir -Uri "$BaseUrl/api/vehiculos/$vehiculoId"
$estadoTrasVenta = try { ($r.Cuerpo | ConvertFrom-Json).estado } catch { '?' }
Registrar 'Vehiculo queda VENDIDO tras la venta' 'VENDIDO' $estadoTrasVenta "placa $placa"

$r = Pedir -Uri "$BaseUrl/api/clientes/0"
Registrar 'GET /api/clientes/0 (id invalido)' 400 $r.Codigo ''

$r = Pedir -Uri "$BaseUrl/api/clientes/999999"
Registrar 'GET /api/clientes/999999 (inexistente)' 404 $r.Codigo ''

$duplicado = @{
    nombre = 'Otro'; apellido = 'Duplicado'; documento = "TEST-$sufijo"
    email = "otro.$sufijo@example.test"; telefono = '3000000001'
} | ConvertTo-Json
$r = Pedir -Uri "$BaseUrl/api/clientes" -Metodo 'POST' -Cuerpo $duplicado -Cabeceras $cabeceras
Registrar 'POST cliente con documento duplicado' 409 $r.Codigo ''

$r = Pedir -Uri "$BaseUrl/api/ventas" -Metodo 'POST' -Cuerpo $ventaNueva -Cabeceras $cabeceras
Registrar 'POST venta duplicada del mismo vehiculo' 409 $r.Codigo ''

$r = Pedir -Uri "$BaseUrl/api/vehiculos/$vehiculoId/conversion-usd"
Registrar 'GET conversion USD (servicio externo)' 200 $r.Codigo ''

Write-Host ''
Write-Host "== 6. Seguridad sin sesion ==" -ForegroundColor Cyan
$sinSesion = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$guardada = $sesion
$sesion = $sinSesion
$r = Pedir -Uri "$BaseUrl/api/clientes"
$sesion = $guardada
Registrar 'GET /api/clientes sin sesion' 401 $r.Codigo ''

Write-Host ''
Write-Host "== 7. Reglas de negocio de eliminacion ==" -ForegroundColor Cyan
if ($clienteId -gt 0) {
    $r = Pedir -Uri "$BaseUrl/api/clientes/$clienteId" -Metodo 'DELETE' -Cabeceras $cabeceras
    Registrar 'DELETE cliente con venta registrada' 409 $r.Codigo $r.Cuerpo.Trim()

    $r = Pedir -Uri "$BaseUrl/api/vehiculos/$vehiculoId" -Metodo 'DELETE' -Cabeceras $cabeceras
    Registrar 'DELETE vehiculo con venta registrada' 409 $r.Codigo ''
}

Write-Host ''
Write-Host "== Resumen ==" -ForegroundColor Cyan
$resultados | Format-Table -Property Prueba, Esperado, Obtenido, Estado, Detalle -AutoSize | Out-String | Write-Host
$fallas = @($resultados | Where-Object { $_.Estado -eq 'FALLA' }).Count
Write-Host "Pruebas: $($resultados.Count) | Fallas: $fallas" -ForegroundColor $(if ($fallas -eq 0) { 'Green' } else { 'Red' })
Write-Host ''
Write-Host "Datos de prueba creados en Supabase (se conservan a proposito):" -ForegroundColor DarkGray
Write-Host "  cliente id=$clienteId documento=TEST-$sufijo"
Write-Host "  vehiculo id=$vehiculoId placa=$placa (estado VENDIDO)"
Write-Host "  venta id=$ventaId"
Write-Host "  Para revertirlos, vea las instrucciones SQL del paso 7."
