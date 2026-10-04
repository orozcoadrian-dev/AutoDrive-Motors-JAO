<!-- BANNER DEL PROYECTO -->
<p align="center">
  <img src="https://github.com/orozcoadrian-dev/AutoDrive-Motors-JAO/blob/d50187dd6dccfc492d49b6524427c4d831fdfe42/01_Documentacion/00_branding/Banner-Automotriz.png" alt="AutoDrive Motors Banner">
</p>

<h1 align="center">AutoDrive Motors</h1>

<p align="center">
  <strong>Sistema de Gestión Vehicular</strong>
</p>

<p align="center">
  API REST desarrollada con Spring Boot para la gestión de clientes, vehículos,
  ventas y mantenimientos de AutoDrive Motors.
</p>

---

## Sobre el proyecto

**AutoDrive Motors** es un sistema backend orientado a la gestión integral de
información relacionada con vehículos, clientes, ventas y mantenimientos.

El proyecto surge como respuesta a una problemática de gestión manual de
información mediante hojas de cálculo y documentos físicos, situación que
puede generar pérdida de datos, errores en precios, ventas duplicadas,
dificultades para consultar vehículos disponibles y retrasos en la atención
al cliente.

La solución implementa una **API RESTful**, una base de datos relacional y
el consumo de una API externa para consultar tasas de cambio y realizar la
conversión automática de valores de vehículos de **COP a USD**.

---

## Objetivo

Desarrollar una API RESTful que permita administrar de manera organizada y
segura los procesos relacionados con clientes, vehículos, ventas y
mantenimientos, incorporando persistencia de datos, validaciones, lógica de
negocio y consumo de servicios externos.

---

## Funcionalidades

### Clientes

- Registrar clientes.
- Consultar clientes.
- Actualizar información.
- Eliminar clientes.
- Validar que no existan correos electrónicos repetidos.

### Vehículos

- Registrar vehículos.
- Consultar vehículos.
- Actualizar vehículos.
- Eliminar vehículos.
- Consultar vehículos por marca.
- Consultar vehículos disponibles.
- Validar que la placa sea única.
- Controlar el estado del vehículo.

### Ventas

- Registrar ventas.
- Asociar clientes con vehículos.
- Calcular automáticamente el total de venta.
- Registrar automáticamente la fecha de venta.
- Cambiar el estado del vehículo a `Vendido`.
- Aplicar un descuento del 5% cuando el vehículo supere los
  $100.000.000 COP.

### Mantenimientos

- Registrar mantenimientos.
- Consultar historial de mantenimientos.
- Cambiar el estado del vehículo a `En mantenimiento`.

### Conversión de moneda

- Consultar una tasa de cambio mediante una API externa.
- Convertir automáticamente el valor de los vehículos de COP a USD.

---

## Tecnologías

| Tecnología | Uso |
|---|---|
| Java | Lenguaje principal |
| Spring Boot | Desarrollo de la API REST |
| Spring Data JPA | Persistencia de datos |
| Hibernate | ORM |
| MySQL / PostgreSQL | Base de datos relacional |
| Postman | Pruebas de endpoints |
| DTOs | Transferencia de datos |
| JSON | Intercambio de información |
| UML / DER | Análisis y diseño |

El proyecto sigue una arquitectura por capas e incorpora validaciones,
manejo de excepciones y separación de responsabilidades. :contentReference[oaicite:1]{index=1}

---

## Arquitectura

El proyecto está organizado mediante una arquitectura por capas para
mantener una separación clara de responsabilidades:

```text
src/
└── main/
    └── java/
        └── ...
            ├── controller/
            ├── service/
            ├── repository/
            ├── model/
            ├── dto/
            ├── exception/
            └── config/
