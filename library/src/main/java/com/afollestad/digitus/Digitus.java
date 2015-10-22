package com.afollestad.digitus;

import android.Manifest;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.provider.Settings;
import android.security.keystore.KeyProperties;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.afollestad.digitus.google.FingerprintAuthenticationDialogFragment;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;

/**
 * @author Aidan Follestad (afollestad)
 */
public class Digitus extends DigitusBase {

    private static Digitus mInstance;

    private int mRequestCode;
    private FingerprintAuthenticationDialogFragment mFragment;
    private boolean mIsReady;

    public static Digitus get() {
        return mInstance;
    }

    public boolean isReady() {
        return mInstance != null && mInstance.mIsReady;
    }

    public boolean notifyPasswordValidation(boolean valid) {
        if (mFragment == null) return false;
        mFragment.notifyPasswordValidation(valid);
        return true;
    }

    public boolean openSecuritySettings() {
        if (mCallback == null) return false;
        ((Activity) mCallback).startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
        return true;
    }

    private <T extends Activity & DigitusCallback> Digitus(T context, String keyName) {
        super(context, keyName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mKeyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            mFingerprintManager = context.getSystemService(FingerprintManager.class);
            try {
                mKeyStore = KeyStore.getInstance("AndroidKeyStore");
            } catch (KeyStoreException e) {
                throw new RuntimeException("Failed to get an instance of KeyStore", e);
            }
            try {
                mKeyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
                throw new RuntimeException("Failed to get an instance of KeyGenerator", e);
            }
            try {
                mCipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                        + KeyProperties.BLOCK_MODE_CBC + "/"
                        + KeyProperties.ENCRYPTION_PADDING_PKCS7);
            } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
                throw new RuntimeException("Failed to get an instance of Cipher", e);
            }
        }
    }

    public static <T extends Activity & DigitusCallback> Digitus init(T context, String keyName, int requestCode) {
        mInstance = new Digitus(context, keyName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mInstance.mRequestCode = requestCode;
            int granted = ContextCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT);
            if (granted != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.USE_FINGERPRINT}, requestCode);
            else finishInit();
        } else finishInit();
        return mInstance;
    }

    public static void deinit() {
        if (mInstance == null) return;
        if (mInstance.mFragment != null) mInstance.mFragment.dismiss();
        mInstance.mRequestCode = 0;
        mInstance.deinitBase();
        mInstance = null;
    }

    private static void finishInit() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (mInstance.isFingerprintRegistered()) {
                mInstance.mIsReady = true;
                mInstance.recreateKey();
                mInstance.mCallback.onDigitusReady(mInstance);
            } else {
                mInstance.mCallback.onDigitusRegistrationNeeded(mInstance);
            }
        } else {
            mInstance.mIsReady = true;
            mInstance.mCallback.onDigitusReady(mInstance);
        }
    }

    public void handleResult(int requestCode, String[] permissions, int[] state) {
        if (requestCode == mRequestCode && permissions != null &&
                permissions[0].equals(Manifest.permission.USE_FINGERPRINT)) {
            if (state[0] == PackageManager.PERMISSION_GRANTED) {
                finishInit();
            } else {
                mInstance.mCallback.onDigitusError(this, new PermissionDeniedError());
            }
        }
    }

    public void beginAuthentication() {
        if (!mIsReady)
            throw new IllegalStateException("beginAuthentication() cannot be called until Digitus is ready.");
        final Activity context = (Activity) mCallback;
        mFragment = new FingerprintAuthenticationDialogFragment();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mFragment.setStage(FingerprintAuthenticationDialogFragment.Stage.PASSWORD);
        } else if (initCipher()) {
            mFragment.setCryptoObject(new FingerprintManager.CryptoObject(mCipher));
            mFragment.setStage(FingerprintAuthenticationDialogFragment.Stage.FINGERPRINT);
        } else {
            mFragment.setCryptoObject(new FingerprintManager.CryptoObject(mCipher));
            mFragment.setStage(FingerprintAuthenticationDialogFragment.Stage.NEW_FINGERPRINT_ENROLLED);
        }

        mFragment.show(context.getFragmentManager(), "[fingerprint-tag]");
    }

    public boolean isFingerprintRegistered() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false;
        int granted = ContextCompat.checkSelfPermission((Activity) mInstance.mCallback, Manifest.permission.USE_FINGERPRINT);
        if (granted != PackageManager.PERMISSION_GRANTED) return false;
        //noinspection ResourceType
        return mKeyguardManager.isKeyguardSecure() && mFingerprintManager.hasEnrolledFingerprints();
    }
}
