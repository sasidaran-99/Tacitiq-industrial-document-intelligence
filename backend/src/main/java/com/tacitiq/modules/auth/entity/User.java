package com.tacitiq.modules.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(nullable = false)
    private String role; // ADMIN, PLANT_MANAGER, MAINTENANCE_ENGINEER, etc.

    @Column(name = "plant_id")
    private UUID plantId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_expertise_areas", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "expertise_area")
    private List<String> expertiseAreas;

    @Column(name = "retirement_date")
    private LocalDate retirementDate;

    @Column(name = "years_experience")
    private Integer yearsExperience;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "profile_picture")
    private String profilePicture;

    @Column(nullable = false)
    @Builder.Default
    private String provider = "LOCAL";

    @Column(name = "google_subject_id", unique = true)
    private String googleSubjectId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "last_active")
    private OffsetDateTime lastActive;
}
