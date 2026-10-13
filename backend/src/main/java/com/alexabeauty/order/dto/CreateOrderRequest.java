package com.alexabeauty.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotBlank(message = "El nombre del cliente es obligatorio")
    @Size(max = 150)
    private String customerName;

    @NotBlank(message = "El teléfono de contacto es obligatorio")
    @Pattern(regexp = "[+0-9 ()-]{7,30}")
    private String customerPhone;

    @NotBlank(message = "El método de entrega es obligatorio")
    @Pattern(regexp = "ENVIO_DOMICILIO|RETIRO_TIENDA|LOCAL_PICKUP|DOMICILIO")
    private String deliveryType;

    @Size(max = 100)
    private String city;

    @Size(max = 255)
    private String address;

    @Size(max = 2000)
    private String customerNotes;

    @NotEmpty(message = "El pedido debe contener al menos un producto")
    @Valid
    @Size(max = 100)
    private List<OrderItemRequest> items;
}
