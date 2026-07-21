-- Migration: Assign professional role-based display names to seeded local users
UPDATE users SET display_name = 'Platform Administrator' WHERE email = 'admin@tacitiq.com';
UPDATE users SET display_name = 'Operations Manager' WHERE email = 'manager@tacitiq.com';
UPDATE users SET display_name = 'Maintenance Engineer' WHERE email = 'engineer@tacitiq.com';
UPDATE users SET display_name = 'Reliability Analyst' WHERE email = 'reliability@tacitiq.com';
UPDATE users SET display_name = 'HSE Coordinator' WHERE email = 'hse@tacitiq.com';
UPDATE users SET display_name = 'Maintenance Engineer' WHERE email = 'mock.engineer@tacitiq.com';
