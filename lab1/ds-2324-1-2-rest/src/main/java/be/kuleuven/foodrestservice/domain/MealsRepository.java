package be.kuleuven.foodrestservice.domain;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.AssertTrue;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.*;

@Component
public class MealsRepository {
    // map: id -> meal
    private static final Map<String, Meal> meals = new HashMap<>();

    @PostConstruct
    public void initData() {

        Meal a = new Meal();
        a.setId("5268203c-de76-4921-a3e3-439db69c462a");
        a.setName("Steak");
        a.setDescription("Steak with fries");
        a.setMealType(MealType.MEAT);
        a.setKcal(1100);
        a.setPrice((10.00));

        meals.put(a.getId(), a);

        Meal b = new Meal();
        b.setId("4237681a-441f-47fc-a747-8e0169bacea1");
        b.setName("Portobello");
        b.setDescription("Portobello Mushroom Burger");
        b.setMealType(MealType.VEGAN);
        b.setKcal(637);
        b.setPrice((7.00));

        meals.put(b.getId(), b);

        Meal c = new Meal();
        c.setId("cfd1601f-29a0-485d-8d21-7607ec0340c8");
        c.setName("Fish and Chips");
        c.setDescription("Fried fish with chips");
        c.setMealType(MealType.FISH);
        c.setKcal(950);
        c.setPrice(5.00);

        meals.put(c.getId(), c);
    }

    public Optional<Meal> findMeal(String id) {
        Assert.notNull(id, "The meal id must not be null");
        Meal meal = meals.get(id);
        return Optional.ofNullable(meal);
    }

    public Collection<Meal> getAllMeal() {
        return meals.values();
    }

    // Requested extensions

    public Optional<Meal> findCheapestMeal(){
        Double price = Double.MAX_VALUE;
        Meal cheapest = null;

        for(Meal meal: meals.values()){
            if(meal.getPrice() < price){
                cheapest = meal;
                price = meal.getPrice();
            }
        }

        return Optional.ofNullable(cheapest);
    }

    public Optional<Meal> findLargestMeal(){

        Integer kcal = 0;
        Meal largest = null;

        for(Meal meal: meals.values()){
            if(meal.getKcal() > kcal){
                kcal = meal.getKcal();
                largest = meal;
            }
        }
        return Optional.ofNullable(largest);
    }

    public void addMeal(Meal meal){
        Assert.notNull(meal.getId(), "Meal should not be null");
        meals.put(meal.getId(), meal);
    }

    public void updateMeal(Meal newMeal){
        Assert.notNull(newMeal, "Meal should not be null");
        Assert.notNull(newMeal.getId(), "Meal id should not be null");

        Optional<Meal> currMeal = findMeal(newMeal.getId());
        if(currMeal.isEmpty())
            return;

        /*
        // Modify all parameters (not all checks are being performed i think this might be unsafe)
        currMeal.get().setDescription(newMeal.getDescription());
        currMeal.get().setMealType(newMeal.getMealType());
        currMeal.get().setKcal(newMeal.getKcal());
        currMeal.get().setName(newMeal.getName());
        currMeal.get().setPrice(newMeal.getPrice());*/

        deleteMeal(currMeal.get().getId());
        addMeal(newMeal);
    }

    public void deleteMeal(String id){
        Assert.notNull(id, "id must not be null");

        Optional<Meal> currMeal = findMeal(id);
        if(currMeal.isEmpty())
            return;

        meals.values().remove(currMeal.get());
    }
}
