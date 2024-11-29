package dev.formation.JavaService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CodeResponse {
    private String output;
    private String executionTime;
    private String memoryUsage;
}
