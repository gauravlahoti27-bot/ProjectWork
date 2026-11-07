package com.trekmate.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Entity representing an Add-on Activity or Facility service.
 */
@Entity
@Table(name = "activities")
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank(message = "Category is required")
    @Column(nullable = false, length = 30)
    private String category; // "ACTIVITY" or "FACILITY"

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Price is required")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @NotBlank(message = "Pricing type is required")
    @Column(name = "pricing_type", nullable = false, length = 20)
    private String pricingType; // "PER_PERSON" or "PER_GROUP"

    @Column(name = "icon_name", length = 50)
    private String iconName;

    @Column(nullable = false)
    private Boolean active = true;

    public Activity() {
    }

    public Activity(String name, String category, String description, BigDecimal price, String pricingType, String iconName) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.price = price;
        this.pricingType = pricingType;
        this.iconName = iconName;
        this.active = true;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getPricingType() {
        return pricingType;
    }

    public void setPricingType(String pricingType) {
        this.pricingType = pricingType;
    }

    public String getIconName() {
        return iconName;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public boolean isPerPerson() {
        return "PER_PERSON".equalsIgnoreCase(pricingType);
    }

    public boolean isFacility() {
        return "FACILITY".equalsIgnoreCase(category);
    }
}

