-- V8__update_whatsapp_number.sql: Set production WhatsApp business number in app_settings
UPDATE app_settings
SET setting_value = '573235814497',
    updated_at = CURRENT_TIMESTAMP
WHERE setting_key = 'whatsapp_number';

INSERT INTO app_settings (id, setting_key, setting_value, description, updated_at)
SELECT CAST('00000000-0000-0000-0000-000000000005' AS UUID),
       'whatsapp_number',
       '573235814497',
       'Número oficial de WhatsApp para pedidos',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM app_settings WHERE setting_key = 'whatsapp_number'
);
