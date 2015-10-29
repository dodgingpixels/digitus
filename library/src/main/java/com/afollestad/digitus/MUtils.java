package com.afollestad.digitus;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;

/**
 * @author Aidan Follestad (afollestad)
 */
@TargetApi(Build.VERSION_CODES.M)
class MUtils {

    private MUtils() {
    }

    public static boolean isFingerprintRegistered(Digitus digitus) {
        if (!isFingerprintAuthAvailable(digitus)) return false;
        //noinspection ResourceType
        return digitus.mKeyguardManager.isKeyguardSecure() && digitus.mFingerprintManager.hasEnrolledFingerprints();
    }

    public static boolean isFingerprintAuthAvailable(Digitus digitus) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false;
        int granted = ContextCompat.checkSelfPermission(digitus.mContext, Manifest.permission.USE_FINGERPRINT);
        if (granted != PackageManager.PERMISSION_GRANTED) return false;
        //noinspection ResourceType
        return digitus.mFingerprintManager.isHardwareDetected() && digitus.mFingerprintManager.hasEnrolledFingerprints();
    }
}
