package com.alexabeauty.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBrandRequest(
        @NotBlank(message = "El nombre de la marca es requerido")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String name,

        @Size(max = 120, message = "El slug no puede superar los 120 caracteres")
        String slug,

        Boolean active
) {}
