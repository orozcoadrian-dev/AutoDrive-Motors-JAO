INSERT INTO
    clientes (
        nombre,
        apellido,
        email,
        telefono,
        fecha_registro
    )
VALUES (
        'Carlos',
        'Mendoza',
        'carlos.mendoza@email.com',
        '3001234567',
        NOW ()
    ),
    (
        'Ana',
        'Gómez',
        'ana.gomez@email.com',
        '3159876543',
        NOW ()
    ),
    (
        'Luis',
        'Rodríguez',
        'luis.rodriguez@email.com',
        '3104567890',
        NOW ()
    ),
    (
        'María',
        'Fernández',
        'maria.fernandez@email.com',
        '3206549871',
        NOW ()
    );

INSERT INTO
    vehiculos (
        placa,
        marca,
        modelo,
        anio,
        precio_cop,
        estado
    )
VALUES (
        'ABC123',
        'Toyota',
        'Corolla',
        2022,
        85000000.00,
        'Disponible'
    ),
    (
        'XYZ789',
        'Mazda',
        'CX-30',
        2023,
        115000000.00,
        'Disponible'
    ),
    (
        'MNO456',
        'Chevrolet',
        'Onix',
        2021,
        55000000.00,
        'Disponible'
    ),
    (
        'KLS321',
        'BMW',
        'Serie 3',
        2024,
        180000000.00,
        'Disponible'
    ),
    (
        'RST654',
        'Renault',
        'Duster',
        2020,
        62000000.00,
        'En mantenimiento'
    );

INSERT INTO
    mantenimientos (
        vehiculo_id,
        fecha_mantenimiento,
        descripcion,
        costo
    )
VALUES (
        5,
        NOW () - INTERVAL '2 days',
        'Cambio de aceite, filtros y revisión general de frenos',
        450000.00
    );

INSERT INTO
    ventas (
        cliente_id,
        vehiculo_id,
        fecha_venta,
        descuento_aplicado,
        monto_total
    )
VALUES (
        1,
        2,
        NOW (),
        5750000.00,
        109250000.00
    );

UPDATE vehiculos SET estado = 'Vendido' WHERE id = 2;

SELECT setval (
        'clientes_id_seq', (
            SELECT MAX(id)
            FROM clientes
        )
    );

SELECT setval (
        'vehiculos_id_seq', (
            SELECT MAX(id)
            FROM vehiculos
        )
    );

SELECT setval ( 'ventas_id_seq', ( SELECT MAX(id) FROM ventas ) );

SELECT setval (
        'mantenimientos_id_seq', (
            SELECT MAX(id)
            FROM mantenimientos
        )
    );