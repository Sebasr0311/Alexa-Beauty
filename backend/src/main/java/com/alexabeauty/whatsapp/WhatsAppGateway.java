package com.alexabeauty.whatsapp;

import com.alexabeauty.order.domain.Order;

/**
 * Gateway contract for formatting order details and generating WhatsApp interaction URLs.
 */
public interface WhatsAppGateway {

    /**
     * Generates a Click-to-Chat URL (https://wa.me/{number}?text={encoded}) for the given order.
     */
    String generateClickToChatUrl(Order order, String businessPhoneNumber);

    /**
     * Formats the order confirmation message in the official conversational template.
     */
    String formatOrderMessage(Order order);
}
