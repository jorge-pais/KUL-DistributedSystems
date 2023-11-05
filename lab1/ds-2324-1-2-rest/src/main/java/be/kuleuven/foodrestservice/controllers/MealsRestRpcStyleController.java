package be.kuleuven.foodrestservice.controllers;

import be.kuleuven.foodrestservice.domain.Meal;
import be.kuleuven.foodrestservice.domain.MealsRepository;
import be.kuleuven.foodrestservice.exceptions.MealNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Optional;
import java.util.ResourceBundle;

@RestController
public class MealsRestRpcStyleController {

    private final MealsRepository mealsRepository;

    @Autowired
    MealsRestRpcStyleController(MealsRepository mealsRepository) { this.mealsRepository = mealsRepository; }

    @GetMapping("/restrpc/meals/{id}")
    Meal getMealById(@PathVariable String id) {
        Optional<Meal> meal = mealsRepository.findMeal(id);

        return meal.orElseThrow(() -> new MealNotFoundException(id));
    }

    @GetMapping("/restrpc/meals")
    Collection<Meal> getMeals() {
        return mealsRepository.getAllMeal();
    }

    @GetMapping("/restrpc/meals/cheapest")
    Meal getCheapest(){
        Optional<Meal> meal = mealsRepository.findCheapestMeal();

        // No lambda, method reference wtv that means
        return meal.orElseThrow(MealNotFoundException::new);
    }

    @GetMapping("/restrpc/meals/largest")
    Meal getLargest(){
        Optional<Meal> meal = mealsRepository.findLargestMeal();

        return meal.orElseThrow();
    }

    @PostMapping(path = "/restrpc/meals/add",
        consumes = MediaType.APPLICATION_JSON_VALUE)
    //public ResponseEntity<Meal> addMeal(@RequestBody Meal meal)
    public void addMeal(@RequestBody Meal meal){
        mealsRepository.addMeal(meal);

        // return new ResponseEntity<>(meal, HttpStatus.CREATED);
    }

    @PutMapping(path = "/restrpc/meals/update",
        consumes = MediaType.APPLICATION_JSON_VALUE)
    public void updateMeal(@RequestBody Meal meal){
        mealsRepository.updateMeal(meal);
    }

    @DeleteMapping(path = "/restrpc/meals/delete/{id}")
    public void deleteMeal(@PathVariable String id){
        mealsRepository.deleteMeal(id);
    }
}