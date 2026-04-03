package com.aigc.contentfactory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReviewActionRequest {

    @NotBlank
    private String comment;
}
