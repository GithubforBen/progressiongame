package com.financegame.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "required_education_type", length = 50)
    private String requiredEducationType;

    @Column(name = "required_education_field", length = 50)
    private String requiredEducationField;

    @Column(name = "required_months_experience", nullable = false)
    private int requiredMonthsExperience = 0;

    @Column(name = "salary", nullable = false, precision = 10, scale = 2)
    private BigDecimal salary;

    @Column(name = "stress_per_month", nullable = false)
    private int stressPerMonth = 5;

    @Column(name = "available", nullable = false)
    private boolean available = true;

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRequiredEducationType() { return requiredEducationType; }
    public void setRequiredEducationType(String t) { this.requiredEducationType = t; }

    public String getRequiredEducationField() { return requiredEducationField; }
    public void setRequiredEducationField(String f) { this.requiredEducationField = f; }

    public int getRequiredMonthsExperience() { return requiredMonthsExperience; }
    public void setRequiredMonthsExperience(int m) { this.requiredMonthsExperience = m; }

    public BigDecimal getSalary() { return salary; }
    public void setSalary(BigDecimal salary) { this.salary = salary; }

    public int getStressPerMonth() { return stressPerMonth; }
    public void setStressPerMonth(int s) { this.stressPerMonth = s; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}
