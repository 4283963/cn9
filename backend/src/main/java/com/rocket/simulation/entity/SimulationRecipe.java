package com.rocket.simulation.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "simulation_recipes")
public class SimulationRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "lox_ratio", nullable = false)
    private Double loxRatio;

    @Column(name = "kerosene_ratio", nullable = false)
    private Double keroseneRatio;

    @Column(name = "chamber_pressure", nullable = false)
    private Double chamberPressure;

    @Column(name = "initial_temperature", nullable = false)
    private Double initialTemperature;

    @Column(name = "burn_duration", nullable = false)
    private Double burnDuration;

    @Column(name = "nozzle_area_ratio", nullable = false)
    private Double nozzleAreaRatio;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
