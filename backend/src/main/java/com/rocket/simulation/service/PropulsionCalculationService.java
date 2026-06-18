package com.rocket.simulation.service;

import com.rocket.simulation.dto.SimulationRequest;
import com.rocket.simulation.dto.SimulationResult;
import com.rocket.simulation.dto.SimulationResult.ThrustPoint;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PropulsionCalculationService {

    private static final double G0 = 9.80665;
    private static final double R_UNIVERSAL = 8314.46;
    private static final double ATM_PRESSURE = 0.101325;

    private static class FuelProperties {
        double molarMass;
        double enthalpyFormation;
        double gamma;
    }

    private static class CombustionResult {
        double temperature;
        double molarMass;
        double gamma;
    }

    public SimulationResult calculate(SimulationRequest request, Long recipeId) {
        double loxRatio = request.getLoxRatio();
        double keroseneRatio = request.getKeroseneRatio();
        double chamberPressure = request.getChamberPressure();
        double initialTemp = request.getInitialTemperature();
        double burnDuration = request.getBurnDuration();
        double nozzleAreaRatio = request.getNozzleAreaRatio();

        double ofRatio = loxRatio / keroseneRatio;

        CombustionResult combustion = calculateCombustion(ofRatio, initialTemp, chamberPressure);

        double characteristicVelocity = calculateCharacteristicVelocity(
                combustion.temperature,
                combustion.gamma,
                combustion.molarMass
        );

        double throatArea = 0.01;
        double massFlowRate = (chamberPressure * 1_000_000 * throatArea) / characteristicVelocity;

        double[] nozzleResults = calculateNozzleFlow(
                combustion.temperature,
                combustion.gamma,
                combustion.molarMass,
                chamberPressure,
                nozzleAreaRatio
        );

        double exitVelocity = nozzleResults[0];
        double exitMach = nozzleResults[1];
        double exitTemp = nozzleResults[2];
        double exitPressure = nozzleResults[3];

        double thrust = calculateThrust(massFlowRate, exitVelocity, exitPressure, throatArea * nozzleAreaRatio);
        double specificImpulse = exitVelocity / G0;
        double thrustCoefficient = thrust / (chamberPressure * 1_000_000 * throatArea);

        List<ThrustPoint> thrustCurve = generateThrustCurve(
                thrust,
                massFlowRate,
                combustion,
                chamberPressure,
                burnDuration,
                nozzleAreaRatio
        );

        double maxThrust = thrustCurve.stream()
                .mapToDouble(ThrustPoint::getThrust)
                .max()
                .orElse(0);

        double averageThrust = thrustCurve.stream()
                .mapToDouble(ThrustPoint::getThrust)
                .average()
                .orElse(0);

        double totalImpulse = averageThrust * burnDuration;

        SimulationResult result = new SimulationResult();
        result.setRecipeId(recipeId);
        result.setRecipeName(request.getName());
        result.setExhaustVelocity(Math.round(exitVelocity * 100.0) / 100.0);
        result.setSpecificImpulse(Math.round(specificImpulse * 100.0) / 100.0);
        result.setMaxThrust(Math.round(maxThrust * 100.0) / 100.0);
        result.setAverageThrust(Math.round(averageThrust * 100.0) / 100.0);
        result.setTotalImpulse(Math.round(totalImpulse * 100.0) / 100.0);
        result.setMassFlowRate(Math.round(massFlowRate * 10000.0) / 10000.0);
        result.setCharacteristicVelocity(Math.round(characteristicVelocity * 100.0) / 100.0);
        result.setThrustCoefficient(Math.round(thrustCoefficient * 10000.0) / 10000.0);
        result.setExitMachNumber(Math.round(exitMach * 100.0) / 100.0);
        result.setExitTemperature(Math.round(exitTemp * 100.0) / 100.0);
        result.setExitPressure(Math.round(exitPressure * 10000.0) / 10000.0);
        result.setThrustCurve(thrustCurve);

        return result;
    }

    private CombustionResult calculateCombustion(double ofRatio, double initialTemp, double pressure) {
        double loxMassFraction = ofRatio / (1 + ofRatio);
        double keroseneMassFraction = 1 / (1 + ofRatio);

        double stoichiometricOf = 3.46;

        double equivalenceRatio = stoichiometricOf / ofRatio;

        double flameTemp;
        if (equivalenceRatio <= 1.0) {
            flameTemp = 3670 * (1 - 0.15 * Math.abs(equivalenceRatio - 1.0));
        } else {
            flameTemp = 3670 * (1 - 0.25 * (equivalenceRatio - 1.0));
        }

        flameTemp = Math.min(flameTemp, initialTemp + 3500);
        flameTemp = Math.max(flameTemp, 1500);

        double avgMolarMass = 0.0287 * (1 + 0.1 * (equivalenceRatio - 1.0));
        double gamma = 1.22 * (1 - 0.05 * (equivalenceRatio - 1.0));
        gamma = Math.min(Math.max(gamma, 1.15), 1.30);

        CombustionResult result = new CombustionResult();
        result.temperature = flameTemp;
        result.molarMass = avgMolarMass;
        result.gamma = gamma;

        return result;
    }

    private double calculateCharacteristicVelocity(double temp, double gamma, double molarMass) {
        double rGas = R_UNIVERSAL / molarMass;
        double numerator = Math.sqrt(gamma * rGas * temp);
        double denominator = gamma * Math.sqrt(2.0 / (gamma + 1.0));
        double exponent = (gamma + 1.0) / (2.0 * (gamma - 1.0));
        denominator *= Math.pow(2.0 / (gamma + 1.0), exponent);

        return numerator / denominator;
    }

    private double[] calculateNozzleFlow(double chamberTemp, double gamma, double molarMass,
                                         double chamberPressure, double areaRatio) {
        double rGas = R_UNIVERSAL / molarMass;

        double machExit = findMachNumber(gamma, areaRatio);

        double pressureRatio = Math.pow(1 + (gamma - 1) / 2 * machExit * machExit, -gamma / (gamma - 1));
        double tempRatio = 1 / (1 + (gamma - 1) / 2 * machExit * machExit);

        double exitPressure = chamberPressure * pressureRatio;
        double exitTemp = chamberTemp * tempRatio;

        double exitVelocity = machExit * Math.sqrt(gamma * rGas * exitTemp);

        return new double[]{exitVelocity, machExit, exitTemp, exitPressure};
    }

    private double findMachNumber(double gamma, double areaRatio) {
        double mach = 1.0;
        double tolerance = 1e-6;
        int maxIterations = 100;

        for (int i = 0; i < maxIterations; i++) {
            double calculatedAr = calculateAreaRatio(gamma, mach);
            double derivative = calculateAreaRatioDerivative(gamma, mach);

            double error = calculatedAr - areaRatio;
            if (Math.abs(error) < tolerance) {
                break;
            }

            mach = mach - error / derivative;
            mach = Math.max(mach, 1.001);
            mach = Math.min(mach, 10.0);
        }

        return mach;
    }

    private double calculateAreaRatio(double gamma, double mach) {
        double exponent1 = (gamma + 1) / (2 * (gamma - 1));
        double term1 = 2.0 / (gamma + 1);
        double term2 = 1 + (gamma - 1) / 2 * mach * mach;

        return (1.0 / mach) * Math.pow(term1 * term2, exponent1);
    }

    private double calculateAreaRatioDerivative(double gamma, double mach) {
        double eps = 1e-6;
        double ar1 = calculateAreaRatio(gamma, mach + eps);
        double ar2 = calculateAreaRatio(gamma, mach - eps);
        return (ar1 - ar2) / (2 * eps);
    }

    private double calculateThrust(double massFlowRate, double exitVelocity,
                                   double exitPressure, double exitArea) {
        double pressureThrust = (exitPressure - ATM_PRESSURE) * 1_000_000 * exitArea;
        double momentumThrust = massFlowRate * exitVelocity;
        return momentumThrust + pressureThrust;
    }

    private List<ThrustPoint> generateThrustCurve(double steadyThrust, double massFlowRate,
                                                  CombustionResult combustion, double chamberPressure,
                                                  double burnDuration, double areaRatio) {
        List<ThrustPoint> curve = new ArrayList<>();
        int numPoints = Math.min((int) (burnDuration * 20) + 1, 1000);
        double timeStep = burnDuration / (numPoints - 1);

        double startupTime = Math.min(0.5, burnDuration * 0.1);
        double shutdownTime = Math.min(0.5, burnDuration * 0.1);

        double throatArea = 0.01;

        for (int i = 0; i < numPoints; i++) {
            double time = i * timeStep;
            double thrustMultiplier = 1.0;

            if (time < startupTime) {
                thrustMultiplier = 1 - Math.exp(-3 * time / startupTime);
            } else if (time > burnDuration - shutdownTime) {
                double shutdownElapsed = time - (burnDuration - shutdownTime);
                thrustMultiplier = Math.exp(-4 * shutdownElapsed / shutdownTime);
            } else {
                double normalizedTime = (time - startupTime) / (burnDuration - startupTime - shutdownTime);
                thrustMultiplier = 0.98 + 0.04 * Math.sin(2 * Math.PI * 5 * normalizedTime)
                        + 0.02 * Math.sin(2 * Math.PI * 17 * normalizedTime);
            }

            double timeVaryingTemp = combustion.temperature *
                    (0.99 + 0.02 * Math.sin(2 * Math.PI * 3 * time / burnDuration));
            double cStar = calculateCharacteristicVelocity(timeVaryingTemp, combustion.gamma, combustion.molarMass);
            double instantMassFlow = (chamberPressure * 1_000_000 * throatArea) / cStar;

            double[] nozzle = calculateNozzleFlow(
                    timeVaryingTemp,
                    combustion.gamma,
                    combustion.molarMass,
                    chamberPressure * (0.995 + 0.01 * Math.sin(2 * Math.PI * 2 * time / burnDuration)),
                    areaRatio
            );

            double instantThrust = calculateThrust(
                    instantMassFlow,
                    nozzle[0],
                    nozzle[3],
                    throatArea * areaRatio
            );

            instantThrust *= thrustMultiplier;
            instantThrust = Math.max(0, instantThrust);

            curve.add(new ThrustPoint(
                    Math.round(time * 1000.0) / 1000.0,
                    Math.round(instantThrust * 100.0) / 100.0
            ));
        }

        return curve;
    }
}
