"use strict";

const cop = new Intl.NumberFormat("es-CO", { style: "currency", currency: "COP", maximumFractionDigits: 0 });
const usd = new Intl.NumberFormat("es-CO", { style: "currency", currency: "USD" });
const formatoFecha = new Intl.DateTimeFormat("es-CO", { day: "numeric", month: "short", year: "numeric" });

const ETIQUETAS_ESTADO = {
    DISPONIBLE: "Disponible",
    VENDIDO: "Vendido",
    EN_MANTENIMIENTO: "En mantenimiento"
};

function etiquetaEstado(estado) {
    return ETIQUETAS_ESTADO[estado] ?? estado;
}

function tokenCsrf() {
    const token = document.querySelector('meta[name="_csrf"]')?.getAttribute("content");
    const encabezado = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content");
    return token && encabezado ? { token, encabezado } : null;
}

async function solicitar(url, options = {}) {
    const encabezados = { Accept: "application/json", ...options.headers };
    if (options.body) encabezados["Content-Type"] = "application/json";
    const metodo = (options.method || "GET").toUpperCase();
    if (!["GET", "HEAD", "OPTIONS", "TRACE"].includes(metodo)) {
        const csrf = tokenCsrf();
        if (csrf) encabezados[csrf.encabezado] = csrf.token;
    }

    let respuesta;
    try {
        respuesta = await fetch(url, { ...options, headers: encabezados });
    } catch {
        throw new Error("No hay conexión con el servidor. Revise la red e intente de nuevo.");
    }

    const cuerpo = respuesta.status === 204 ? null : await respuesta.json().catch(() => null);
    if (respuesta.status === 401) {
        window.location.assign("/login");
        throw new Error("La sesión de administrador venció o no es válida.");
    }
    if (!respuesta.ok) throw new Error(cuerpo?.mensaje || "La operación no pudo completarse.");
    return cuerpo;
}

function mostrarMensaje(id, texto = "", tipo = "") {
    const elemento = document.getElementById(id);
    if (!elemento) return;
    elemento.textContent = texto;
    elemento.className = `mensaje ${tipo}`;
}

function agregarCelda(fila, valor, clase = "") {
    const celda = document.createElement("td");
    if (clase) celda.className = clase;
    celda.textContent = valor ?? "—";
    fila.append(celda);
    return celda;
}

function crearEstado(estado) {
    const insignia = document.createElement("span");
    insignia.className = `estado estado--${estado.toLowerCase().replaceAll("_", "-")}`;
    insignia.textContent = etiquetaEstado(estado);
    return insignia;
}

function mostrarEstadoTabla(cuerpo, columnas, texto, reintentar) {
    cuerpo.replaceChildren();
    const fila = document.createElement("tr");
    fila.className = reintentar ? "estado-tabla estado-tabla--error" : "estado-tabla";
    const celda = document.createElement("td");
    celda.colSpan = columnas;

    const parrafo = document.createElement("p");
    parrafo.className = "estado-tabla__texto";
    parrafo.textContent = texto;
    celda.append(parrafo);

    if (reintentar) {
        const boton = document.createElement("button");
        boton.type = "button";
        boton.className = "secundario";
        boton.textContent = "Reintentar";
        boton.addEventListener("click", reintentar);
        celda.append(boton);
    }
    fila.append(celda);
    cuerpo.append(fila);
}

function mostrarVacio(cuerpo, columnas, texto) {
    mostrarEstadoTabla(cuerpo, columnas, texto);
}

function terminarCarga(cuerpo) {
    cuerpo.closest("[aria-busy]")?.setAttribute("aria-busy", "false");
}

function configurarNavegacion() {
    const boton = document.querySelector("[data-nav-toggle]");
    const navegacion = document.querySelector("[data-nav]");
    if (!boton || !navegacion) return;
    const rotulo = boton.querySelector(".sr-only");

    const actualizar = (abierto) => {
        boton.setAttribute("aria-expanded", String(abierto));
        navegacion.classList.toggle("is-open", abierto);
        if (rotulo) rotulo.textContent = abierto ? "Cerrar navegación" : "Abrir navegación";
    };

    boton.addEventListener("click", () => actualizar(boton.getAttribute("aria-expanded") !== "true"));
    navegacion.addEventListener("click", (evento) => {
        if (evento.target.closest("a")) actualizar(false);
    });
    document.addEventListener("keydown", (evento) => {
        if (evento.key === "Escape" && boton.getAttribute("aria-expanded") === "true") {
            actualizar(false);
            boton.focus();
        }
    });
}

function crearFichaInventario(vehiculo) {
    const ficha = document.createElement("article");
    ficha.className = "vehiculo-destacado";

    const placa = document.createElement("p");
    placa.className = "vehiculo-destacado__placa";
    placa.textContent = vehiculo.placa;

    const titulo = document.createElement("h3");
    titulo.textContent = `${vehiculo.marca} ${vehiculo.modelo}`;

    const detalle = document.createElement("p");
    detalle.className = "vehiculo-destacado__detalle";
    detalle.textContent = `${vehiculo.anio} · ${cop.format(vehiculo.precioCop)}`;

    const enlace = document.createElement("a");
    enlace.className = "vehiculo-destacado__enlace";
    enlace.href = "/vehiculos";
    enlace.textContent = "Ver detalle en inventario";

    ficha.append(placa, titulo, detalle, enlace);
    return ficha;
}

function mostrarInventarioInicio(vehiculos, error = "") {
    const contenedor = document.getElementById("vehiculos-destacados");
    if (!contenedor) return;
    contenedor.replaceChildren();
    contenedor.setAttribute("aria-busy", "false");

    if (error || !vehiculos.length) {
        const estado = document.createElement("p");
        estado.className = "estado-inventario";
        estado.textContent = error || "No hay vehículos disponibles por el momento.";
        contenedor.append(estado);
        return;
    }

    vehiculos.slice(0, 3).forEach((vehiculo) => contenedor.append(crearFichaInventario(vehiculo)));
}

function mensajeValidacion(campo) {
    const validez = campo.validity;
    if (validez.valueMissing) return campo.tagName === "SELECT" ? "Seleccione una opción." : "Este campo es obligatorio.";
    if (validez.typeMismatch) return campo.type === "email" ? "Escriba un correo válido, como nombre@dominio.co." : "El valor no es válido.";
    if (validez.patternMismatch) return campo.dataset.mensajePatron || "El formato no es válido.";
    if (validez.tooShort) return `Use al menos ${campo.minLength} caracteres.`;
    if (validez.rangeUnderflow || validez.rangeOverflow) {
        return campo.min && campo.max
            ? `Escriba un valor entre ${campo.min} y ${campo.max}.`
            : `El valor mínimo es ${campo.min}.`;
    }
    if (validez.stepMismatch || validez.badInput) return "Escriba un número entero, sin puntos ni comas.";
    return campo.validationMessage;
}

function ponerError(campo, texto = "") {
    const error = document.getElementById(`error-${campo.name}`);
    if (!error) return;
    error.textContent = texto;
    error.hidden = !texto;
    if (texto) campo.setAttribute("aria-invalid", "true");
    else campo.removeAttribute("aria-invalid");
}

function validarCampo(campo) {
    const texto = campo.checkValidity() ? "" : mensajeValidacion(campo);
    ponerError(campo, texto);
    return !texto;
}

function validarFormulario(formulario) {
    let primerInvalido = null;
    for (const campo of formulario.elements) {
        if (!campo.name || !campo.willValidate) continue;
        if (!validarCampo(campo) && !primerInvalido) primerInvalido = campo;
    }
    primerInvalido?.focus();
    return !primerInvalido;
}

function configurarFormulario(formulario) {
    const revalidar = (evento) => {
        if (evento.target.getAttribute?.("aria-invalid")) validarCampo(evento.target);
    };
    formulario.addEventListener("input", revalidar);
    formulario.addEventListener("change", revalidar);
}

function limpiarErrores(formulario) {
    for (const campo of formulario.elements) {
        if (campo.name) ponerError(campo);
    }
}

const CAMPOS_POR_MENSAJE = [
    [/placa/i, "placa"],
    [/correo/i, "email"],
    [/documento/i, "documento"],
    [/no está disponible/i, "vehiculoId"],
    [/vehículo vendido/i, "vehiculoId"]
];

function mostrarErrorServidor(formulario, mensajeId, error) {
    const texto = error.message;
    const invalidos = texto.match(/^Campos inválidos: (.+)\.$/);
    let primero = null;

    if (invalidos) {
        invalidos[1].split(", ").forEach((nombre) => {
            const campo = formulario.elements[nombre];
            if (!campo) return;
            ponerError(campo, "Revise este dato.");
            primero ??= campo;
        });
        mostrarMensaje(mensajeId, "Revise los campos marcados.", "error");
        primero?.focus();
        return;
    }

    const coincidencia = CAMPOS_POR_MENSAJE.find(([patron, nombre]) => patron.test(texto) && formulario.elements[nombre]);
    if (coincidencia) {
        const campo = formulario.elements[coincidencia[1]];
        ponerError(campo, texto);
        mostrarMensaje(mensajeId);
        campo.focus();
        return;
    }
    mostrarMensaje(mensajeId, texto, "error");
}

function activarEnvio(formulario, trabajando) {
    const boton = formulario.querySelector('button[type="submit"]');
    if (!boton) return;
    boton.dataset.textoOriginal ??= boton.textContent;
    boton.disabled = trabajando;
    boton.setAttribute("aria-busy", String(trabajando));
    boton.textContent = trabajando ? (boton.dataset.textoEnvio || "Enviando…") : boton.dataset.textoOriginal;
    formulario.setAttribute("aria-busy", String(trabajando));
}

function manejarEnvio(formulario, { ruta, mensajeId, exito, preparar = (datos) => datos, despues }) {
    formulario.addEventListener("submit", async (evento) => {
        evento.preventDefault();
        mostrarMensaje(mensajeId);
        if (!validarFormulario(formulario)) return;

        activarEnvio(formulario, true);
        try {
            await solicitar(ruta, { method: "POST", body: JSON.stringify(preparar(Object.fromEntries(new FormData(formulario)))) });
            formulario.reset();
            limpiarErrores(formulario);
            mostrarMensaje(mensajeId, exito, "exito");
            await despues();
        } catch (error) {
            mostrarErrorServidor(formulario, mensajeId, error);
        } finally {
            activarEnvio(formulario, false);
        }
    });
    configurarFormulario(formulario);
}

function botonEliminar(url, descripcion, onSuccess, mensajeId) {
    const boton = document.createElement("button");
    boton.type = "button";
    boton.className = "enlace-boton peligro";
    boton.textContent = "Eliminar";
    boton.setAttribute("aria-label", `Eliminar ${descripcion}`);
    boton.addEventListener("click", async () => {
        if (!window.confirm(`¿Eliminar ${descripcion}? Esta acción no se puede deshacer.`)) return;
        boton.disabled = true;
        try {
            await solicitar(url, { method: "DELETE" });
            mostrarMensaje(mensajeId, "Registro eliminado.", "exito");
            await onSuccess();
        } catch (error) {
            mostrarMensaje(mensajeId, error.message, "error");
            boton.disabled = false;
        }
    });
    return boton;
}

function botonConversionUsd(vehiculo) {
    const boton = document.createElement("button");
    boton.type = "button";
    boton.className = "enlace-boton";
    boton.textContent = "Ver USD";
    boton.setAttribute("aria-label", `Ver el precio en USD del vehículo ${vehiculo.placa}`);
    boton.addEventListener("click", async () => {
        boton.disabled = true;
        try {
            const conversion = await solicitar(`/api/vehiculos/${vehiculo.id}/conversion-usd`);
            mostrarMensaje("mensaje-vehiculos", `${vehiculo.placa}: ${usd.format(conversion.precioUsd)} (tasa: ${cop.format(conversion.tasaCopPorUsd)} por USD).`, "exito");
        } catch (error) {
            mostrarMensaje("mensaje-vehiculos", error.message, "error");
        } finally {
            boton.disabled = false;
        }
    });
    return boton;
}

async function cargarClientes() {
    const cuerpo = document.getElementById("tabla-clientes");
    if (!cuerpo) return [];
    try {
        const clientes = await solicitar("/api/clientes");
        if (!clientes.length) {
            mostrarVacio(cuerpo, 5, "Aún no hay clientes. Registre el primero con el formulario.");
            return clientes;
        }
        cuerpo.replaceChildren();
        clientes.forEach((cliente) => {
            const fila = document.createElement("tr");
            agregarCelda(fila, `${cliente.nombre} ${cliente.apellido}`);
            agregarCelda(fila, cliente.documento, "codigo");
            agregarCelda(fila, cliente.email);
            agregarCelda(fila, cliente.telefono, "sin-corte");
            const acciones = agregarCelda(fila, "", "acciones");
            acciones.replaceChildren(botonEliminar(`/api/clientes/${cliente.id}`, `al cliente ${cliente.nombre} ${cliente.apellido}`, cargarClientes, "mensaje-clientes"));
            cuerpo.append(fila);
        });
        return clientes;
    } catch {
        mostrarEstadoTabla(cuerpo, 5, "No se pudieron cargar los clientes.", cargarClientes);
        return [];
    } finally {
        terminarCarga(cuerpo);
    }
}

async function configurarClientes() {
    manejarEnvio(document.getElementById("form-cliente"), {
        ruta: "/api/clientes",
        mensajeId: "mensaje-clientes",
        exito: "Cliente registrado correctamente.",
        despues: cargarClientes
    });
    document.getElementById("recargar-clientes").addEventListener("click", cargarClientes);
    await cargarClientes();
}

async function cargarVehiculos(marca = "") {
    const cuerpo = document.getElementById("tabla-vehiculos");
    if (!cuerpo) return [];
    const ruta = marca ? `/api/vehiculos/marca/${encodeURIComponent(marca)}` : "/api/vehiculos";
    try {
        const vehiculos = await solicitar(ruta);
        if (!vehiculos.length) {
            mostrarVacio(cuerpo, 6, marca ? `Ninguna marca coincide con «${marca}».` : "Aún no hay vehículos. Regístrelos con el formulario.");
            return vehiculos;
        }
        cuerpo.replaceChildren();
        vehiculos.forEach((vehiculo) => {
            const fila = document.createElement("tr");
            agregarCelda(fila, vehiculo.placa, "placa");
            agregarCelda(fila, `${vehiculo.marca} ${vehiculo.modelo}`);
            agregarCelda(fila, vehiculo.anio, "num");
            agregarCelda(fila, cop.format(vehiculo.precioCop), "num");
            agregarCelda(fila, "").replaceChildren(crearEstado(vehiculo.estado));
            const acciones = agregarCelda(fila, "", "acciones");
            acciones.replaceChildren(botonConversionUsd(vehiculo));
            if (vehiculo.estado === "DISPONIBLE") {
                acciones.append(botonEliminar(`/api/vehiculos/${vehiculo.id}`, `el vehículo ${vehiculo.placa}`, () => cargarVehiculos(marca), "mensaje-vehiculos"));
            }
            cuerpo.append(fila);
        });
        return vehiculos;
    } catch {
        mostrarEstadoTabla(cuerpo, 6, "No se pudo cargar el inventario.", () => cargarVehiculos(marca));
        return [];
    } finally {
        terminarCarga(cuerpo);
    }
}

async function configurarVehiculos() {
    manejarEnvio(document.getElementById("form-vehiculo"), {
        ruta: "/api/vehiculos",
        mensajeId: "mensaje-vehiculos",
        exito: "Vehículo registrado correctamente.",
        preparar: (datos) => ({
            ...datos,
            placa: datos.placa.trim().toUpperCase(),
            anio: Number(datos.anio),
            precioCop: Number(datos.precioCop)
        }),
        despues: () => cargarVehiculos()
    });
    document.getElementById("form-filtro-marca").addEventListener("submit", (evento) => {
        evento.preventDefault();
        cargarVehiculos(document.getElementById("marca-filtro").value.trim());
    });
    document.getElementById("limpiar-filtro").addEventListener("click", () => {
        document.getElementById("marca-filtro").value = "";
        cargarVehiculos();
    });
    document.getElementById("recargar-vehiculos").addEventListener("click", () => cargarVehiculos());
    await cargarVehiculos();
}

function llenarOpciones(select, elementos, etiqueta, vacio) {
    select.replaceChildren();
    const inicial = document.createElement("option");
    inicial.value = "";
    inicial.textContent = elementos.length ? "Seleccione una opción" : vacio;
    select.append(inicial);
    elementos.forEach((elemento) => {
        const opcion = document.createElement("option");
        opcion.value = elemento.id;
        opcion.textContent = etiqueta(elemento);
        select.append(opcion);
    });
}

function fallarOpciones(...ids) {
    ids.forEach((id) => {
        const select = document.getElementById(id);
        if (select) llenarOpciones(select, [], () => "", "No se pudieron cargar las opciones");
    });
}

async function cargarDatosVenta() {
    const [clientes, vehiculos] = await Promise.all([
        solicitar("/api/clientes"),
        solicitar("/api/vehiculos/disponibles")
    ]);
    llenarOpciones(document.getElementById("cliente-id"), clientes, (c) => `${c.nombre} ${c.apellido}`, "Aún no hay clientes registrados");
    llenarOpciones(document.getElementById("vehiculo-id"), vehiculos, (v) => `${v.placa} · ${v.marca} ${v.modelo}`, "No hay vehículos disponibles");
}

async function cargarVentas() {
    const cuerpo = document.getElementById("tabla-ventas");
    try {
        const ventas = await solicitar("/api/ventas");
        if (!ventas.length) {
            mostrarVacio(cuerpo, 5, "Aún no hay ventas. Aparecerán aquí cuando registre la primera.");
            return;
        }
        cuerpo.replaceChildren();
        ventas.forEach((venta) => {
            const fila = document.createElement("tr");
            agregarCelda(fila, formatoFecha.format(new Date(venta.fechaVenta)), "sin-corte");
            agregarCelda(fila, venta.clienteNombre);
            agregarCelda(fila, venta.vehiculoDescripcion);
            agregarCelda(fila, venta.descuentoAplicado > 0 ? cop.format(venta.descuentoAplicado) : "—", "num");
            agregarCelda(fila, cop.format(venta.montoTotal), "num");
            cuerpo.append(fila);
        });
    } catch {
        mostrarEstadoTabla(cuerpo, 5, "No se pudieron cargar las ventas.", cargarVentas);
    } finally {
        terminarCarga(cuerpo);
    }
}

async function cargarResumenVentas() {
    const totalVentas = document.getElementById("resumen-total-ventas");
    if (!totalVentas) return;
    const resumen = await solicitar("/api/reportes/resumen");
    totalVentas.textContent = resumen.totalVentas;
    document.getElementById("resumen-monto-ventas").textContent = cop.format(resumen.montoTotalVentas);
    document.getElementById("resumen-mantenimiento").textContent = resumen.vehiculosEnMantenimiento;
}

async function refrescarVentas() {
    try {
        await Promise.all([cargarVentas(), cargarDatosVenta(), cargarResumenVentas()]);
    } catch (error) {
        fallarOpciones("cliente-id", "vehiculo-id");
        mostrarMensaje("mensaje-ventas", error.message, "error");
    }
}

async function configurarVentas() {
    manejarEnvio(document.getElementById("form-venta"), {
        ruta: "/api/ventas",
        mensajeId: "mensaje-ventas",
        exito: "Venta registrada. El vehículo quedó marcado como vendido.",
        preparar: (datos) => ({ clienteId: Number(datos.clienteId), vehiculoId: Number(datos.vehiculoId) }),
        despues: refrescarVentas
    });
    document.getElementById("recargar-ventas").addEventListener("click", refrescarVentas);
    await refrescarVentas();
}

async function cargarDatosMantenimiento() {
    const vehiculos = await solicitar("/api/vehiculos");
    const mantenibles = vehiculos.filter((vehiculo) => vehiculo.estado !== "VENDIDO");
    llenarOpciones(
        document.getElementById("mantenimiento-vehiculo-id"),
        mantenibles,
        (vehiculo) => `${vehiculo.placa} · ${vehiculo.marca} ${vehiculo.modelo} (${etiquetaEstado(vehiculo.estado).toLowerCase()})`,
        "No hay vehículos para mantenimiento"
    );
}

async function cargarMantenimientos() {
    const cuerpo = document.getElementById("tabla-mantenimientos");
    try {
        const mantenimientos = await solicitar("/api/mantenimientos");
        if (!mantenimientos.length) {
            mostrarVacio(cuerpo, 4, "Aún no hay mantenimientos registrados.");
            return;
        }
        cuerpo.replaceChildren();
        [...mantenimientos].sort((a, b) => b.fechaMantenimiento.localeCompare(a.fechaMantenimiento)).forEach((mantenimiento) => {
            const fila = document.createElement("tr");
            agregarCelda(fila, formatoFecha.format(new Date(`${mantenimiento.fechaMantenimiento}T00:00:00`)), "sin-corte");
            agregarCelda(fila, mantenimiento.vehiculoDescripcion);
            agregarCelda(fila, mantenimiento.descripcion);
            agregarCelda(fila, cop.format(mantenimiento.costo), "num");
            cuerpo.append(fila);
        });
    } catch {
        mostrarEstadoTabla(cuerpo, 4, "No se pudo cargar el historial de mantenimientos.", cargarMantenimientos);
    } finally {
        terminarCarga(cuerpo);
    }
}

async function refrescarMantenimientos() {
    try {
        await Promise.all([cargarMantenimientos(), cargarDatosMantenimiento()]);
    } catch (error) {
        fallarOpciones("mantenimiento-vehiculo-id");
        mostrarMensaje("mensaje-mantenimientos", error.message, "error");
    }
}

async function configurarMantenimientos() {
    manejarEnvio(document.getElementById("form-mantenimiento"), {
        ruta: "/api/mantenimientos",
        mensajeId: "mensaje-mantenimientos",
        exito: "Mantenimiento registrado. El vehículo quedó en mantenimiento.",
        preparar: (datos) => ({ ...datos, vehiculoId: Number(datos.vehiculoId), costo: Number(datos.costo) }),
        despues: refrescarMantenimientos
    });
    document.getElementById("recargar-mantenimientos").addEventListener("click", refrescarMantenimientos);
    await refrescarMantenimientos();
}

async function configurarInicio() {
    try {
        const [clientes, disponibles, ventas] = await Promise.all([
            solicitar("/api/clientes"),
            solicitar("/api/vehiculos/disponibles"),
            solicitar("/api/ventas")
        ]);
        document.getElementById("total-clientes").textContent = clientes.length;
        document.getElementById("total-disponibles").textContent = disponibles.length;
        document.getElementById("total-ventas").textContent = ventas.length;
        mostrarInventarioInicio(disponibles);
    } catch (error) {
        mostrarMensaje("mensaje-inicio", error.message, "error");
        mostrarInventarioInicio([], "No fue posible consultar el inventario disponible.");
    }
}

document.addEventListener("DOMContentLoaded", async () => {
    configurarNavegacion();
    const configuraciones = {
        inicio: configurarInicio,
        clientes: configurarClientes,
        vehiculos: configurarVehiculos,
        ventas: configurarVentas,
        mantenimientos: configurarMantenimientos
    };
    const configurar = configuraciones[document.body.dataset.page];
    if (configurar) await configurar();
});
