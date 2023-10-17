package be.kuleuven.foodrestservice.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Request body for a meal
 */

@Schema(name = "MealUpdateRequest", description = "Request body for a meal")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-10-16T15:19:46.752948262+02:00[Europe/Brussels]")
public class MealUpdateRequest {

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

  public MealUpdateRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public MealUpdateRequest(String name, MealTypeEnum mealType) {
    this.name = name;
    this.mealType = mealType;
  }

  public MealUpdateRequest name(String name) {
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

  public MealUpdateRequest price(Double price) {
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

  public MealUpdateRequest kcal(Integer kcal) {
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

  public MealUpdateRequest description(String description) {
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

  public MealUpdateRequest mealType(MealTypeEnum mealType) {
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
    MealUpdateRequest mealUpdateRequest = (MealUpdateRequest) o;
    return Objects.equals(this.name, mealUpdateRequest.name) &&
        Objects.equals(this.price, mealUpdateRequest.price) &&
        Objects.equals(this.kcal, mealUpdateRequest.kcal) &&
        Objects.equals(this.description, mealUpdateRequest.description) &&
        Objects.equals(this.mealType, mealUpdateRequest.mealType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, price, kcal, description, mealType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MealUpdateRequest {\n");
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

