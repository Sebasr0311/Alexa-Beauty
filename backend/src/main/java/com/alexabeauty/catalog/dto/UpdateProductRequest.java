package com.alexabeauty.catalog.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateProductRequest(
        @Size(max = 200, message = "El nombre no puede superar los 200 caracteres")
        String name,

        UUID brandId,

        UUID categoryId,

        String description,

        @JsonAlias("isFeatured")
        Boolean featured,

        Boolean active
) {}
