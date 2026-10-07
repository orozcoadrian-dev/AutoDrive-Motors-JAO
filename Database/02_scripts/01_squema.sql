CREATE TABLE clientes (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    telefono VARCHAR(20),
    fecha_registro TIMESTAMP
    WITH
        TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
        CONSTRAINT uk_clientes_email UNIQUE (email)
);

CREATE TABLE vehiculos (
    id BIGSERIAL PRIMARY KEY,
    placa VARCHAR(10) NOT NULL,
    marca VARCHAR(50) NOT NULL,
    modelo VARCHAR(50) NOT NULL,
    anio INT NOT NULL,
    precio_cop DECIMAL(15, 2) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'Disponible',
    CONSTRAINT uk_vehiculos_placa UNIQUE (placa),
    CONSTRAINT chk_vehiculos_precio CHECK (precio_cop >= 0),
    CONSTRAINT chk_vehiculos_estado CHECK (
        estado IN (
            'Disponible',
            'Vendido',
            'En mantenimiento'
        )
    )
);

CREATE TABLE ventas (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    vehiculo_id BIGINT NOT NULL,
    fecha_venta TIMESTAMP
    WITH
        TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
        descuento_aplicado DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
        monto_total DECIMAL(15, 2) NOT NULL,
        CONSTRAINT uk_ventas_vehiculo UNIQUE (vehiculo_id),
        CONSTRAINT fk_ventas_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id) ON DELETE RESTRICT ON UPDATE CASCADE,
        CONSTRAINT fk_ventas_vehiculo FOREIGN KEY (vehiculo_id) REFERENCES vehiculos (id) ON DELETE RESTRICT ON UPDATE CASCADE,
        CONSTRAINT chk_ventas_monto CHECK (monto_total >= 0)
);

CREATE TABLE mantenimientos (
    id BIGSERIAL PRIMARY KEY,
    vehiculo_id BIGINT NOT NULL,
    fecha_mantenimiento TIMESTAMP
    WITH
        TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
        descripcion TEXT NOT NULL,
        costo DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
        CONSTRAINT fk_mantenimientos_vehiculo FOREIGN KEY (vehiculo_id) REFERENCES vehiculos (id) ON DELETE CASCADE ON UPDATE CASCADE,
        CONSTRAINT chk_mantenimientos_costo CHECK (costo >= 0)
);

CREATE INDEX idx_vehiculos_estado ON vehiculos (estado);

CREATE INDEX idx_vehiculos_marca ON vehiculos (marca);

CREATE INDEX idx_ventas_cliente ON ventas (cliente_id);

CREATE INDEX idx_mantenimientos_vehiculo ON mantenimientos (vehiculo_id);