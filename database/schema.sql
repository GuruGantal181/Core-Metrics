CREATE DATABASE IF NOT EXISTS core_metrics;
USE core_metrics;

-- WARNING: This script recreates the schema and DROPS existing project tables/data. Back up data before running it again.

SET FOREIGN_KEY_CHECKS = 0;
DROP VIEW IF EXISTS user_daily_nutrition_view;
DROP FUNCTION IF EXISTS CalculateUserBMR;
DROP PROCEDURE IF EXISTS GetUserWeeklySummary;
DROP PROCEDURE IF EXISTS CalculateTotalStrengthVolume;
DROP TRIGGER IF EXISTS trg_validate_strength_log;
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
    weight_kg DECIMAL(5,2) NOT NULL CHECK (weight_kg > 0),
    height_cm DECIMAL(5,2) NOT NULL CHECK (height_cm > 0),
    age INT NOT NULL CHECK (age > 0 AND age <= 120),
    gender ENUM('M', 'F') NOT NULL,
    PRIMARY KEY (user_id)
);

-- 2. EXERCISE_DICT (Centralized dictionary for 3NF)
CREATE TABLE exercise_dict (
    exercise_id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    type ENUM('strength', 'cardio') NOT NULL,
    met_value DECIMAL(4,2) NOT NULL CHECK (met_value > 0),
    created_by_uid INT NULL,
    PRIMARY KEY (exercise_id),
    UNIQUE (name, created_by_uid),
    FOREIGN KEY (created_by_uid) REFERENCES user_profiles(user_id) ON DELETE SET NULL
);

-- 3. ROUTINES
CREATE TABLE routines (
    routine_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    routine_name VARCHAR(100) NOT NULL,
    PRIMARY KEY (routine_id),
    UNIQUE (user_id, routine_name),
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
    weight_kg DECIMAL(5,2) NOT NULL CHECK (weight_kg >= 0),
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
    duration_m INT NOT NULL CHECK (duration_m > 0),
    calories_out DECIMAL(6,2) NOT NULL CHECK (calories_out >= 0),
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
    calories INT NOT NULL CHECK (calories >= 0),
    protein_g DECIMAL(5,2) NOT NULL CHECK (protein_g >= 0),
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

-- ==========================================
-- 8. VIEW: User Daily Calorie & Protein Summary
-- ==========================================
CREATE OR REPLACE VIEW user_daily_nutrition_view AS
SELECT 
    u.user_id,
    u.gender,
    u.weight_kg,
    DATE(d.consumed_at) AS log_date,
    SUM(d.calories) AS total_calories_in,
    SUM(d.protein_g) AS total_protein_g
FROM user_profiles u
INNER JOIN diet_logs d ON u.user_id = d.user_id
GROUP BY u.user_id, u.gender, u.weight_kg, DATE(d.consumed_at);

-- ==========================================
-- 9. STORED PROCEDURE: User Weekly Summary
-- ==========================================
DELIMITER //
CREATE PROCEDURE GetUserWeeklySummary(IN p_user_id INT)
BEGIN
    SELECT 
        (SELECT COALESCE(SUM(calories), 0) FROM diet_logs 
         WHERE user_id = p_user_id AND consumed_at >= NOW() - INTERVAL 7 DAY) AS weekly_calories_in,
        (SELECT COALESCE(SUM(calories_out), 0) FROM cardio_logs 
         WHERE user_id = p_user_id AND logged_at >= NOW() - INTERVAL 7 DAY) AS weekly_cardio_out,
        (SELECT COUNT(*) FROM strength_logs 
         WHERE user_id = p_user_id AND logged_at >= NOW() - INTERVAL 7 DAY) AS weekly_strength_sets;
END //
DELIMITER ;

-- ==========================================
-- 10. STORED FUNCTION: Calculate User BMR
-- ==========================================
DELIMITER //
CREATE FUNCTION CalculateUserBMR(p_user_id INT)
RETURNS DECIMAL(7,2)
NOT DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_weight DECIMAL(5,2);
    DECLARE v_height DECIMAL(5,2);
    DECLARE v_age INT;
    DECLARE v_gender CHAR(1);
    DECLARE v_bmr DECIMAL(7,2);
    DECLARE v_user_found BOOLEAN DEFAULT TRUE;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_user_found = FALSE;

    SELECT weight_kg, height_cm, age, gender
    INTO v_weight, v_height, v_age, v_gender
    FROM user_profiles
    WHERE user_id = p_user_id;

    IF NOT v_user_found THEN
        RETURN NULL;
    END IF;

    IF v_gender = 'M' THEN
        SET v_bmr = (10 * v_weight) + (6.25 * v_height) - (5 * v_age) + 5;
    ELSE
        SET v_bmr = (10 * v_weight) + (6.25 * v_height) - (5 * v_age) - 161;
    END IF;

    RETURN v_bmr;
END //
DELIMITER ;

-- ==========================================
-- 11. TRIGGER: Validate Strength Log Before Insert
-- ==========================================
DELIMITER //
CREATE TRIGGER trg_validate_strength_log
BEFORE INSERT ON strength_logs
FOR EACH ROW
BEGIN
    IF NEW.weight_kg < 0 OR NEW.reps <= 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Invalid strength log: weight_kg must be >= 0 and reps must be > 0';
    END IF;
END //
DELIMITER ;

-- ==========================================
-- 12. CURSOR PROCEDURE: Total Strength Volume for a User
-- ==========================================
DELIMITER //
CREATE PROCEDURE CalculateTotalStrengthVolume(IN p_user_id INT, OUT p_total_volume DECIMAL(10,2))
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_weight DECIMAL(5,2);
    DECLARE v_reps INT;
    DECLARE v_running_total DECIMAL(10,2) DEFAULT 0.00;

    DECLARE strength_cur CURSOR FOR
        SELECT weight_kg, reps
        FROM strength_logs
        WHERE user_id = p_user_id;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN strength_cur;

    read_loop: LOOP
        FETCH strength_cur INTO v_weight, v_reps;
        IF done THEN
            LEAVE read_loop;
        END IF;
        SET v_running_total = v_running_total + (v_weight * v_reps);
    END LOOP;

    CLOSE strength_cur;
    SET p_total_volume = v_running_total;
END //
DELIMITER ;