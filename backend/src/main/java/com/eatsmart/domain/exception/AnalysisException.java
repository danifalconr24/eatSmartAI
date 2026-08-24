package com.eatsmart.domain.exception;

/**
 * Technical failure while obtaining an analysis: provider errors, transport
 * failures or unusable responses. Triggers fallback to the next provider.
 */
public class AnalysisException extends Exception {

    public AnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
