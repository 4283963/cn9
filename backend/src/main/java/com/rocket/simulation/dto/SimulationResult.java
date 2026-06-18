package com.rocket.simulation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulationResult {

    private Long recipeId;
    private String recipeName;
    private Double exhaustVelocity;
    private Double specificImpulse;
    private Double maxThrust;
    private Double averageThrust;
    private Double totalImpulse;
    private Double massFlowRate;
    private Double characteristicVelocity;
    private Double thrustCoefficient;
    private Double exitMachNumber;
    private Double exitTemperature;
    private Double exitPressure;
    private List<ThrustPoint> thrustCurve;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ThrustPoint {
        private Double time;
        private Double thrust;
    }
}
