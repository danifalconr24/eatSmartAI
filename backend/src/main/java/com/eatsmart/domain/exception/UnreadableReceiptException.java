package com.eatsmart.domain.exception;

/**
 * The image provided is not a legible supermarket receipt, according to the
 * AI provider. This is a valid business answer, not a technical failure.
 */
public class UnreadableReceiptException extends Exception {

    public UnreadableReceiptException(String message) {
        super(message);
    }
}
