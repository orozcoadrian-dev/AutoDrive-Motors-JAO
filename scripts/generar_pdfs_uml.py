"""Genera PDFs legibles de los diagramas UML de AutoDrive Motors JAO."""

from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.pagesizes import A3, landscape
from reportlab.pdfbase.pdfmetrics import stringWidth
from reportlab.pdfgen import canvas


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "output" / "pdf"
PAGE_W, PAGE_H = landscape(A3)
NAVY = colors.HexColor("#12355B")
BLUE = colors.HexColor("#DCEBFA")
GREEN = colors.HexColor("#E0F2E9")
YELLOW = colors.HexColor("#FFF4CC")
RED = colors.HexColor("#FCE4E4")
GRAY = colors.HexColor("#5A6772")


def lines_for(text, width, font="Helvetica", size=9):
    words = text.split()
    lines, current = [], ""
    for word in words:
        candidate = word if not current else f"{current} {word}"
        if stringWidth(candidate, font, size) <= width:
            current = candidate
        else:
            if current:
                lines.append(current)
            current = word
    if current:
        lines.append(current)
    return lines or [""]


def draw_wrapped(c, text, x, y, width, size=9, leading=12, font="Helvetica", color=colors.black):
    c.setFillColor(color)
    c.setFont(font, size)
    for line in lines_for(text, width, font, size):
        c.drawString(x, y, line)
        y -= leading
    return y


def title(c, value, page):
    c.setFillColor(NAVY)
    c.setFont("Helvetica-Bold", 20)
    c.drawString(48, PAGE_H - 42, value)
    c.setStrokeColor(colors.HexColor("#9DB9D1"))
    c.line(48, PAGE_H - 52, PAGE_W - 48, PAGE_H - 52)
    c.setFillColor(GRAY)
    c.setFont("Helvetica", 8)
    c.drawRightString(PAGE_W - 48, 28, f"AutoDrive Motors JAO | 2026-10-06 | Página {page}")


def card(c, x, y, width, height, stereotype, name, members, fill=BLUE):
    c.setFillColor(fill)
    c.setStrokeColor(NAVY)
    c.roundRect(x, y, width, height, 8, fill=1, stroke=1)
    c.setFillColor(NAVY)
    c.setFont("Helvetica", 8)
    c.drawCentredString(x + width / 2, y + height - 15, stereotype)
    c.setFont("Helvetica-Bold", 11)
    c.drawCentredString(x + width / 2, y + height - 31, name)
    c.setStrokeColor(colors.HexColor("#7AA0C3"))
    c.line(x + 7, y + height - 38, x + width - 7, y + height - 38)
    cursor = y + height - 51
    c.setFillColor(colors.black)
    c.setFont("Helvetica", 8)
    for member in members:
        for line in lines_for(member, width - 16, "Helvetica", 8):
            if cursor < y + 10:
                return
            c.drawString(x + 8, cursor, line)
            cursor -= 10


def arrow(c, x1, y1, x2, y2, label="", dashed=False):
    c.setStrokeColor(NAVY)
    c.setFillColor(NAVY)
    c.setLineWidth(1)
    if dashed:
        c.setDash(4, 3)
    c.line(x1, y1, x2, y2)
    c.setDash()
    angle = 0
    if x2 != x1:
        angle = 1 if x2 > x1 else -1
    c.line(x2, y2, x2 - 7 * angle - 3, y2 + 4)
    c.line(x2, y2, x2 - 7 * angle - 3, y2 - 4)
    if label:
        c.setFont("Helvetica", 7)
        c.drawCentredString((x1 + x2) / 2, (y1 + y2) / 2 + 5, label)


def class_pdf(path):
    c = canvas.Canvas(str(path), pagesize=landscape(A3))

    title(c, "Diagrama de clases de implementación - visión por capas", 1)
    c.setFillColor(GRAY)
    c.setFont("Helvetica", 10)
    c.drawString(48, PAGE_H - 72, "Controladores REST y vistas MVC delegan la lógica de negocio a servicios; los servicios usan DAO/JPA y DTOs.")
    names = [
        ("ClienteController", "ClienteService", "ClienteDao"),
        ("VehiculoController", "VehiculoService", "VehiculoDao"),
        ("VentaController", "VentaService", "VentaDao"),
        ("MantenimientoController", "MantenimientoService", "MantenimientoDao"),
        ("DivisaController", "TasaCambioService", "TasaCambioGateway"),
    ]
    xs = [55, 282, 509, 736, 963]
    for index, (controller, service, persistence) in enumerate(names):
        x = xs[index]
        card(c, x, 565, 172, 105, "«RestController>" if index < 4 else "«RestController>", controller,
             ["HTTP JSON", "recibe y devuelve DTOs"], BLUE)
        card(c, x, 350, 172, 125, "«Service>", service,
             ["reglas de negocio", "validación de flujo", "transacción cuando aplica"], GREEN)
        card(c, x, 140, 172, 105, "«DAO/JPA>" if index < 4 else "«Client HTTP>", persistence,
             ["acceso a datos" if index < 4 else "consume tasa externa", "sin lógica de negocio"], YELLOW)
        arrow(c, x + 86, 565, x + 86, 475, "llama")
        arrow(c, x + 86, 350, x + 86, 245, "usa")
    c.setFillColor(colors.HexColor("#F7FAFC"))
    c.setStrokeColor(colors.HexColor("#7AA0C3"))
    c.roundRect(54, 65, PAGE_W - 108, 48, 8, fill=1, stroke=1)
    c.setFillColor(NAVY)
    c.setFont("Helvetica-Bold", 10)
    c.drawString(70, 94, "Modelo compartido")
    c.setFont("Helvetica", 9)
    c.drawString(70, 77, "Cliente, Vehiculo, Venta, Mantenimiento, EstadoVehiculo, DTOs, mapeadores y GlobalExceptionHandler.")
    c.showPage()

    title(c, "Clases de la aplicación - control, servicio y persistencia", 2)
    columns = [
        (70, "Clientes", "ClienteController", "ClienteService", "ClienteDao", ["registrar(dto)", "listar()", "actualizar(id, dto)", "eliminar(id)"],
         ["validarCorreoUnico(email)", "convierte con ClienteMapper"], ["extends JpaRepository<Cliente, Long>", "existsByEmail(email)"], "Cliente"),
        (400, "Vehículos", "VehiculoController", "VehiculoService", "VehiculoDao", ["registrar(dto)", "listarDisponibles()", "listarPorMarca(marca)"],
         ["validarPlacaUnica(placa)", "precio no negativo"], ["extends JpaRepository<Vehiculo, Long>", "findByEstado(estado)"], "Vehiculo"),
        (730, "Ventas", "VentaController", "VentaService", "VentaDao", ["registrar(dto)", "listar()", "obtenerPorId(id)"],
         ["@Transactional", "valida disponibilidad", "calcula descuento y total"], ["extends JpaRepository<Venta, Long>", "findAll()"], "Venta"),
    ]
    for x, label, controller, service, dao, controller_members, service_members, dao_members, entity in columns:
        c.setFillColor(NAVY)
        c.setFont("Helvetica-Bold", 12)
        c.drawCentredString(x + 135, 742, label)
        card(c, x, 555, 270, 140, "«RestController>", controller, controller_members, BLUE)
        card(c, x, 335, 270, 155, "«Service>", service, service_members, GREEN)
        card(c, x, 130, 270, 125, "«DAO/JPA>", dao, dao_members, YELLOW)
        arrow(c, x + 135, 555, x + 135, 490, "DTO")
        arrow(c, x + 135, 335, x + 135, 255, "entidad")
        c.setFillColor(GRAY)
        c.setFont("Helvetica", 8)
        c.drawCentredString(x + 135, 112, f"Persiste {entity}; el controlador no accede al DAO.")
    c.showPage()

    title(c, "Modelo, DTOs y relaciones de negocio", 3)
    card(c, 55, 465, 250, 195, "«Entity>", "Cliente", ["id: Long", "nombre, apellido: String", "email: String {unique}", "telefono: String", "fechaRegistro: LocalDateTime"], YELLOW)
    card(c, 470, 465, 250, 195, "«Entity>", "Vehiculo", ["id: Long", "placa: String {unique}", "marca, modelo: String", "anio: Integer", "precioCop: BigDecimal", "estado: EstadoVehiculo"], YELLOW)
    card(c, 885, 465, 250, 195, "«Entity>", "Venta", ["id: Long", "cliente: Cliente", "vehiculo: Vehiculo {unique}", "fechaVenta: LocalDateTime", "descuentoAplicado, montoTotal: BigDecimal"], YELLOW)
    card(c, 55, 175, 250, 190, "«DTO>", "DTOs de entrada y salida", ["ClienteRequestDTO / ClienteResponseDTO", "VehiculoRequestDTO / VehiculoResponseDTO", "VentaRequestDTO(clienteId, vehiculoId)", "VentaResponseDTO", "ConversionUsdDTO(valorCop, tasa, valorUsd)"], RED)
    card(c, 470, 175, 250, 190, "«Component>", "Mappers y cálculos", ["ClienteMapper y VehiculoMapper", "VentaMapper", "CalculoVentaService", "ResultadoCalculoVenta"], GREEN)
    card(c, 885, 175, 250, 190, "«Advice>", "Errores y estados", ["GlobalExceptionHandler", "404 recurso no encontrado", "409 regla de negocio", "503 servicio externo", "EstadoVehiculo: DISPONIBLE, VENDIDO, EN_MANTENIMIENTO"], BLUE)
    c.setFillColor(GRAY)
    c.setFont("Helvetica-Bold", 8)
    c.drawString(55, 440, "Relaciones persistentes")
    c.setFont("Helvetica", 8)
    c.drawString(55, 427, "Cliente 1 - 0..* Venta")
    c.drawString(55, 414, "Vehículo 1 - 0..1 Venta")
    arrow(c, 595, 465, 595, 365, "se transforma con")
    c.setFillColor(GRAY)
    c.setFont("Helvetica", 9)
    draw_wrapped(c, "Reglas: solo se vende un vehículo DISPONIBLE; si el precio supera 100.000.000 COP se aplica 5 %; fecha y total se generan en el servicio.", 55, 90, PAGE_W - 110, 9, 12)
    c.save()


def participant(c, x, label):
    c.setFillColor(BLUE)
    c.setStrokeColor(NAVY)
    c.roundRect(x - 55, 710, 110, 34, 6, fill=1, stroke=1)
    c.setFillColor(NAVY)
    c.setFont("Helvetica-Bold", 8)
    cursor = 730
    for line in lines_for(label, 98, "Helvetica-Bold", 8):
        c.drawCentredString(x, cursor, line)
        cursor -= 9
    c.setDash(3, 3)
    c.setStrokeColor(colors.HexColor("#8797A6"))
    c.line(x, 695, x, 100)
    c.setDash()


def message(c, xs, y, sender, receiver, text, return_message=False):
    x1, x2 = xs[sender], xs[receiver]
    if sender == receiver:
        c.setStrokeColor(NAVY)
        c.line(x1, y, x1 + 24, y)
        c.line(x1 + 24, y, x1 + 24, y - 18)
        c.line(x1 + 24, y - 18, x1, y - 18)
        c.line(x1, y - 18, x1 + 7, y - 14)
        c.line(x1, y - 18, x1 + 7, y - 22)
        c.setFillColor(colors.black)
        c.setFont("Helvetica", 7.4)
        cursor = y + 6
        for line in lines_for(text, 95, "Helvetica", 7.4):
            c.drawString(x1 + 30, cursor, line)
            cursor += 8
        return
    c.setStrokeColor(NAVY if not return_message else GRAY)
    if return_message:
        c.setDash(4, 3)
    c.line(x1, y, x2, y)
    c.setDash()
    direction = 1 if x2 > x1 else -1
    c.line(x2, y, x2 - 7 * direction - 3, y + 4)
    c.line(x2, y, x2 - 7 * direction - 3, y - 4)
    c.setFillColor(colors.black)
    c.setFont("Helvetica", 7.4)
    for index, line in enumerate(lines_for(text, abs(x2 - x1) - 16, "Helvetica", 7.4)):
        c.drawCentredString((x1 + x2) / 2, y + 6 + index * 8, line)


def note(c, y, text, x=55, width=None):
    width = width or PAGE_W - 110
    c.setFillColor(colors.HexColor("#FFF9DD"))
    c.setStrokeColor(colors.HexColor("#C8AC48"))
    c.roundRect(x, y - 13, width, 22, 5, fill=1, stroke=1)
    c.setFillColor(colors.HexColor("#66571B"))
    c.setFont("Helvetica-Oblique", 8)
    c.drawString(x + 8, y - 5, text)


def sequence_page(c, page, heading, participant_labels, events):
    title(c, heading, page)
    count = len(participant_labels)
    margin = 85
    xs = [margin + i * ((PAGE_W - 2 * margin) / (count - 1)) for i in range(count)]
    for x, label in zip(xs, participant_labels):
        participant(c, x, label)
    y = 670
    for event in events:
        if event[0] == "note":
            note(c, y, event[1])
            y -= 34
            continue
        sender, receiver, text, is_return = event
        message(c, xs, y, sender, receiver, text, is_return)
        y -= 31
    c.setFillColor(GRAY)
    c.setFont("Helvetica", 8)
    c.drawString(55, 70, "Línea continua: llamada. Línea discontinua: respuesta. Los bloques amarillos indican regla, alternativa o límite transaccional.")
    c.showPage()


def sequence_pdf(path):
    c = canvas.Canvas(str(path), pagesize=landscape(A3))
    sequence_page(c, 1, "Diagrama de secuencia - Registrar venta (CU-12)",
                  ["Vendedor", "VentaController", "VentaService", "ClienteDao", "VehiculoDao", "CalculoVentaService", "VentaDao", "ExceptionHandler"],
                  [
                      (0, 1, "POST /api/ventas {clienteId, vehiculoId}", False),
                      (1, 2, "registrar(dto)", False),
                      ("note", "Inicio de transacción: venta y cambio de estado deben confirmarse o revertirse juntos."),
                      (2, 3, "findById(clienteId)", False),
                      (3, 2, "Cliente", True),
                      (2, 4, "findById(vehiculoId)", False),
                      (4, 2, "Vehiculo", True),
                      ("note", "Alternativa: si cliente o vehículo no existe, se responde 404 y no se crea venta."),
                      (2, 5, "calcular(precioCop)", False),
                      (5, 2, "total y descuento (5 % solo si supera el umbral)", True),
                      ("note", "Alternativa: si estado no es DISPONIBLE, se responde 409 y no se cambia el vehículo."),
                      (2, 2, "asignar fecha y estado VENDIDO", False),
                      (2, 6, "save(venta)", False),
                      (6, 2, "Venta persistida", True),
                      (2, 1, "VentaResponseDTO", True),
                      (1, 0, "201 Created + JSON", True),
                  ])
    sequence_page(c, 2, "Diagrama de secuencia - Registrar mantenimiento (CU-21)",
                  ["Vendedor", "MantenimientoController", "MantenimientoService", "VehiculoDao", "MantenimientoDao", "ExceptionHandler"],
                  [
                      (0, 1, "POST /api/mantenimientos {vehiculoId, fecha, descripción}", False),
                      (1, 1, "validar DTO", False),
                      ("note", "Alternativa: DTO inválido -> 400 JSON controlado, sin invocar el servicio."),
                      (1, 2, "registrar(dto)", False),
                      ("note", "Inicio de transacción: mantenimiento y estado EN_MANTENIMIENTO son una sola operación."),
                      (2, 3, "findById(vehiculoId)", False),
                      (3, 2, "Vehiculo", True),
                      ("note", "Alternativa: vehículo inexistente -> 404 y no se registra mantenimiento."),
                      (2, 2, "cambiar estado a EN_MANTENIMIENTO", False),
                      (2, 4, "save(mantenimiento)", False),
                      (4, 2, "Mantenimiento persistido", True),
                      (2, 1, "MantenimientoResponseDTO", True),
                      (1, 0, "201 Created + JSON", True),
                  ])
    sequence_page(c, 3, "Diagrama de secuencia - Convertir vehículo a USD (CU-25)",
                  ["Vendedor", "DivisaController", "TasaCambioService", "VehiculoDao", "TasaCambioGateway", "API externa", "ExceptionHandler"],
                  [
                      (0, 1, "GET /api/divisas/vehiculos/{id}/usd", False),
                      (1, 2, "convertirVehiculoAUsd(id)", False),
                      (2, 3, "findById(id)", False),
                      (3, 2, "Vehiculo con precio COP", True),
                      ("note", "Alternativa: vehículo inexistente -> 404 JSON controlado."),
                      (2, 4, "obtenerTasaCopUsd()", False),
                      (4, 5, "GET HTTP", False),
                      (5, 4, "JSON con tasa COP/USD", True),
                      (4, 2, "BigDecimal tasa", True),
                      (2, 2, "valorUsd = valorCop / tasa", False),
                      (2, 1, "ConversionUsdDTO", True),
                      (1, 0, "200 {valorCop, tasa, valorUsd}", True),
                      ("note", "Alternativa: timeout, HTTP erróneo o JSON inválido -> ServicioExternoException y 503 controlado."),
                  ])
    c.save()


if __name__ == "__main__":
    OUTPUT.mkdir(parents=True, exist_ok=True)
    class_pdf(OUTPUT / "diagrama-de-clases-aplicacion.pdf")
    sequence_pdf(OUTPUT / "diagramas-de-secuencia.pdf")
