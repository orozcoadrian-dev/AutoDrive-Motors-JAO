ALTER TABLE mantenimientos
    ALTER COLUMN fecha_mantenimiento TYPE DATE
    USING (fecha_mantenimiento AT TIME ZONE 'UTC')::date;
