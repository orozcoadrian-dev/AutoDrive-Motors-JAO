INSERT INTO clientes (nombre, apellido, documento, email, telefono, fecha_registro)
VALUES
    ('Carlos', 'Mendoza', '1000000002', 'carlos.mendoza@example.test', '3001234567', CURRENT_TIMESTAMP),
    ('Ana', 'Gómez', '1000000003', 'ana.gomez@example.test', '3159876543', CURRENT_TIMESTAMP),
    ('Luis', 'Rodríguez', '1000000004', 'luis.rodriguez@example.test', '3104567890', CURRENT_TIMESTAMP);

INSERT INTO vehiculos (placa, marca, modelo, anio, precio_cop, estado, version)
VALUES
    ('ABC123', 'Toyota', 'Corolla', 2022, 85000000.00, 'DISPONIBLE', 0),
    ('XYZ789', 'Mazda', 'CX-30', 2023, 115000000.00, 'VENDIDO', 0),
    ('MNO456', 'Chevrolet', 'Onix', 2021, 55000000.00, 'DISPONIBLE', 0),
    ('KLS321', 'BMW', 'Serie 3', 2024, 180000000.00, 'DISPONIBLE', 0),
    ('RST654', 'Renault', 'Duster', 2020, 62000000.00, 'EN_MANTENIMIENTO', 0);

INSERT INTO ventas (cliente_id, vehiculo_id, fecha_venta, descuento_aplicado, monto_total)
VALUES (1, 2, CURRENT_TIMESTAMP, 5750000.00, 109250000.00);

INSERT INTO mantenimientos (vehiculo_id, fecha_mantenimiento, descripcion, costo)
VALUES (5, CURRENT_DATE - INTERVAL '2 days', 'Cambio de aceite, filtros y revisión general de frenos', 450000.00);
