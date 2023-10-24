package be.kuleuven.foodrestservice.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A tall order
 */

@Schema(name = "Order", description = "A tall order")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-10-19T18:47:33.031558363+02:00[Europe/Brussels]")
public class Order {

  private UUID id;

  private String address;

  @Valid
  private List<UUID> meals = new ArrayList<>();

  public Order() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Order(String address, List<UUID> meals) {
    this.address = address;
    this.meals = meals;
  }

  public Order id(UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Unique id of the meal
   * @return id
  */
  @Valid 
  @Schema(name = "id", description = "Unique id of the meal", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Order address(String address) {
    this.address = address;
    return this;
  }

  /**
   * Destination address for the order
   * @return address
  */
  @NotNull 
  @Schema(name = "address", description = "Destination address for the order", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("address")
  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public Order meals(List<UUID> meals) {
    this.meals = meals;
    return this;
  }

  public Order addMealsItem(UUID mealsItem) {
    if (this.meals == null) {
      this.meals = new ArrayList<>();
    }
    this.meals.add(mealsItem);
    return this;
  }

  /**
   * Array of string IDs of all the meals within the order
   * @return meals
  */
  @NotNull @Valid 
  @Schema(name = "meals", description = "Array of string IDs of all the meals within the order", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("meals")
  public List<UUID> getMeals() {
    return meals;
  }

  public void setMeals(List<UUID> meals) {
    this.meals = meals;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Order order = (Order) o;
    return Objects.equals(this.id, order.id) &&
        Objects.equals(this.address, order.address) &&
        Objects.equals(this.meals, order.meals);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, address, meals);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Order {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    address: ").append(toIndentedString(address)).append("\n");
    sb.append("    meals: ").append(toIndentedString(meals)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

