package com.klu.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "safety_resources")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Optimistic locking version control
    @Version
    private Long version;

    // Resource/Service Title
    private String name;
    
    // Detailed description of the resource/service
    @Column(columnDefinition = "TEXT")
    private String description;
    
    // Type of resource (e.g., SafetyTip, SupportService, LegalResource, CounselingTip)
    private String resourceType;
    
    // Target user roles (comma-separated: Victim, Counsellor, LegalAdvisor, Admin)
    private String targetAudience;
    
    // Contact information or resource link
    private String contactInfo;
    
    // Emergency contact number if applicable
    private String emergencyHotline;
    
    // Whether this resource is active
    private Boolean isActive = true;
    
    // Relevance or priority level (1-10)
    private Integer priorityLevel;
    
    // Image/Icon URL for the resource
    private String imageUrl;
    
    // Created timestamp
    private LocalDateTime createdAt;
    
    // Last updated timestamp
    private LocalDateTime updatedAt;
    
    // Admin/Creator ID
    private Long createdBy;

    // No-argument constructor (required by JPA)
    public Product() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // All-argument constructor
    public Product(String name, String description, String resourceType, String targetAudience, 
                   String contactInfo, String emergencyHotline, Integer priorityLevel, String imageUrl) {
        this.name = name;
        this.description = description;
        this.resourceType = resourceType;
        this.targetAudience = targetAudience;
        this.contactInfo = contactInfo;
        this.emergencyHotline = emergencyHotline;
        this.priorityLevel = priorityLevel;
        this.imageUrl = imageUrl;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getTargetAudience() {
        return targetAudience;
    }

    public void setTargetAudience(String targetAudience) {
        this.targetAudience = targetAudience;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public String getEmergencyHotline() {
        return emergencyHotline;
    }

    public void setEmergencyHotline(String emergencyHotline) {
        this.emergencyHotline = emergencyHotline;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(Integer priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
}