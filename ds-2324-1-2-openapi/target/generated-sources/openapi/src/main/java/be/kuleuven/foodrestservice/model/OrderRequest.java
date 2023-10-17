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
 * Request body for an order
 */

@Schema(name = "OrderRequest", description = "Request body for an order")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-10-17T11:14:11.305407941+02:00[Europe/Brussels]")
public class OrderRequest {

  private String address;

  @Valid
  private List<UUID> meals = new ArrayList<>();

  public OrderRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public OrderRequest(String address, List<UUID> meals) {
    this.address = address;
    this.meals = meals;
  }

  public OrderRequest address(String address) {
    this.address = address;
    return this;
  }

  /**
   * Destination address of the order
   * @return address
  */
  @NotNull 
  @Schema(name = "address", description = "Destination address of the order", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("address")
  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public OrderRequest meals(List<UUID> meals) {
    this.meals = meals;
    return this;
  }

  public OrderRequest addMealsItem(UUID mealsItem) {
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
    OrderRequest orderRequest = (OrderRequest) o;
    return Objects.equals(this.address, orderRequest.address) &&
        Objects.equals(this.meals, orderRequest.meals);
  }

  @Override
  public int hashCode() {
    return Objects.hash(address, meals);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OrderRequest {\n");
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

