
CREATE TABLE user_profiles (
                               id UUID PRIMARY KEY,
                               email VARCHAR(255) NOT NULL UNIQUE,
                               status VARCHAR(50) NOT NULL,
                               personal_info JSONB NOT NULL DEFAULT '{}'::jsonb,
                               preferences JSONB NOT NULL DEFAULT '{}'::jsonb,
                               avatar JSONB NOT NULL DEFAULT '{}'::jsonb,
                               admin_metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
                               created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                               updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_user_profiles_email ON user_profiles(email);

CREATE TABLE profile_audit_logs (
                                    id UUID PRIMARY KEY,
                                    target_user_id UUID NOT NULL,
                                    actor_id UUID,
                                    actor_role VARCHAR(50) NOT NULL,
                                    action VARCHAR(100) NOT NULL,
                                    reason TEXT,
                                    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_audit_target_user ON profile_audit_logs(target_user_id);

ALTER TABLE user_profiles DROP CONSTRAINT IF EXISTS uk_user_profiles_email;

CREATE UNIQUE INDEX idx_user_profiles_active_email
    ON user_profiles (email)
    WHERE status != 'DELETED';