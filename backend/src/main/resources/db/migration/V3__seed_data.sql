-- V3__seed_data.sql: Initial seed data for settings, categories, brands, and administrator

INSERT INTO app_settings (id, setting_key, setting_value, description, updated_at)
VALUES 
    ('00000000-0000-0000-0000-000000000001', 'business_name', 'Alexa Beauty', 'Nombre comercial de la tienda', CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000000002', 'currency', 'COP', 'Moneda principal para precios', CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000000003', 'timezone', 'America/Bogota', 'Zona horaria operativa del negocio', CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000000004', 'reservation_minutes', '45', 'Ventana en minutos para expiración de reservas temporales', CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000000005', 'whatsapp_number', '573001234567', 'Número oficial de WhatsApp para pedidos', CURRENT_TIMESTAMP);

INSERT INTO categories (id, name, slug, sort_order, active, created_at, updated_at)
VALUES
    ('10000000-0000-0000-0000-000000000001', 'Rostro', 'rostro', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000002', 'Labios', 'labios', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000003', 'Ojos', 'ojos', 3, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000004', 'Accesorios', 'accesorios', 4, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO brands (id, name, slug, active, created_at, updated_at)
VALUES
    ('20000000-0000-0000-0000-000000000001', 'BELLA COSMETICS', 'bella-cosmetics', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('20000000-0000-0000-0000-000000000002', 'ROSA GLOW', 'rosa-glow', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('20000000-0000-0000-0000-000000000003', 'LUNA BEAUTY', 'luna-beauty', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO users (id, email, password_hash, role, full_name, active, last_login_at, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-000000000099', 'admin@alexabeauty.com', '$2a$12$7kS8rWw85K.lU9u06Jc83u/1i45eX02Y4t9Cg43X3rGZ7P75HqfWW', 'ADMIN', 'Administrador Alexa Beauty', TRUE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
