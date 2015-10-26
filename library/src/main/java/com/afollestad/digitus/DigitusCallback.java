package com.afollestad.digitus;

/**
 * @author Aidan Follestad (afollestad)
 */
public interface DigitusCallback {

    void onDigitusReady(Digitus digitus);

    void onDigitusListening(boolean newFingerprint);

    void onDigitusAuthenticated(Digitus digitus);

    void onDigitusError(Digitus digitus, DigitusErrorType type, Exception e);
}