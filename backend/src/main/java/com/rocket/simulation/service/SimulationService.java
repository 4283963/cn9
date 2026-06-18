package com.rocket.simulation.service;

import com.rocket.simulation.dto.SimulationRequest;
import com.rocket.simulation.dto.SimulationResult;
import com.rocket.simulation.entity.SimulationRecipe;
import com.rocket.simulation.repository.SimulationRecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SimulationService {

    private final SimulationRecipeRepository recipeRepository;
    private final PropulsionCalculationService calculationService;

    public List<SimulationRecipe> getAllRecipes() {
        return recipeRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<SimulationRecipe> searchRecipes(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllRecipes();
        }
        return recipeRepository.findByNameContainingIgnoreCaseOrderByCreatedAtDesc(keyword);
    }

    public Optional<SimulationRecipe> getRecipeById(Long id) {
        return recipeRepository.findById(id);
    }

    @Transactional
    public SimulationRecipe createRecipe(SimulationRequest request) {
        SimulationRecipe recipe = new SimulationRecipe();
        recipe.setName(request.getName());
        recipe.setDescription(request.getDescription());
        recipe.setLoxRatio(request.getLoxRatio());
        recipe.setKeroseneRatio(request.getKeroseneRatio());
        recipe.setChamberPressure(request.getChamberPressure());
        recipe.setInitialTemperature(request.getInitialTemperature());
        recipe.setBurnDuration(request.getBurnDuration());
        recipe.setNozzleAreaRatio(request.getNozzleAreaRatio());

        return recipeRepository.save(recipe);
    }

    @Transactional
    public Optional<SimulationRecipe> updateRecipe(Long id, SimulationRequest request) {
        return recipeRepository.findById(id).map(recipe -> {
            recipe.setName(request.getName());
            recipe.setDescription(request.getDescription());
            recipe.setLoxRatio(request.getLoxRatio());
            recipe.setKeroseneRatio(request.getKeroseneRatio());
            recipe.setChamberPressure(request.getChamberPressure());
            recipe.setInitialTemperature(request.getInitialTemperature());
            recipe.setBurnDuration(request.getBurnDuration());
            recipe.setNozzleAreaRatio(request.getNozzleAreaRatio());
            return recipeRepository.save(recipe);
        });
    }

    @Transactional
    public boolean deleteRecipe(Long id) {
        return recipeRepository.findById(id)
                .map(recipe -> {
                    recipeRepository.delete(recipe);
                    return true;
                })
                .orElse(false);
    }

    public SimulationResult runSimulation(SimulationRequest request, Long recipeId) {
        return calculationService.calculate(request, recipeId);
    }

    public Optional<SimulationResult> runSimulationById(Long id) {
        return recipeRepository.findById(id).map(recipe -> {
            SimulationRequest request = convertToRequest(recipe);
            return calculationService.calculate(request, recipe.getId());
        });
    }

    private SimulationRequest convertToRequest(SimulationRecipe recipe) {
        SimulationRequest request = new SimulationRequest();
        request.setName(recipe.getName());
        request.setDescription(recipe.getDescription());
        request.setLoxRatio(recipe.getLoxRatio());
        request.setKeroseneRatio(recipe.getKeroseneRatio());
        request.setChamberPressure(recipe.getChamberPressure());
        request.setInitialTemperature(recipe.getInitialTemperature());
        request.setBurnDuration(recipe.getBurnDuration());
        request.setNozzleAreaRatio(recipe.getNozzleAreaRatio());
        return request;
    }
}
