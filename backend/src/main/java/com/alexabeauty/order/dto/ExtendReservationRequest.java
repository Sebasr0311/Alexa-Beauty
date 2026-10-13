package com.alexabeauty.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtendReservationRequest {

    @Min(value = 1, message = "Los minutos adicionales deben ser al menos 1")
    @Max(value = 120, message = "Los minutos adicionales no pueden exceder 120")
    private int additionalMinutes;

    private String notes;
}
