package com.alexabeauty.config.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for updating the official WhatsApp business number.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWhatsAppNumberRequest {

    @NotBlank(message = "El número de WhatsApp es requerido")
    private String whatsappNumber;
}
