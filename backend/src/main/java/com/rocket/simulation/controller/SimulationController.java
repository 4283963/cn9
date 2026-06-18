package com.rocket.simulation.controller;

import com.rocket.simulation.dto.SimulationRequest;
import com.rocket.simulation.dto.SimulationResult;
import com.rocket.simulation.entity.SimulationRecipe;
import com.rocket.simulation.service.SimulationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/simulation")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;

    @GetMapping("/recipes")
    public ResponseEntity<List<SimulationRecipe>> getAllRecipes(
            @RequestParam(required = false) String keyword) {
        List<SimulationRecipe> recipes = simulationService.searchRecipes(keyword);
        return ResponseEntity.ok(recipes);
    }

    @GetMapping("/recipes/{id}")
    public ResponseEntity<SimulationRecipe> getRecipeById(@PathVariable Long id) {
        return simulationService.getRecipeById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/recipes")
    public ResponseEntity<SimulationRecipe> createRecipe(
            @Valid @RequestBody SimulationRequest request) {
        SimulationRecipe recipe = simulationService.createRecipe(request);
        return new ResponseEntity<>(recipe, HttpStatus.CREATED);
    }

    @PutMapping("/recipes/{id}")
    public ResponseEntity<SimulationRecipe> updateRecipe(
            @PathVariable Long id,
            @Valid @RequestBody SimulationRequest request) {
        return simulationService.updateRecipe(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/recipes/{id}")
    public ResponseEntity<Map<String, Boolean>> deleteRecipe(@PathVariable Long id) {
        boolean deleted = simulationService.deleteRecipe(id);
        Map<String, Boolean> response = new HashMap<>();
        response.put("deleted", deleted);

        if (deleted) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/run")
    public ResponseEntity<SimulationResult> runSimulation(
            @Valid @RequestBody SimulationRequest request) {
        SimulationRecipe recipe = simulationService.createRecipe(request);
        SimulationResult result = simulationService.runSimulation(request, recipe.getId());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/run/{id}")
    public ResponseEntity<SimulationResult> runSimulationById(@PathVariable Long id) {
        return simulationService.runSimulationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/preview")
    public ResponseEntity<SimulationResult> previewSimulation(
            @Valid @RequestBody SimulationRequest request) {
        SimulationResult result = simulationService.runSimulation(request, null);
        return ResponseEntity.ok(result);
    }
}
