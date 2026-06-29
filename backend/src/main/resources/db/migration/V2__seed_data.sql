-- Pre-seeded Users (BCrypt hash for plain text 'password' is: $2a$10$r9x7H43gE6hT5XwK5d3t3uR5rD5A5d.9W3y.1C8u0D2f.v1B3K.2W)
-- Roles: ADMIN, PLANT_MANAGER, MAINTENANCE_ENGINEER, RELIABILITY_ENGINEER, HSE_OFFICER, VIEWER

INSERT INTO users (id, email, password_hash, role, plant_id, retirement_date, years_experience) VALUES
('a0000000-0000-0000-0000-000000000001', 'admin@tacitiq.com', '$2a$10$dtXXPsza8nQWjn5wVDnevezYQNmKtYjLIk2z1dU7nf1zcwtRxEtdm', 'ADMIN', 'b0000000-0000-0000-0000-000000000001', '2045-12-31', 10),
('a0000000-0000-0000-0000-000000000002', 'manager@tacitiq.com', '$2a$10$dtXXPsza8nQWjn5wVDnevezYQNmKtYjLIk2z1dU7nf1zcwtRxEtdm', 'PLANT_MANAGER', 'b0000000-0000-0000-0000-000000000001', '2038-06-15', 20),
('a0000000-0000-0000-0000-000000000003', 'engineer@tacitiq.com', '$2a$10$dtXXPsza8nQWjn5wVDnevezYQNmKtYjLIk2z1dU7nf1zcwtRxEtdm', 'MAINTENANCE_ENGINEER', 'b0000000-0000-0000-0000-000000000001', '2027-04-30', 35), -- Retiring soon!
('a0000000-0000-0000-0000-000000000004', 'reliability@tacitiq.com', '$2a$10$dtXXPsza8nQWjn5wVDnevezYQNmKtYjLIk2z1dU7nf1zcwtRxEtdm', 'RELIABILITY_ENGINEER', 'b0000000-0000-0000-0000-000000000001', '2042-09-01', 15),
('a0000000-0000-0000-0000-000000000005', 'hse@tacitiq.com', '$2a$10$dtXXPsza8nQWjn5wVDnevezYQNmKtYjLIk2z1dU7nf1zcwtRxEtdm', 'HSE_OFFICER', 'b0000000-0000-0000-0000-000000000001', '2040-01-01', 12);

INSERT INTO user_expertise_areas (user_id, expertise_area) VALUES
('a0000000-0000-0000-0000-000000000001', 'System Admin'),
('a0000000-0000-0000-0000-000000000002', 'Management'),
('a0000000-0000-0000-0000-000000000002', 'Operations'),
('a0000000-0000-0000-0000-000000000003', 'Rotating Equipment'),
('a0000000-0000-0000-0000-000000000003', 'Shaft Alignment'),
('a0000000-0000-0000-0000-000000000003', 'Vibration Analysis'),
('a0000000-0000-0000-0000-000000000004', 'RCA'),
('a0000000-0000-0000-0000-000000000004', 'FMEA'),
('a0000000-0000-0000-0000-000000000004', 'Predictive Maintenance'),
('a0000000-0000-0000-0000-000000000005', 'OSHA'),
('a0000000-0000-0000-0000-000000000005', 'Process Safety'),
('a0000000-0000-0000-0000-000000000005', 'Environmental Audit');

-- Pre-seeded Assets (Tag convention: P-101, K-201, E-205, V-301)
INSERT INTO assets (id, tag_number, asset_type, parent_asset_id, criticality, health_score, installation_date, plant_area, oem_model) VALUES
('c0000000-0000-0000-0000-000000000001', 'P-101', 'Centrifugal Pump', NULL, 'A', 0.85, '2015-04-12', 'Crude Distillation Unit (CDU)', 'Flowserve 10-HDX-18A'),
('c0000000-0000-0000-0000-000000000002', 'K-201', 'Centrifugal Compressor', NULL, 'A', 0.92, '2012-08-20', 'Hydrocracker Unit (HCU)', 'Siemens STC-SH'),
('c0000000-0000-0000-0000-000000000003', 'E-205', 'Shell & Tube Heat Exchanger', NULL, 'B', 0.78, '2018-05-15', 'CDU Pre-Heat Train', 'Alfa Laval Compabloc'),
('c0000000-0000-0000-0000-000000000004', 'V-301', 'High-Pressure Separator Vessel', NULL, 'A', 0.98, '2010-10-10', 'HCU Feed System', 'CBI Heavy Vessels');

-- Sub-assets (P-101 motor component)
INSERT INTO assets (id, tag_number, asset_type, parent_asset_id, criticality, health_score, installation_date, plant_area, oem_model) VALUES
('c0000000-0000-0000-0000-000000000005', 'M-101', 'Electric Drive Motor', 'c0000000-0000-0000-0000-000000000001', 'A', 0.88, '2015-04-12', 'Crude Distillation Unit (CDU)', 'Baldor Reliance 500HP');

-- Pre-seeded Compliance Rules (OSHA 1910.147, ISO 9001, API 570)
INSERT INTO compliance_rules (id, standard, clause, description, check_query, frequency, applicable_assets, severity, effective_date) VALUES
('d0000000-0000-0000-0000-000000000001', 'OSHA 1910.147', 'Lockout/Tagout (LOTO)', 'Require formal lockout/tagout procedure before initiating maintenance on mechanical assets with energy sources.', 'MATCH (p:Procedure)-[:SATISFIES]->(c:ComplianceRule {clause: "Lockout/Tagout (LOTO)"}) RETURN p', 'DAILY', '{"asset_types": ["Centrifugal Pump", "Centrifugal Compressor", "Electric Drive Motor"]}', 'CRITICAL', '1990-01-02'),
('d0000000-0000-0000-0000-000000000002', 'API 570', 'Piping Inspection Code', 'Periodic thickness measurements and inspection of process piping systems for corrosion.', 'MATCH (p:Procedure)-[:SATISFIES]->(c:ComplianceRule {standard: "API 570"}) RETURN p', 'ANNUAL', '{"asset_types": ["Shell & Tube Heat Exchanger", "High-Pressure Separator Vessel"]}', 'MAJOR', '2016-02-15');

-- Pre-seeded Incidents
INSERT INTO incidents (id, incident_type, severity, asset_id, root_cause, contributing_factors, lessons_learned, occurred_at, closed_at, reported_by) VALUES
('e0000000-0000-0000-0000-000000000001', 'Equipment Failure', 'P2', 'c0000000-0000-0000-0000-000000000001', 'Bearing failure due to lubrication starvation resulting in shaft seizure.', '{"factors": ["Lubrication line blockage", "Vibration sensor threshold alarm ignored"]}', 'Lubrication inspection frequency upgraded from monthly to weekly.', '2024-03-10 08:30:00+00', '2024-03-12 17:00:00+00', 'a0000000-0000-0000-0000-000000000003'),
('e0000000-0000-0000-0000-000000000002', 'HSE Violation', 'P1', 'c0000000-0000-0000-0000-000000000002', 'Lockout tagout (LOTO) procedure not followed during electrical inspection of compressor drive.', '{"factors": ["Missing tag identifier", "Operator error"]}', 'LOTO refresher training mandated for all maintenance technicians.', '2025-01-15 14:00:00+00', '2025-01-16 11:30:00+00', 'a0000000-0000-0000-0000-000000000005');

-- Pre-seeded Maintenance Records
INSERT INTO maintenance_records (id, asset_id, work_order_no, maint_type, technician_id, findings, parts_replaced, next_due_date, labor_hours, total_cost, completed_at) VALUES
('f0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'WO-849204', 'Corrective Maintenance', 'a0000000-0000-0000-0000-000000000003', 'Replaced mechanical shaft seal and inboard sleeve bearings. Flushed grease reservoirs.', '{"parts": [{"name": "Mechanical Seal", "id": "MS-994", "cost": 450.00}, {"name": "SKF Bearing", "id": "SKF-6312", "cost": 180.00}]}', '2026-09-25', 6.5, 950.00, '2026-06-25 12:00:00+00'),
('f0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000003', 'WO-991823', 'Preventive Maintenance', 'a0000000-0000-0000-0000-000000000003', 'Conducted shell-side backflush and tube bundle inspection. No major scaling observed.', '{"parts": []}', '2026-12-25', 4.0, 320.00, '2026-06-20 16:00:00+00');
