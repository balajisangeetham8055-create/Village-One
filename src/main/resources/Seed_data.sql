-- ═══════════════════════════════════════════════════════════
--  VillageOne — Seed Data
--  Run this ONCE in MySQL Workbench after first app startup
--  Database: villageone_app
-- ═══════════════════════════════════════════════════════════

USE villageone_app;

-- ── Plans ──────────────────────────────────────────────────
INSERT IGNORE INTO plans (id, label, price, price_label, days, per, badge, features, active) VALUES
('monthly',     'Monthly',   299.00,  '₹299',   30,  '/ month',   NULL,        'Unlimited Invoices,GST Bills,PDF Download,Stock Alerts,1 Worker Free', true),
('half_yearly', '6 Months',  1499.00, '₹1,499', 180, '/ 6 months','Save 16%',  'Unlimited Invoices,GST Bills,PDF Download,Stock Alerts,1 Worker Free,Priority Support', true),
('yearly',      '1 Year',    2499.00, '₹2,499', 365, '/ year',    'Best Value','Unlimited Invoices,GST Bills,PDF Download,Stock Alerts,1 Worker Free,Priority Support,Free Updates', true);

-- ── Contact Info ───────────────────────────────────────────
INSERT IGNORE INTO contact_info (whatsapp, whatsapp_label, phone, phone_label, email, hours_chat, hours_call, reply_time)
VALUES ('9945027973', '+91 9945027973', '9945027973', '+91 9945027973',
        'villageone.help@gmail.com', '24/7', 'Mon–Sun · 6 AM – 11 PM', 'under 30 minutes');

-- Verify
SELECT 'Plans:' AS info; SELECT id, label, price FROM plans;
SELECT 'Contact:' AS info; SELECT whatsapp, email FROM contact_info;
SELECT 'Seed done ✅' AS status;

-- ── App Config (Razorpay keys + worker price) ──────────────
-- Run after first startup. Replace with your actual Razorpay live keys.
USE villageone_app;

INSERT INTO app_config (key_name, key_value, description) VALUES
                                                              ('razorpay_key_id',     'rzp_live_STbdzKC8dmrGV4',  'Razorpay Live Key ID'),
                                                              ('razorpay_key_secret', 'BBNV52jQU8Q9ClNdCtAA6pdZ', 'Razorpay Live Key Secret'),
                                                              ('worker_price',        '30',                        'Price per extra worker slot per month');

-- Verify
SELECT key_name, description FROM app_config;

SELECT 'App config inserted ✅' AS status;
SELECT key_name, description FROM app_config;







-- Run in MySQL Workbench — adds Fast2SMS key to app_config
USE villageone_app;

INSERT INTO app_config (key_name, key_value, description)
VALUES ('fast2sms_api_key', 'RBlp7CTUkVEboZfjcFvahxQr9ISKe54HAwdJ2ysm8OtYuGNqM0CY6dyuTtZmHsweKM4pAOIUSoavqLc2', 'Fast2SMS API Key for OTP')
    ON DUPLICATE KEY UPDATE key_value = VALUES(key_value);

SELECT 'Fast2SMS key saved ✅' AS status;
SELECT key_name, description FROM app_config;



-- Run in MySQL Workbench
USE villageone_app;

-- Add MSG91 credentials to app_config
INSERT INTO app_config (key_name, key_value, description) VALUES
                                                              ('msg91_auth_token',  '502115TR8E94213469be6b6cP1', 'MSG91 Auth Token'),
                                                              ('msg91_template_id', '69be69972d789fe30d0907e4',   'MSG91 OTP Template ID'),
                                                              ('msg91_sender_id',   'VLGONE',                      'MSG91 Sender ID')
    AS new_val
ON DUPLICATE KEY UPDATE key_value = new_val.key_value;

-- Remove old fast2sms key if present
DELETE FROM app_config WHERE key_name = 'fast2sms_api_key';

SELECT 'MSG91 config saved ✅' AS status;
SELECT key_name, description FROM app_config;

-- Run in MySQL Workbench
USE villageone_app;

INSERT INTO app_config (key_name, key_value, description) VALUES
                                                              ('email_host',      'smtp.gmail.com',       'Gmail SMTP Host'),
                                                              ('email_port',      '587',                  'Gmail SMTP Port'),
                                                              ('email_username',  'villageone.help@gmail.com', 'Your Gmail address'),
                                                              ('email_password',  'vjblsrfmkxjsutsp',     'Gmail App Password (no spaces)'),
                                                              ('email_from_name', 'VillageOne',           'Sender name shown in email')
    ON DUPLICATE KEY UPDATE key_value = VALUES(key_value);

SELECT 'Email config saved ✅' AS status;
SELECT key_name, description FROM app_config;



-- Run in MySQL Workbench
USE villageone_app;

INSERT INTO app_config (key_name, key_value, description) VALUES
                                                              ('social_instagram', 'https://instagram.com/villageone.in', 'https://www.instagram.com/villageone.in?igsh=anI0dnF5Y3RhaWlp&utm_source=qr'),
                                                              ('social_twitter',   'https://x.com/villageone_in',         'https://x.com/villageone_in?s=11'),
                                                              ('social_whatsapp',  'https://wa.me/7731077973',           'https://wa.me/917731077973?text=Hello%20I%20want%20to%20know%20more')
    AS new_val
ON DUPLICATE KEY UPDATE key_value = new_val.key_value;

SELECT 'Social links saved ✅' AS status;


-- 1. Add profile photo to users table
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS profile_photo LONGTEXT NULL COMMENT 'Base64 encoded profile photo';

-- 2. Add BORROW to invoice payment_mode enum
ALTER TABLE invoices
    MODIFY COLUMN payment_mode ENUM('CASH','UPI','CARD','CREDIT','BORROW') NOT NULL;

SELECT 'Migration V4 complete ✅' AS status;

//====================================================04/04/2026

UPDATE app_config SET key_value = '1'    WHERE key_name = 'worker_free_slots';

UPDATE app_config SET key_value = '190'  WHERE key_name = 'worker_price_monthly';

UPDATE app_config SET key_value = '950'  WHERE key_name = 'worker_price_half_yearly';

UPDATE app_config SET key_value = '1700' WHERE key_name = 'worker_price_yearly';
//----------------------

DELETE FROM app_config WHERE key_name = 'worker_price';

//-------------------
INSERT INTO app_config (key_name, key_value, description) VALUES
                                                              ('admin_username', 'manikanta7973', 'Super admin login username'),
                                                              ('admin_password_hash', '77310Mani77973@K', 'Admin password hash'),
                                                              ('free_trial_days', '30', 'Free trial duration in days')
    ON DUPLICATE KEY UPDATE key_value = VALUES(key_value);

//---------------------------------------

UPDATE app_config SET key_value = 'Free'     WHERE key_name = 'worker_plan_label_free';

UPDATE app_config SET key_value = 'Monthly'  WHERE key_name = 'worker_plan_label_monthly';

UPDATE app_config SET key_value = '6 Months' WHERE key_name = 'worker_plan_label_half_yearly';

UPDATE app_config SET key_value = '1 Year'   WHERE key_name = 'worker_plan_label_yearly';


//---------

SELECT key_name, key_value FROM app_config ORDER BY key_name;