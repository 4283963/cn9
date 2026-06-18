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
    private static final double MIN_FINITE = 1e-12;
    private static final double MAX_REASONABLE_THRUST = 1e9;
    private static final double MAX_REASONABLE_VELOCITY = 20000.0;
    private static final double MAX_REASONABLE_MACH = 10.0;
    private static final double MIN_REASONABLE_MACH = 1.0;
    private static final int MAX_MACH_ITERATIONS = 100;
    private static final double MACH_TOLERANCE = 1e-6;
    private static final int MAX_THRUST_POINTS = 2000;

    private static class CombustionResult {
        double temperature;
        double molarMass;
        double gamma;
    }

    public SimulationResult calculate(SimulationRequest request, Long recipeId) {
        double loxRatio = safeValue(request.getLoxRatio(), 2.56);
        double keroseneRatio = safeValue(request.getKeroseneRatio(), 1.0);
        double chamberPressure = clamp(request.getChamberPressure(), 1.0, 50.0);
        double initialTemp = clamp(request.getInitialTemperature(), 200.0, 800.0);
        double burnDuration = clamp(request.getBurnDuration(), 1.0, 600.0);
        double nozzleAreaRatio = clamp(request.getNozzleAreaRatio(), 1.0, 100.0);

        double ofRatio = loxRatio / Math.max(keroseneRatio, MIN_FINITE);
        if (!isFinite(ofRatio)) {
            ofRatio = 2.56;
        }

        CombustionResult combustion = calculateCombustion(ofRatio, initialTemp, chamberPressure);

        double characteristicVelocity = calculateCharacteristicVelocity(
                combustion.temperature,
                combustion.gamma,
                combustion.molarMass
        );

        double throatArea = 0.01;
        double massFlowRate = (chamberPressure * 1_000_000 * throatArea) / Math.max(characteristicVelocity, MIN_FINITE);
        if (!isFinite(massFlowRate) || massFlowRate <= 0) {
            massFlowRate = 100.0;
        }

        double[] nozzleResults = calculateNozzleFlow(
                combustion.temperature,
                combustion.gamma,
                combustion.molarMass,
                chamberPressure,
                nozzleAreaRatio
        );

        double exitVelocity = clamp(nozzleResults[0], 0, MAX_REASONABLE_VELOCITY);
        double exitMach = clamp(nozzleResults[1], MIN_REASONABLE_MACH, MAX_REASONABLE_MACH);
        double exitTemp = clamp(nozzleResults[2], 100.0, combustion.temperature);
        double exitPressure = clamp(nozzleResults[3], 0.001, chamberPressure);

        double thrust = calculateThrust(massFlowRate, exitVelocity, exitPressure, throatArea * nozzleAreaRatio);
        thrust = clamp(thrust, 0.0, MAX_REASONABLE_THRUST);

        double specificImpulse = isFinite(exitVelocity) ? exitVelocity / G0 : 0.0;
        double thrustCoeffDenominator = chamberPressure * 1_000_000 * throatArea;
        double thrustCoefficient = thrust / Math.max(thrustCoeffDenominator, MIN_FINITE);

        List<ThrustPoint> thrustCurve = generateThrustCurve(
                thrust,
                massFlowRate,
                combustion,
                chamberPressure,
                burnDuration,
                nozzleAreaRatio
        );

        double maxThrust_N = thrustCurve.stream()
                .mapToDouble(ThrustPoint::getThrust)
                .max()
                .orElse(thrust);

        double averageThrust_N = thrustCurve.stream()
                .mapToDouble(ThrustPoint::getThrust)
                .average()
                .orElse(thrust);

        double totalImpulse_Ns = averageThrust_N * burnDuration;

        List<ThrustPoint> thrustCurve_kN = new ArrayList<>();
        for (ThrustPoint point : thrustCurve) {
            double thrust_kN = point.getThrust() / 1000.0;
            thrust_kN = round2(thrust_kN);
            thrustCurve_kN.add(new ThrustPoint(point.getTime(), thrust_kN));
        }

        double maxThrust_kN = round2(maxThrust_N / 1000.0);
        double averageThrust_kN = round2(averageThrust_N / 1000.0);
        double totalImpulse_kNs = round2(totalImpulse_Ns / 1000.0);

        SimulationResult result = new SimulationResult();
        result.setRecipeId(recipeId);
        result.setRecipeName(request.getName());
        result.setExhaustVelocity(round2(exitVelocity));
        result.setSpecificImpulse(round2(specificImpulse));
        result.setMaxThrust(maxThrust_kN);
        result.setAverageThrust(averageThrust_kN);
        result.setTotalImpulse(totalImpulse_kNs);
        result.setMassFlowRate(round4(massFlowRate));
        result.setCharacteristicVelocity(round2(characteristicVelocity));
        result.setThrustCoefficient(round4(thrustCoefficient));
        result.setExitMachNumber(round2(exitMach));
        result.setExitTemperature(round2(exitTemp));
        result.setExitPressure(round4(exitPressure));
        result.setThrustCurve(thrustCurve_kN);

        return result;
    }

    private CombustionResult calculateCombustion(double ofRatio, double initialTemp, double pressure) {
        double stoichiometricOf = 3.46;
        double equivalenceRatio = stoichiometricOf / Math.max(ofRatio, MIN_FINITE);
        if (!isFinite(equivalenceRatio)) {
            equivalenceRatio = 1.0;
        }

        double flameTemp;
        if (equivalenceRatio <= 1.0) {
            flameTemp = 3670 * (1 - 0.15 * Math.abs(equivalenceRatio - 1.0));
        } else {
            flameTemp = 3670 * (1 - 0.25 * (equivalenceRatio - 1.0));
        }

        flameTemp = Math.min(flameTemp, initialTemp + 3500);
        flameTemp = Math.max(flameTemp, 1500.0);
        flameTemp = clamp(flameTemp, 1000.0, 5000.0);

        double avgMolarMass = 28.7 * (1 + 0.1 * (equivalenceRatio - 1.0));
        avgMolarMass = clamp(avgMolarMass, 20.0, 40.0);

        double gamma = 1.22 * (1 - 0.05 * (equivalenceRatio - 1.0));
        gamma = clamp(gamma, 1.12, 1.35);

        CombustionResult result = new CombustionResult();
        result.temperature = flameTemp;
        result.molarMass = avgMolarMass;
        result.gamma = gamma;

        return result;
    }

    private double calculateCharacteristicVelocity(double temp, double gamma, double molarMass) {
        if (!isFinite(temp) || !isFinite(gamma) || !isFinite(molarMass)) {
            return 1500.0;
        }

        double rGas = R_UNIVERSAL / Math.max(molarMass, MIN_FINITE);
        if (!isFinite(rGas) || rGas <= 0) {
            return 1500.0;
        }

        double gammaRT = gamma * rGas * temp;
        if (!isFinite(gammaRT) || gammaRT <= 0) {
            return 1500.0;
        }

        double numerator = Math.sqrt(gammaRT);
        if (!isFinite(numerator) || numerator <= 0) {
            return 1500.0;
        }

        double gammaPlus1 = gamma + 1.0;
        double gammaMinus1 = gamma - 1.0;

        if (Math.abs(gammaMinus1) < MIN_FINITE) {
            return numerator / gamma;
        }

        double term1 = 2.0 / gammaPlus1;
        double sqrtTerm1 = Math.sqrt(term1);

        double exponent = gammaPlus1 / (2.0 * gammaMinus1);
        exponent = clamp(exponent, 0.1, 20.0);

        double powTerm = Math.pow(term1, exponent);
        if (!isFinite(powTerm) || powTerm <= 0) {
            powTerm = 0.5;
        }

        double denominator = gamma * sqrtTerm1 * powTerm;
        if (!isFinite(denominator) || denominator <= 0) {
            return 1500.0;
        }

        double cStar = numerator / denominator;
        cStar = clamp(cStar, 500.0, 5000.0);

        return cStar;
    }

    private double[] calculateNozzleFlow(double chamberTemp, double gamma, double molarMass,
                                         double chamberPressure, double areaRatio) {
        double rGas = R_UNIVERSAL / Math.max(molarMass, MIN_FINITE);
        if (!isFinite(rGas) || rGas <= 0) {
            rGas = 287.0;
        }

        double machExit = findMachNumber(gamma, areaRatio);
        machExit = clamp(machExit, MIN_REASONABLE_MACH, MAX_REASONABLE_MACH);

        double machSq = machExit * machExit;
        double tempRatio = 1.0 / (1.0 + (gamma - 1.0) / 2.0 * machSq);
        tempRatio = clamp(tempRatio, 0.01, 1.0);

        double pressureRatio = Math.pow(tempRatio, gamma / (gamma - 1.0));
        if (!isFinite(pressureRatio) || pressureRatio <= 0) {
            pressureRatio = 0.01;
        }
        pressureRatio = clamp(pressureRatio, 1e-6, 1.0);

        double exitPressure = chamberPressure * pressureRatio;
        double exitTemp = chamberTemp * tempRatio;

        double gammaRTe = gamma * rGas * exitTemp;
        if (!isFinite(gammaRTe) || gammaRTe <= 0) {
            gammaRTe = 100000.0;
        }

        double exitVelocity = machExit * Math.sqrt(gammaRTe);
        exitVelocity = clamp(exitVelocity, 0.0, MAX_REASONABLE_VELOCITY);

        return new double[]{exitVelocity, machExit, exitTemp, exitPressure};
    }

    private double findMachNumber(double gamma, double areaRatio) {
        if (!isFinite(areaRatio) || areaRatio <= 1.0) {
            return 1.0;
        }

        double mach = estimateInitialMach(gamma, areaRatio);
        mach = clamp(mach, 1.001, 9.0);

        int stagnationCount = 0;
        double prevError = Double.MAX_VALUE;

        for (int i = 0; i < MAX_MACH_ITERATIONS; i++) {
            double calculatedAr = calculateAreaRatio(gamma, mach);

            if (!isFinite(calculatedAr)) {
                mach = (mach + 1.0) / 2.0;
                stagnationCount = 0;
                continue;
            }

            double error = calculatedAr - areaRatio;

            if (Math.abs(error) < MACH_TOLERANCE) {
                break;
            }

            if (Math.abs(error) >= Math.abs(prevError)) {
                stagnationCount++;
                if (stagnationCount >= 5) {
                    break;
                }
            } else {
                stagnationCount = 0;
            }
            prevError = error;

            double derivative = calculateAreaRatioDerivative(gamma, mach);

            if (!isFinite(derivative) || Math.abs(derivative) < MIN_FINITE) {
                if (error < 0) {
                    mach = Math.min(mach * 1.2, MAX_REASONABLE_MACH);
                } else {
                    mach = Math.max(mach * 0.8, 1.001);
                }
                continue;
            }

            double delta = error / derivative;

            if (!isFinite(delta) || Math.abs(delta) > mach * 0.5) {
                if (error < 0) {
                    mach = Math.min(mach * 1.3, MAX_REASONABLE_MACH);
                } else {
                    mach = Math.max(mach * 0.7, 1.001);
                }
                continue;
            }

            mach = mach - delta;
            mach = clamp(mach, 1.001, MAX_REASONABLE_MACH);
        }

        if (!isFinite(mach) || mach < 1.0) {
            mach = 2.0;
        }

        return mach;
    }

    private double estimateInitialMach(double gamma, double areaRatio) {
        if (areaRatio <= 1.0) return 1.0;

        double mach;
        if (areaRatio < 2.0) {
            mach = 1.0 + 0.6 * (areaRatio - 1.0);
        } else if (areaRatio < 5.0) {
            mach = 1.6 + 0.5 * (areaRatio - 2.0) / 3.0;
        } else if (areaRatio < 10.0) {
            mach = 2.1 + 0.9 * (areaRatio - 5.0) / 5.0;
        } else if (areaRatio < 20.0) {
            mach = 3.0 + 1.5 * (areaRatio - 10.0) / 10.0;
        } else if (areaRatio < 50.0) {
            mach = 4.5 + 2.5 * (areaRatio - 20.0) / 30.0;
        } else {
            mach = 7.0;
        }

        return clamp(mach, 1.001, 9.5);
    }

    private double calculateAreaRatio(double gamma, double mach) {
        if (!isFinite(mach) || mach < MIN_FINITE) {
            return Double.POSITIVE_INFINITY;
        }

        double gammaPlus1 = gamma + 1.0;
        double gammaMinus1 = gamma - 1.0;

        if (Math.abs(gammaMinus1) < MIN_FINITE) {
            return 1.0 / mach;
        }

        double exponent1 = gammaPlus1 / (2.0 * gammaMinus1);
        exponent1 = clamp(exponent1, 0.1, 20.0);

        double term1 = 2.0 / gammaPlus1;
        double term2 = 1.0 + gammaMinus1 / 2.0 * mach * mach;

        double base = term1 * term2;
        if (base <= 0 || !isFinite(base)) {
            return Double.POSITIVE_INFINITY;
        }

        double powResult = Math.pow(base, exponent1);
        if (!isFinite(powResult)) {
            return Double.POSITIVE_INFINITY;
        }

        double result = (1.0 / mach) * powResult;

        if (!isFinite(result) || result <= 0) {
            return Double.POSITIVE_INFINITY;
        }

        return result;
    }

    private double calculateAreaRatioDerivative(double gamma, double mach) {
        double eps = Math.max(mach * 1e-5, 1e-6);

        double arPlus = calculateAreaRatio(gamma, mach + eps);
        double arMinus = calculateAreaRatio(gamma, mach - eps);

        if (!isFinite(arPlus) && !isFinite(arMinus)) {
            return 0.0;
        }
        if (!isFinite(arPlus)) {
            return (arMinus - calculateAreaRatio(gamma, mach - eps * 2)) / eps;
        }
        if (!isFinite(arMinus)) {
            return (calculateAreaRatio(gamma, mach + eps * 2) - arPlus) / eps;
        }

        double derivative = (arPlus - arMinus) / (2.0 * eps);

        if (!isFinite(derivative)) {
            return 0.01;
        }

        return derivative;
    }

    private double calculateThrust(double massFlowRate, double exitVelocity,
                                   double exitPressure, double exitArea) {
        if (!isFinite(massFlowRate) || !isFinite(exitVelocity)
                || !isFinite(exitPressure) || !isFinite(exitArea)) {
            return 0.0;
        }

        double pressureThrust = (exitPressure - ATM_PRESSURE) * 1_000_000 * exitArea;
        double momentumThrust = massFlowRate * exitVelocity;

        double thrust = momentumThrust + pressureThrust;

        if (!isFinite(thrust)) {
            return 0.0;
        }

        return Math.max(thrust, 0.0);
    }

    private List<ThrustPoint> generateThrustCurve(double steadyThrust, double massFlowRate,
                                                  CombustionResult combustion, double chamberPressure,
                                                  double burnDuration, double areaRatio) {
        List<ThrustPoint> curve = new ArrayList<>();

        if (!isFinite(burnDuration) || burnDuration <= 0) {
            curve.add(new ThrustPoint(0.0, 0.0));
            curve.add(new ThrustPoint(1.0, steadyThrust));
            return curve;
        }

        int numPoints = (int) Math.min(Math.max(burnDuration * 20 + 1, 10), MAX_THRUST_POINTS);
        numPoints = Math.max(numPoints, 3);

        double timeStep = burnDuration / (numPoints - 1);
        if (!isFinite(timeStep) || timeStep <= 0) {
            timeStep = burnDuration;
            numPoints = 2;
        }

        double startupTime = Math.min(0.5, burnDuration * 0.1);
        double shutdownTime = Math.min(0.5, burnDuration * 0.1);

        startupTime = Math.min(startupTime, burnDuration * 0.4);
        shutdownTime = Math.min(shutdownTime, burnDuration * 0.4);

        double throatArea = 0.01;

        double lastValidThrust = steadyThrust;

        for (int i = 0; i < numPoints; i++) {
            double time = i * timeStep;

            if (!isFinite(time)) {
                time = burnDuration * i / numPoints;
            }

            double thrustMultiplier = 1.0;

            try {
                if (time < startupTime) {
                    double ratio = time / Math.max(startupTime, MIN_FINITE);
                    thrustMultiplier = 1.0 - Math.exp(-3.0 * ratio);
                } else if (time > burnDuration - shutdownTime) {
                    double shutdownElapsed = time - (burnDuration - shutdownTime);
                    double ratio = shutdownElapsed / Math.max(shutdownTime, MIN_FINITE);
                    thrustMultiplier = Math.exp(-4.0 * ratio);
                } else {
                    double steadyDuration = burnDuration - startupTime - shutdownTime;
                    if (steadyDuration > 0) {
                        double normalizedTime = (time - startupTime) / steadyDuration;
                        normalizedTime = clamp(normalizedTime, 0.0, 1.0);

                        thrustMultiplier = 0.98
                                + 0.04 * Math.sin(2.0 * Math.PI * 5.0 * normalizedTime)
                                + 0.02 * Math.sin(2.0 * Math.PI * 17.0 * normalizedTime);
                    }
                }
            } catch (Exception e) {
                thrustMultiplier = 1.0;
            }

            if (!isFinite(thrustMultiplier)) {
                thrustMultiplier = 1.0;
            }

            double instantThrust;

            try {
                double timeFrac = burnDuration > 0 ? time / burnDuration : 0.0;
                timeFrac = clamp(timeFrac, 0.0, 1.0);

                double tempVariation = 0.99 + 0.02 * Math.sin(2.0 * Math.PI * 3.0 * timeFrac);
                double timeVaryingTemp = combustion.temperature * tempVariation;

                double cStar = calculateCharacteristicVelocity(
                        timeVaryingTemp, combustion.gamma, combustion.molarMass);

                double instantMassFlow = (chamberPressure * 1_000_000 * throatArea)
                        / Math.max(cStar, MIN_FINITE);

                double pressureVariation = 0.995 + 0.01 * Math.sin(2.0 * Math.PI * 2.0 * timeFrac);
                double instantPressure = chamberPressure * pressureVariation;

                double[] nozzle = calculateNozzleFlow(
                        timeVaryingTemp,
                        combustion.gamma,
                        combustion.molarMass,
                        instantPressure,
                        areaRatio
                );

                instantThrust = calculateThrust(
                        instantMassFlow,
                        nozzle[0],
                        nozzle[3],
                        throatArea * areaRatio
                );

                instantThrust *= thrustMultiplier;
                instantThrust = Math.max(instantThrust, 0.0);

            } catch (Exception e) {
                instantThrust = steadyThrust * thrustMultiplier;
            }

            if (!isFinite(instantThrust) || instantThrust < 0) {
                instantThrust = lastValidThrust * 0.95;
            }

            instantThrust = clamp(instantThrust, 0.0, MAX_REASONABLE_THRUST);
            lastValidThrust = instantThrust;

            double roundedTime = Math.round(time * 1000.0) / 1000.0;
            double roundedThrust = Math.round(instantThrust * 100.0) / 100.0;

            curve.add(new ThrustPoint(roundedTime, roundedThrust));
        }

        if (curve.isEmpty()) {
            curve.add(new ThrustPoint(0.0, 0.0));
            curve.add(new ThrustPoint(burnDuration, steadyThrust));
        }

        return curve;
    }

    private boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    private double clamp(double value, double min, double max) {
        if (!isFinite(value)) {
            return min;
        }
        return Math.min(Math.max(value, min), max);
    }

    private double safeValue(Double value, double defaultValue) {
        if (value == null || !isFinite(value)) {
            return defaultValue;
        }
        return value;
    }

    private double round2(double value) {
        if (!isFinite(value)) {
            return 0.0;
        }
        return Math.round(value * 100.0) / 100.0;
    }

    private double round4(double value) {
        if (!isFinite(value)) {
            return 0.0;
        }
        return Math.round(value * 10000.0) / 10000.0;
    }
}
