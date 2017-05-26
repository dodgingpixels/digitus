package com.afollestad.digitus;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.support.annotation.RequiresPermission;

/**
 * @author Aidan Follestad (afollestad)
 */
@TargetApi(Build.VERSION_CODES.M)
class AuthenticationHandler extends FingerprintManager.AuthenticationCallback {

    private CancellationSignal mCancellationSignal;
    private boolean mSelfCancelled;
    private FingerprintManager.CryptoObject mCryptoObject;
    private Context mContext;

    private Digitus mDigitus;

    public AuthenticationHandler(Digitus digitus, FingerprintManager.CryptoObject cryptoObject) {
        mDigitus = digitus;
        mCryptoObject = cryptoObject;
        mContext = digitus.mContext.getApplicationContext();
    }

    public boolean isReadyToStart() {
        return mCancellationSignal == null;
    }

    @RequiresPermission(Manifest.permission.USE_FINGERPRINT)
    public void start() {
        mCancellationSignal = new CancellationSignal();
        mSelfCancelled = false;
        mDigitus.mFingerprintManager.authenticate(mCryptoObject, mCancellationSignal, 0 /* flags */, this, null);
    }

    public void stop() {
        if (mCancellationSignal != null) {
            mSelfCancelled = true;
            mCancellationSignal.cancel();
            mCancellationSignal = null;
        }
    }

    // Callbacks from FingerprintManager

    @Override
    public void onAuthenticationError(int errorCode, CharSequence errString) {
        super.onAuthenticationError(errorCode, errString);
        if (!mSelfCancelled) {
            if (mDigitus.mCallback != null)
                mDigitus.mCallback.onDigitusError(mDigitus, DigitusErrorType.UNRECOVERABLE_ERROR, new Exception(errString.toString()));
        }
        stop();
        mDigitus.mFingerprintManager = mContext.getSystemService(FingerprintManager.class);
    }

    @Override
    public void onAuthenticationFailed() {
        super.onAuthenticationFailed();
        if (mDigitus.mCallback != null)
            mDigitus.mCallback.onDigitusError(mDigitus, DigitusErrorType.FINGERPRINT_NOT_RECOGNIZED, new Exception("Fingerprint not recognized, try again."));
    }

    @Override
    public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
        super.onAuthenticationHelp(helpCode, helpString);
        if (mDigitus.mCallback != null)
            mDigitus.mCallback.onDigitusError(mDigitus, DigitusErrorType.HELP_ERROR, new Exception(helpString.toString()));
    }

    @Override
    public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
        super.onAuthenticationSucceeded(result);
        if (mDigitus.mCallback != null)
            mDigitus.mCallback.onDigitusAuthenticated(mDigitus);
        stop();
    }
}