-- 创建数据库
-- CREATE DATABASE rocket_simulation;

-- 切换到数据库后执行以下表创建（JPA会自动创建，此脚本作为参考）

CREATE TABLE IF NOT EXISTS simulation_recipes (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    lox_ratio DOUBLE PRECISION NOT NULL,
    kerosene_ratio DOUBLE PRECISION NOT NULL,
    chamber_pressure DOUBLE PRECISION NOT NULL,
    initial_temperature DOUBLE PRECISION NOT NULL,
    burn_duration DOUBLE PRECISION NOT NULL,
    nozzle_area_ratio DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_simulation_recipes_name ON simulation_recipes(name);
CREATE INDEX IF NOT EXISTS idx_simulation_recipes_created_at ON simulation_recipes(created_at DESC);

-- 插入示例数据
INSERT INTO simulation_recipes (name, description, lox_ratio, kerosene_ratio, chamber_pressure, initial_temperature, burn_duration, nozzle_area_ratio)
VALUES 
('标准液氧煤油配方', '用于中型运载火箭的标准推进剂配方', 2.56, 1.0, 10.0, 300.0, 120.0, 16.0),
('高空优化配方', '优化高空性能的富燃配方', 2.8, 1.0, 12.0, 280.0, 180.0, 25.0),
('大推力配方', '适用于助推器的大推力配方', 2.4, 1.0, 15.0, 300.0, 90.0, 12.0);
