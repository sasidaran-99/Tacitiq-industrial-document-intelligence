package com.tacitiq.modules.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private UUID id;
    private String email;
    private String role;
    private UUID plantId;
    private List<String> expertiseAreas;
    private LocalDate retirementDate;
    private Integer yearsExperience;
}
