-- Reservation and review schema for event module.

CREATE TABLE IF NOT EXISTS evt_reservation (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    event_id INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reserved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    qr_code_checked TINYINT(1) NOT NULL DEFAULT 0,
    CONSTRAINT uq_evt_reservation_user_event UNIQUE (user_id, event_id),
    CONSTRAINT fk_evt_reservation_user FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE,
    CONSTRAINT fk_evt_reservation_event FOREIGN KEY (event_id) REFERENCES evt_event (id) ON DELETE CASCADE,
    CONSTRAINT chk_evt_reservation_status CHECK (status IN ('PENDING', 'ACCEPTED', 'DENIED'))
);

CREATE TABLE IF NOT EXISTS evt_review (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    event_id INT NOT NULL,
    rating INT NOT NULL,
    comment TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_evt_review_user_event UNIQUE (user_id, event_id),
    CONSTRAINT fk_evt_review_user FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE,
    CONSTRAINT fk_evt_review_event FOREIGN KEY (event_id) REFERENCES evt_event (id) ON DELETE CASCADE,
    CONSTRAINT chk_evt_review_rating CHECK (rating >= 1 AND rating <= 5)
);

CREATE INDEX idx_evt_reservation_status ON evt_reservation (status);
CREATE INDEX idx_evt_reservation_reserved_at ON evt_reservation (reserved_at);
CREATE INDEX idx_evt_review_event ON evt_review (event_id);

CREATE TABLE IF NOT EXISTS evt_reclamation (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    event_id INT NOT NULL,
    subject VARCHAR(255) NOT NULL,
    message LONGTEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    ai_response LONGTEXT NULL,
    admin_response LONGTEXT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_evt_reclamation_user FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE,
    CONSTRAINT fk_evt_reclamation_event FOREIGN KEY (event_id) REFERENCES evt_event (id) ON DELETE CASCADE,
    CONSTRAINT chk_evt_reclamation_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'ESCALATED', 'RESOLVED', 'REJECTED'))
);

CREATE INDEX idx_evt_reclamation_user ON evt_reclamation (user_id);
CREATE INDEX idx_evt_reclamation_event ON evt_reclamation (event_id);
CREATE INDEX idx_evt_reclamation_status ON evt_reclamation (status);
CREATE INDEX idx_evt_reclamation_created_at ON evt_reclamation (created_at);
