-- Create pgvector extension if it doesn't exist
CREATE EXTENSION IF NOT EXISTS vector;

-- Table: users
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    plant_id UUID,
    retirement_date DATE,
    years_experience INT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_active TIMESTAMPTZ
);

-- Table: user_expertise_areas
CREATE TABLE user_expertise_areas (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expertise_area VARCHAR(255) NOT NULL,
    PRIMARY KEY (user_id, expertise_area)
);

-- Table: assets
CREATE TABLE assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tag_number VARCHAR(50) UNIQUE NOT NULL,
    asset_type VARCHAR(100) NOT NULL,
    parent_asset_id UUID REFERENCES assets(id),
    criticality VARCHAR(10) NOT NULL,
    health_score DOUBLE PRECISION NOT NULL CHECK (health_score BETWEEN 0.0 AND 1.0),
    installation_date DATE NOT NULL,
    digital_twin_id UUID,
    plant_area VARCHAR(100) NOT NULL,
    oem_model VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Table: documents
CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doc_type VARCHAR(50) NOT NULL,
    title TEXT NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    version INT NOT NULL DEFAULT 1,
    embedding_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    chunk_count INT NOT NULL DEFAULT 0,
    uploaded_by UUID REFERENCES users(id),
    processed_at TIMESTAMPTZ
);

-- Table: document_related_assets
CREATE TABLE document_related_assets (
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    asset_id UUID NOT NULL,
    PRIMARY KEY (document_id, asset_id)
);

-- Table: document_chunks
CREATE TABLE document_chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    embedding vector(768) NOT NULL
);

-- Table: incidents
CREATE TABLE incidents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_type VARCHAR(50) NOT NULL,
    severity VARCHAR(10) NOT NULL,
    asset_id UUID REFERENCES assets(id),
    root_cause TEXT,
    contributing_factors JSONB,
    lessons_learned TEXT,
    occurred_at TIMESTAMPTZ NOT NULL,
    closed_at TIMESTAMPTZ,
    reported_by UUID REFERENCES users(id)
);

-- Table: maintenance_records
CREATE TABLE maintenance_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id UUID REFERENCES assets(id),
    work_order_no VARCHAR(100) UNIQUE NOT NULL,
    maint_type VARCHAR(50) NOT NULL,
    technician_id UUID REFERENCES users(id),
    findings TEXT,
    parts_replaced JSONB,
    next_due_date DATE,
    labor_hours DOUBLE PRECISION,
    total_cost DECIMAL(10,2),
    completed_at TIMESTAMPTZ
);

-- Table: expert_knowledge
CREATE TABLE expert_knowledge (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    engineer_id UUID REFERENCES users(id),
    knowledge_type VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    confidence_score DOUBLE PRECISION NOT NULL CHECK (confidence_score BETWEEN 0.0 AND 1.0),
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Table: expert_knowledge_asset_tags
CREATE TABLE expert_knowledge_asset_tags (
    knowledge_id UUID NOT NULL REFERENCES expert_knowledge(id) ON DELETE CASCADE,
    asset_tag VARCHAR(255) NOT NULL,
    PRIMARY KEY (knowledge_id, asset_tag)
);

-- Table: expert_knowledge_validators
CREATE TABLE expert_knowledge_validators (
    knowledge_id UUID NOT NULL REFERENCES expert_knowledge(id) ON DELETE CASCADE,
    validator_id UUID NOT NULL,
    PRIMARY KEY (knowledge_id, validator_id)
);

-- Table: risk_scores
CREATE TABLE risk_scores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    risk_type VARCHAR(50) NOT NULL,
    score DOUBLE PRECISION NOT NULL CHECK (score BETWEEN 0.0 AND 1.0),
    contributing_factors JSONB,
    valid_until TIMESTAMPTZ NOT NULL,
    computed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    model_version VARCHAR(50) NOT NULL
);

-- Table: compliance_rules
CREATE TABLE compliance_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    standard VARCHAR(50) NOT NULL,
    clause VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    check_query TEXT NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    applicable_assets JSONB,
    severity VARCHAR(20) NOT NULL,
    effective_date DATE NOT NULL
);

-- Table: audit_logs
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    actor_id UUID,
    action_type VARCHAR(100) NOT NULL,
    resource_id VARCHAR(100),
    ip_address VARCHAR(45),
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indices
CREATE INDEX idx_assets_tag ON assets(tag_number);
CREATE INDEX idx_documents_status ON documents(embedding_status);
CREATE INDEX idx_incidents_asset ON incidents(asset_id);
CREATE INDEX idx_maint_asset ON maintenance_records(asset_id);
CREATE INDEX idx_risk_entity ON risk_scores(entity_type, entity_id);
CREATE INDEX idx_chunks_doc ON document_chunks(document_id);

-- Vector index for cosine distance similarity (HNSW)
CREATE INDEX idx_chunks_embedding ON document_chunks USING hnsw (embedding vector_cosine_ops);
