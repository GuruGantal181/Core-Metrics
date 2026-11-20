CREATE DATABASE IF NOT EXISTS core_metrics;
USE core_metrics;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS routine_map;
DROP TABLE IF EXISTS routines;
DROP TABLE IF EXISTS strength_logs;
DROP TABLE IF EXISTS cardio_logs;
DROP TABLE IF EXISTS diet_logs;
DROP TABLE IF EXISTS exercise_dict;
DROP TABLE IF EXISTS user_profiles;
SET FOREIGN_KEY_CHECKS = 1;

-- 1. USER_PROFILES
CREATE TABLE user_profiles (
    user_id INT NOT NULL AUTO_INCREMENT,
    weight_kg DECIMAL(5,2) NOT NULL,
    height_cm DECIMAL(5,2) NOT NULL,
    age INT NOT NULL,
    gender ENUM('M', 'F') NOT NULL,
    PRIMARY KEY (user_id)
);

-- 2. EXERCISE_DICT (Centralized dictionary for 3NF)
CREATE TABLE exercise_dict (
    exercise_id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    type ENUM('strength', 'cardio') NOT NULL,
    met_value DECIMAL(4,2) NOT NULL,
    created_by_uid INT NULL,
    PRIMARY KEY (exercise_id),
    FOREIGN KEY (created_by_uid) REFERENCES user_profiles(user_id) ON DELETE SET NULL
);

-- 3. ROUTINES
CREATE TABLE routines (
    routine_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    routine_name VARCHAR(100) NOT NULL,
    PRIMARY KEY (routine_id),
    FOREIGN KEY (user_id) REFERENCES user_profiles(user_id) ON DELETE CASCADE
);

-- 4. ROUTINE_MAP (Resolves M:N relationship with composite key)
CREATE TABLE routine_map (
    routine_id INT NOT NULL,
    exercise_id INT NOT NULL,
    default_sets INT DEFAULT 3,
    default_reps INT DEFAULT 10,
    default_incl DECIMAL(4,2) DEFAULT 0.00,
    default_dur INT DEFAULT 0,
    PRIMARY KEY (routine_id, exercise_id),
    FOREIGN KEY (routine_id) REFERENCES routines(routine_id) ON DELETE CASCADE,
    FOREIGN KEY (exercise_id) REFERENCES exercise_dict(exercise_id) ON DELETE CASCADE
);

-- 5. STRENGTH_LOGS
CREATE TABLE strength_logs (
    log_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    exercise_id INT NOT NULL,
    weight_kg DECIMAL(5,2) NOT NULL,
    reps INT NOT NULL,
    logged_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id),
    INDEX idx_strength_date (logged_at),
    FOREIGN KEY (user_id) REFERENCES user_profiles(user_id) ON DELETE CASCADE,
    FOREIGN KEY (exercise_id) REFERENCES exercise_dict(exercise_id) ON DELETE CASCADE
);

-- 6. CARDIO_LOGS
CREATE TABLE cardio_logs (
    log_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    exercise_id INT NOT NULL,
    incline_pct DECIMAL(4,2) DEFAULT 0.00,
    duration_m INT NOT NULL,
    calories_out DECIMAL(6,2) NOT NULL,
    logged_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id),
    INDEX idx_cardio_date (logged_at),
    FOREIGN KEY (user_id) REFERENCES user_profiles(user_id) ON DELETE CASCADE,
    FOREIGN KEY (exercise_id) REFERENCES exercise_dict(exercise_id) ON DELETE CASCADE
);

-- 7. DIET_LOGS
CREATE TABLE diet_logs (
    log_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    food_item VARCHAR(100) NOT NULL,
    calories INT NOT NULL,
    protein_g DECIMAL(5,2) NOT NULL,
    consumed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id),
    INDEX idx_diet_date (consumed_at),
    FOREIGN KEY (user_id) REFERENCES user_profiles(user_id) ON DELETE CASCADE
);

-- Seed Starter Exercises (created_by_uid is NULL so they don't depend on dummy users)
INSERT INTO exercise_dict (name, type, met_value, created_by_uid) VALUES
('Bench Press', 'strength', 6.00, NULL),
('Pullups', 'strength', 8.00, NULL),
('Treadmill Run', 'cardio', 9.80, NULL),
('Cycling', 'cardio', 7.50, NULL);