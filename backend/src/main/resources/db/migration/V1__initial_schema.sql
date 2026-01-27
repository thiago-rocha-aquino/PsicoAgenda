-- Initial schema for PsicoAgenda

-- Admin Users table
CREATE TABLE admin_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_login TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Session Types table
CREATE TABLE session_type (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    duration_minutes INTEGER NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Patients table (with encrypted fields)
CREATE TABLE patient (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(500) NOT NULL,  -- Encrypted
    email VARCHAR(500),           -- Encrypted
    active BOOLEAN NOT NULL DEFAULT TRUE,
    anonymized BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Availability (weekly schedule)
CREATE TABLE availability (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    day_of_week VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Blocks (vacations, holidays, breaks)
CREATE TABLE block (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    start_datetime TIMESTAMP NOT NULL,
    end_datetime TIMESTAMP NOT NULL,
    block_type VARCHAR(20) NOT NULL,
    reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Recurring Series
CREATE TABLE recurring_series (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES patient(id) ON DELETE CASCADE,
    session_type_id UUID NOT NULL REFERENCES session_type(id),
    day_of_week VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Appointments
CREATE TABLE appointment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES patient(id) ON DELETE CASCADE,
    session_type_id UUID NOT NULL REFERENCES session_type(id),
    recurring_series_id UUID REFERENCES recurring_series(id) ON DELETE SET NULL,
    start_datetime TIMESTAMP NOT NULL,
    end_datetime TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    session_link VARCHAR(500),
    cancellation_token VARCHAR(255) UNIQUE,
    cancelled_at TIMESTAMP,
    cancelled_by VARCHAR(255),
    cancellation_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Payments
CREATE TABLE payment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID NOT NULL UNIQUE REFERENCES appointment(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    amount DECIMAL(10, 2) NOT NULL,
    paid_at TIMESTAMP,
    receipt_number VARCHAR(100),
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Consent Versions (the actual terms)
CREATE TABLE consent_version (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version VARCHAR(50) NOT NULL UNIQUE,
    content TEXT NOT NULL,
    effective_from TIMESTAMP NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Consent Records (patient acceptance)
CREATE TABLE consent (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES patient(id) ON DELETE CASCADE,
    version VARCHAR(50) NOT NULL,
    accepted_at TIMESTAMP NOT NULL,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Notification Logs
CREATE TABLE notification_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID NOT NULL REFERENCES appointment(id) ON DELETE CASCADE,
    notification_type VARCHAR(20) NOT NULL,
    trigger_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    sent_at TIMESTAMP,
    error_message VARCHAR(1000),
    recipient VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Audit Logs
CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    old_value JSONB,
    new_value JSONB,
    performed_by VARCHAR(255),
    performed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(50)
);

-- Indexes
CREATE INDEX idx_appointment_start ON appointment(start_datetime);
CREATE INDEX idx_appointment_patient ON appointment(patient_id);
CREATE INDEX idx_appointment_status ON appointment(status);
CREATE INDEX idx_appointment_cancellation_token ON appointment(cancellation_token);
CREATE INDEX idx_notification_appointment ON notification_log(appointment_id);
CREATE INDEX idx_notification_trigger ON notification_log(trigger_type);
CREATE INDEX idx_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_performed_at ON audit_log(performed_at);
CREATE INDEX idx_recurring_patient ON recurring_series(patient_id);
CREATE INDEX idx_recurring_active ON recurring_series(active);
CREATE INDEX idx_block_datetime ON block(start_datetime, end_datetime);

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers for updated_at
CREATE TRIGGER update_admin_user_updated_at BEFORE UPDATE ON admin_user FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_session_type_updated_at BEFORE UPDATE ON session_type FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_patient_updated_at BEFORE UPDATE ON patient FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_availability_updated_at BEFORE UPDATE ON availability FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_block_updated_at BEFORE UPDATE ON block FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_recurring_series_updated_at BEFORE UPDATE ON recurring_series FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_appointment_updated_at BEFORE UPDATE ON appointment FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_payment_updated_at BEFORE UPDATE ON payment FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_consent_version_updated_at BEFORE UPDATE ON consent_version FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_consent_updated_at BEFORE UPDATE ON consent FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_notification_log_updated_at BEFORE UPDATE ON notification_log FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
