"use strict";

const cop = new Intl.NumberFormat("es-CO", { style: "currency", currency: "COP", maximumFractionDigits: 0 });

async function solicitar(url, options = {}) {
    const encabezados = { Accept: "application/json", ...options.headers };
    if (options.body) encabezados["Content-Type"] = "application/json";

    let respuesta;
    try {
        respuesta = await fetch(url, { ...options, headers: encabezados });
    } catch {
        throw new Error("No fue posible conectar con la API REST.");
    }

    const cuerpo = respuesta.status === 204 ? null : await respuesta.json().catch(() => null);
    if (!respuesta.ok) throw new Error(cuerpo?.mensaje || "La operación no pudo completarse.");
    return cuerpo;
}

function mostrarMensaje(id, texto = "", tipo = "") {
    const elemento = document.getElementById(id);
    if (!elemento) return;
    elemento.textContent = texto;
    elemento.className = `mensaje ${tipo}`;
}

function agregarCelda(fila, valor) {
    const celda = document.createElement("td");
    celda.textContent = valor ?? "—";
    fila.append(celda);
}

function mostrarVacio(cuerpo, columnas, texto) {
    cuerpo.replaceChildren();
    const fila = document.createElement("tr");
    const celda = document.createElement("td");
    celda.colSpan = columnas;
    celda.className = "fila-vacia";
    celda.textContent = texto;
    fila.append(celda);
    cuerpo.append(fila);
}

function activarEnvio(formulario, trabajando) {
    const boton = formulario.querySelector('button[type="submit"]');
    boton.disabled = trabajando;
    formulario.setAttribute("aria-busy", String(trabajando));
}

function botonEliminar(url, onSuccess, mensajeId) {
    const boton = document.createElement("button");
    boton.type = "button";
    boton.className = "enlace-boton peligro";
    boton.textContent = "Eliminar";
    boton.addEventListener("click", async () => {
        if (!window.confirm("¿Desea eliminar este registro?")) return;
        boton.disabled = true;
        try {
            await solicitar(url, { method: "DELETE" });
            mostrarMensaje(mensajeId, "Registro eliminado.", "exito");
            await onSuccess();
        } catch (error) {
            mostrarMensaje(mensajeId, error.message, "error");
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
            mostrarVacio(cuerpo, 4, "Aún no hay clientes registrados.");
            return clientes;
        }
        cuerpo.replaceChildren();
        clientes.forEach((cliente) => {
            const fila = document.createElement("tr");
            agregarCelda(fila, `${cliente.nombre} ${cliente.apellido}`);
            agregarCelda(fila, cliente.email);
            agregarCelda(fila, cliente.telefono);
            const acciones = document.createElement("td");
            acciones.append(botonEliminar(`/api/clientes/${cliente.id}`, cargarClientes, "mensaje-clientes"));
            fila.append(acciones);
            cuerpo.append(fila);
        });
        return clientes;
    } catch (error) {
        mostrarMensaje("mensaje-clientes", error.message, "error");
        mostrarVacio(cuerpo, 4, "No se pudieron cargar los clientes.");
        return [];
    }
}

async function configurarClientes() {
    const formulario = document.getElementById("form-cliente");
    formulario.addEventListener("submit", async (evento) => {
        evento.preventDefault();
        activarEnvio(formulario, true);
        const datos = Object.fromEntries(new FormData(formulario));
        try {
            await solicitar("/api/clientes", { method: "POST", body: JSON.stringify(datos) });
            formulario.reset();
            mostrarMensaje("mensaje-clientes", "Cliente registrado correctamente.", "exito");
            await cargarClientes();
        } catch (error) {
            mostrarMensaje("mensaje-clientes", error.message, "error");
        } finally {
            activarEnvio(formulario, false);
        }
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
            mostrarVacio(cuerpo, 6, "No se encontraron vehículos.");
            return vehiculos;
        }
        cuerpo.replaceChildren();
        vehiculos.forEach((vehiculo) => {
            const fila = document.createElement("tr");
            agregarCelda(fila, vehiculo.placa);
            agregarCelda(fila, `${vehiculo.marca} ${vehiculo.modelo}`);
            agregarCelda(fila, vehiculo.anio);
            agregarCelda(fila, cop.format(vehiculo.precioCop));
            agregarCelda(fila, vehiculo.estado.replaceAll("_", " "));
            const acciones = document.createElement("td");
            if (vehiculo.estado === "DISPONIBLE") {
                acciones.append(botonEliminar(`/api/vehiculos/${vehiculo.id}`, () => cargarVehiculos(marca), "mensaje-vehiculos"));
            }
            fila.append(acciones);
            cuerpo.append(fila);
        });
        return vehiculos;
    } catch (error) {
        mostrarMensaje("mensaje-vehiculos", error.message, "error");
        mostrarVacio(cuerpo, 6, "No se pudo cargar el inventario.");
        return [];
    }
}

async function configurarVehiculos() {
    const formulario = document.getElementById("form-vehiculo");
    formulario.addEventListener("submit", async (evento) => {
        evento.preventDefault();
        activarEnvio(formulario, true);
        const datos = Object.fromEntries(new FormData(formulario));
        datos.anio = Number(datos.anio);
        datos.precioCop = Number(datos.precioCop);
        try {
            await solicitar("/api/vehiculos", { method: "POST", body: JSON.stringify(datos) });
            formulario.reset();
            mostrarMensaje("mensaje-vehiculos", "Vehículo registrado correctamente.", "exito");
            await cargarVehiculos();
        } catch (error) {
            mostrarMensaje("mensaje-vehiculos", error.message, "error");
        } finally {
            activarEnvio(formulario, false);
        }
    });
    const filtro = document.getElementById("form-filtro-marca");
    filtro.addEventListener("submit", (evento) => {
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

async function cargarDatosVenta() {
    const [clientes, vehiculos] = await Promise.all([
        solicitar("/api/clientes"),
        solicitar("/api/vehiculos/disponibles")
    ]);
    llenarOpciones(document.getElementById("cliente-id"), clientes, (c) => `${c.nombre} ${c.apellido}`, "No hay clientes");
    llenarOpciones(document.getElementById("vehiculo-id"), vehiculos, (v) => `${v.placa} — ${v.marca} ${v.modelo}`, "No hay vehículos disponibles");
}

async function cargarVentas() {
    const cuerpo = document.getElementById("tabla-ventas");
    try {
        const ventas = await solicitar("/api/ventas");
        if (!ventas.length) return mostrarVacio(cuerpo, 5, "Aún no hay ventas registradas.");
        cuerpo.replaceChildren();
        ventas.forEach((venta) => {
            const fila = document.createElement("tr");
            agregarCelda(fila, new Date(venta.fechaVenta).toLocaleDateString("es-CO"));
            agregarCelda(fila, venta.clienteNombre);
            agregarCelda(fila, venta.vehiculoDescripcion);
            agregarCelda(fila, cop.format(venta.descuentoAplicado));
            agregarCelda(fila, cop.format(venta.montoTotal));
            cuerpo.append(fila);
        });
    } catch (error) {
        mostrarMensaje("mensaje-ventas", error.message, "error");
        mostrarVacio(cuerpo, 5, "No se pudieron cargar las ventas.");
    }
}

async function configurarVentas() {
    const formulario = document.getElementById("form-venta");
    formulario.addEventListener("submit", async (evento) => {
        evento.preventDefault();
        activarEnvio(formulario, true);
        const datos = Object.fromEntries(new FormData(formulario));
        datos.clienteId = Number(datos.clienteId);
        datos.vehiculoId = Number(datos.vehiculoId);
        try {
            await solicitar("/api/ventas", { method: "POST", body: JSON.stringify(datos) });
            formulario.reset();
            mostrarMensaje("mensaje-ventas", "Venta registrada y vehículo marcado como vendido.", "exito");
            await Promise.all([cargarVentas(), cargarDatosVenta()]);
        } catch (error) {
            mostrarMensaje("mensaje-ventas", error.message, "error");
        } finally {
            activarEnvio(formulario, false);
        }
    });
    document.getElementById("recargar-ventas").addEventListener("click", async () => {
        try {
            await Promise.all([cargarVentas(), cargarDatosVenta()]);
        } catch (error) {
            mostrarMensaje("mensaje-ventas", error.message, "error");
        }
    });
    try {
        await Promise.all([cargarVentas(), cargarDatosVenta()]);
    } catch (error) {
        mostrarMensaje("mensaje-ventas", error.message, "error");
    }
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
    } catch (error) {
        mostrarMensaje("mensaje-inicio", error.message, "error");
    }
}

document.addEventListener("DOMContentLoaded", async () => {
    const configuraciones = {
        inicio: configurarInicio,
        clientes: configurarClientes,
        vehiculos: configurarVehiculos,
        ventas: configurarVentas
    };
    const configurar = configuraciones[document.body.dataset.page];
    if (configurar) await configurar();
});
