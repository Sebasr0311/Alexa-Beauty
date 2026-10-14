package com.alexabeauty.whatsapp;

import com.alexabeauty.order.domain.Order;
import com.alexabeauty.order.domain.OrderItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Click-to-Chat implementation for generating wa.me prefilled URLs.
 */
@Slf4j
@Component
public class ClickToChatGateway implements WhatsAppGateway {

    @Override
    public String generateClickToChatUrl(Order order, String businessPhoneNumber) {
        String message = formatOrderMessage(order);
        String cleanNumber = businessPhoneNumber != null ? businessPhoneNumber.replaceAll("[^0-9]", "") : "";
        String encodedText = URLEncoder.encode(message, StandardCharsets.UTF_8).replace("+", "%20");
        return "https://wa.me/" + cleanNumber + "?text=" + encodedText;
    }

    @Override
    public String formatOrderMessage(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hola, Alexa Beauty 🌷\n");
        sb.append("Quiero consultar y confirmar mi pedido ").append(order.getReference()).append(":\n");

        if (order.getItems() != null && !order.getItems().isEmpty()) {
            for (OrderItem item : order.getItems()) {
                String shade = (item.getVariantName() != null && !item.getVariantName().isBlank())
                        ? " " + item.getVariantName().trim()
                        : "";
                sb.append("• ")
                        .append(item.getProductName().trim())
                        .append(shade)
                        .append(" × ")
                        .append(item.getQuantity())
                        .append(" — $ ")
                        .append(formatAmount(item.getSubtotal()))
                        .append("\n");
            }
        }

        sb.append("Total productos: $ ").append(formatAmount(order.getTotal())).append("\n");
        boolean isDelivery = "ENVIO_DOMICILIO".equalsIgnoreCase(order.getDeliveryType())
                || "DOMICILIO".equalsIgnoreCase(order.getDeliveryType());
        if (isDelivery) {
            sb.append("Entrega: Domicilio en Valledupar (costo de envío pagadero contraentrega)\n");
            if (order.getAddress() != null && !order.getAddress().isBlank()) {
                sb.append("Dirección: ").append(order.getAddress().trim()).append("\n");
            }
            sb.append("Ciudad: Valledupar\n");
        } else {
            sb.append("Entrega: Retiro en tienda física (Valledupar)\n");
        }
        if (order.getCustomerNotes() != null && !order.getCustomerNotes().isBlank()) {
            sb.append("Detalles / Pago: ").append(order.getCustomerNotes().trim()).append("\n");
        }
        sb.append("Nombre: ").append(order.getCustomerName() != null ? order.getCustomerName().trim() : "").append("\n");
        sb.append("¿Me ayudan a confirmar disponibilidad y cómo realizar el pago?");

        return sb.toString();
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("es", "CO"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#,##0", symbols);
        return df.format(amount);
    }
}
