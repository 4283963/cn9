package com.rocket.simulation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
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
    private Boolean safetyCompliant;
    private List<SafetyViolation> safetyViolations;
    private String safetySummary;

    public boolean isSafetyCompliant() {
        return Boolean.TRUE.equals(safetyCompliant);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ThrustPoint {
        private Double time;
        private Double thrust;
        private Double temperature;

        public ThrustPoint(Double time, Double thrust) {
            this.time = time;
            this.thrust = thrust;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SafetyViolation {
        private String violationType;
        private String description;
        private Double startTime;
        private Double endTime;
        private Double duration;
        private Double peakValue;
        private Double threshold;
        private String severity;

        public static SafetyViolation create(String type, String desc,
                                             double start, double end,
                                             double peak, double threshold,
                                             String severity) {
            SafetyViolation v = new SafetyViolation();
            v.setViolationType(type);
            v.setDescription(desc);
            v.setStartTime(Math.round(start * 100.0) / 100.0);
            v.setEndTime(Math.round(end * 100.0) / 100.0);
            v.setDuration(Math.round((end - start) * 100.0) / 100.0);
            v.setPeakValue(Math.round(peak * 100.0) / 100.0);
            v.setThreshold(threshold);
            v.setSeverity(severity);
            return v;
        }
    }
}
