package be.kuleuven.foodrestservice.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.UUID;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A Delicious meal
 */

@Schema(name = "Meal", description = "A Delicious meal")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-10-17T11:14:11.305407941+02:00[Europe/Brussels]")
public class Meal {

  private UUID id;

  private String name;

  private Double price;

  private Integer kcal;

  private String description;

  /**
   * The type of meal
   */
  public enum MealTypeEnum {
    VEGAN("VEGAN"),
    
    VEGGIE("VEGGIE"),
    
    MEAT("MEAT"),
    
    FISH("FISH");

    private String value;

    MealTypeEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static MealTypeEnum fromValue(String value) {
      for (MealTypeEnum b : MealTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private MealTypeEnum mealType;

  public Meal() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Meal(String name, MealTypeEnum mealType) {
    this.name = name;
    this.mealType = mealType;
  }

  public Meal id(UUID id) {
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

  public Meal name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of the meal
   * @return name
  */
  @NotNull 
  @Schema(name = "name", description = "The name of the meal", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Meal price(Double price) {
    this.price = price;
    return this;
  }

  /**
   * The price of the meal
   * @return price
  */
  
  @Schema(name = "price", description = "The price of the meal", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("price")
  public Double getPrice() {
    return price;
  }

  public void setPrice(Double price) {
    this.price = price;
  }

  public Meal kcal(Integer kcal) {
    this.kcal = kcal;
    return this;
  }

  /**
   * The energetic value of the meal
   * @return kcal
  */
  
  @Schema(name = "kcal", description = "The energetic value of the meal", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("kcal")
  public Integer getKcal() {
    return kcal;
  }

  public void setKcal(Integer kcal) {
    this.kcal = kcal;
  }

  public Meal description(String description) {
    this.description = description;
    return this;
  }

  /**
   * A description of the meal
   * @return description
  */
  
  @Schema(name = "description", description = "A description of the meal", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Meal mealType(MealTypeEnum mealType) {
    this.mealType = mealType;
    return this;
  }

  /**
   * The type of meal
   * @return mealType
  */
  @NotNull 
  @Schema(name = "mealType", description = "The type of meal", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("mealType")
  public MealTypeEnum getMealType() {
    return mealType;
  }

  public void setMealType(MealTypeEnum mealType) {
    this.mealType = mealType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Meal meal = (Meal) o;
    return Objects.equals(this.id, meal.id) &&
        Objects.equals(this.name, meal.name) &&
        Objects.equals(this.price, meal.price) &&
        Objects.equals(this.kcal, meal.kcal) &&
        Objects.equals(this.description, meal.description) &&
        Objects.equals(this.mealType, meal.mealType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, price, kcal, description, mealType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Meal {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    price: ").append(toIndentedString(price)).append("\n");
    sb.append("    kcal: ").append(toIndentedString(kcal)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    mealType: ").append(toIndentedString(mealType)).append("\n");
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

