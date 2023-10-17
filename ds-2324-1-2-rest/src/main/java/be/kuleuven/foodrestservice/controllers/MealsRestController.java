package be.kuleuven.foodrestservice.controllers;

import be.kuleuven.foodrestservice.domain.Meal;
import be.kuleuven.foodrestservice.domain.MealsRepository;
import be.kuleuven.foodrestservice.exceptions.MealNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.*;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
public class MealsRestController {

    private final MealsRepository mealsRepository;

    @Autowired
    MealsRestController(MealsRepository mealsRepository) {
        this.mealsRepository = mealsRepository;
    }

    @Operation(summary = "Get a meal by its id", description = "Get a meal by id description")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the meal",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Meal.class))}),
            @ApiResponse(responseCode = "404", description = "Meal not found", content = @Content)})
    @GetMapping("/rest/meals/{id}")
    ResponseEntity<?> getMealById(
            @Parameter(description = "Id of the meal", schema = @Schema(format = "uuid", type = "string"))
            @PathVariable String id) {
        Meal meal = mealsRepository.findMeal(id).orElseThrow(() -> new MealNotFoundException(id));
        EntityModel<Meal> mealEntityModel = mealToEntityModel(id, meal);
        return ResponseEntity.ok(mealEntityModel);
    }

    @GetMapping("/rest/meals")
    CollectionModel<EntityModel<Meal>> getMeals() {
        Collection<Meal> meals = mealsRepository.getAllMeal();

        List<EntityModel<Meal>> mealEntityModels = new ArrayList<>();
        for (Meal m : meals) {
            EntityModel<Meal> em = mealToEntityModel(m.getId(), m);
            mealEntityModels.add(em);
        }
        return CollectionModel.of(mealEntityModels,
                linkTo(methodOn(MealsRestController.class).getMeals()).withSelfRel());
    }

    // ---------------- REQUESTED EXTENSIONS ------------------------ //

    @Operation(summary = "Get cheapest meal", description = "Get the cheapest available meal")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the chepeast meal",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Meal.class))}),
            @ApiResponse(responseCode = "404", description = "Meal not found", content = @Content)})
    @GetMapping("/rest/cheapest-meal")
    ResponseEntity<?> findCheapestMeal() {
        Meal meal = mealsRepository.findCheapestMeal().orElseThrow(() -> new MealNotFoundException());
        EntityModel<Meal> mealEntityModel = mealToEntityModel(meal.getId(), meal);
        return ResponseEntity.ok(mealEntityModel);
    }

    @Operation(summary = "Get largest meal", description = "Get the largest available meal")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the largest meal",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Meal.class))}),
            @ApiResponse(responseCode = "404", description = "Meal not found", content = @Content)})
    @GetMapping("/rest/largest-meal")
    ResponseEntity<?> findLargestMeal() {
        Meal meal = mealsRepository.findLargestMeal().orElseThrow(() -> new MealNotFoundException());
        EntityModel<Meal> mealEntityModel = mealToEntityModel(meal.getId(), meal);
        return ResponseEntity.ok(mealEntityModel);
    }

    @Operation(summary = "Add meal", description = "Add the meal to the available meals")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Added the meal",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Meal.class))}),
            @ApiResponse(responseCode = "404", description = "Meal not Added", content = @Content)})
    @PostMapping("/rest/meals")
    ResponseEntity<?> addMeal(@RequestBody Meal meal) {
        mealsRepository.addMeal(meal);

        // Create a URI for the newly added meal
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(meal.getId())
                .toUri();

        return ResponseEntity.created(location).build();
    }

    @Operation(summary = "Update meal", description = "Update the meal on the available meals")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated the meal",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Meal.class))}),
            @ApiResponse(responseCode = "404", description = "Meal not Found", content = @Content)})
    @PutMapping("/rest/meals/{id}")
    void updateMeal(@PathVariable String id, @RequestBody Meal updatedMeal) {
        Optional<Meal> existingMeal = mealsRepository.findMeal(id);
        if (existingMeal.isEmpty()) {
            throw new MealNotFoundException();
        }
        mealsRepository.updateMeal(updatedMeal); // Update the meal in the repository
    }

    @Operation(summary = "Delete meal", description = "Delete the meal on the available meals")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Deleted the meal",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Meal.class))}),
            @ApiResponse(responseCode = "404", description = "Meal not Found", content = @Content)})
    @DeleteMapping("/rest/meals/{id}")
    ResponseEntity<?> deleteMeal(@PathVariable String id) {

        Meal existingMeal = mealsRepository.findMeal(id).orElseThrow(() -> new MealNotFoundException(id));
        EntityModel<Meal> mealEntityModel = mealToEntityModel(id, existingMeal);
        mealsRepository.deleteMeal(id);
        return ResponseEntity.ok(mealEntityModel);
    }

    private EntityModel<Meal> mealToEntityModel(String id, Meal meal) {
        return EntityModel.of(meal,
                linkTo(methodOn(MealsRestController.class).getMealById(id)).withSelfRel(),
                linkTo(methodOn(MealsRestController.class).getMeals()).withRel("All Meals"));
    }
}
