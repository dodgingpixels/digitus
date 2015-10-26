package com.afollestad.digitus.google;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.digitus.Digitus;
import com.afollestad.digitus.DigitusCallback;
import com.afollestad.digitus.R;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.internal.MDTintHelper;

/**
 * A dialog which uses fingerprint APIs to authenticate the user, and falls back to password
 * authentication if fingerprint is not available.
 */
@SuppressWarnings("ResourceType")
public class FingerprintAuthenticationDialogFragment extends DialogFragment
        implements TextView.OnEditorActionListener, FingerprintUiHelper.Callback {

    private View mFingerprintContent;
    private View mBackupContent;
    private EditText mPassword;
    private CheckBox mUseFingerprintFutureCheckBox;
    private TextView mPasswordDescriptionTextView;
    private TextView mNewFingerprintEnrolledTextView;

    private Stage mStage = Stage.FINGERPRINT;

    private FingerprintManager.CryptoObject mCryptoObject;
    private FingerprintUiHelper mFingerprintUiHelper;
    private DigitusCallback mCallback;

    FingerprintUiHelper.FingerprintUiHelperBuilder mFingerprintUiHelperBuilder;
    InputMethodManager mInputMethodManager;

    public FingerprintAuthenticationDialogFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.sign_in)
                .customView(R.layout.fingerprint_dialog_container, false)
                .positiveText(android.R.string.cancel)
                .negativeText(R.string.use_password)
                .autoDismiss(false)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                        materialDialog.dismiss();
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                        if (mStage == Stage.FINGERPRINT) {
                            goToBackup(materialDialog);
                        } else {
                            verifyPassword();
                        }
                    }
                })
                .build();

        final View v = dialog.getCustomView();
        assert v != null;
        mFingerprintContent = v.findViewById(R.id.fingerprint_container);
        mBackupContent = v.findViewById(R.id.backup_container);
        mPassword = (EditText) v.findViewById(R.id.password);
        mPassword.setOnEditorActionListener(this);
        mPasswordDescriptionTextView = (TextView) v.findViewById(R.id.password_description);
        mUseFingerprintFutureCheckBox = (CheckBox)
                v.findViewById(R.id.use_fingerprint_in_future_check);
        mNewFingerprintEnrolledTextView = (TextView)
                v.findViewById(R.id.new_fingerprint_enrolled_description);
        if (mFingerprintUiHelperBuilder != null) {
            mFingerprintUiHelper = mFingerprintUiHelperBuilder.build(
                    (ImageView) v.findViewById(R.id.fingerprint_icon),
                    (TextView) v.findViewById(R.id.fingerprint_status), this);
        }
        updateStage(dialog);

        // If fingerprint authentication is not available, switch immediately to the backup (password) screen.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.USE_FINGERPRINT)
                        != PackageManager.PERMISSION_GRANTED ||
                mFingerprintUiHelper == null ||
                !mFingerprintUiHelper.isFingerprintAuthAvailable()) {
            goToBackup(dialog);
        }

        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mStage == Stage.FINGERPRINT && mFingerprintUiHelper != null)
            mFingerprintUiHelper.startListening(mCryptoObject);
    }

    public void setStage(Stage stage) {
        mStage = stage;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mFingerprintUiHelper != null)
            mFingerprintUiHelper.stopListening();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallback = (DigitusCallback) activity;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mFingerprintUiHelperBuilder = new FingerprintUiHelper.FingerprintUiHelperBuilder(
                    (FingerprintManager) activity.getSystemService(Context.FINGERPRINT_SERVICE));
        }
        mInputMethodManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    /**
     * Sets the crypto object to be passed in when authenticating with fingerprint.
     */
    public void setCryptoObject(FingerprintManager.CryptoObject cryptoObject) {
        mCryptoObject = cryptoObject;
    }

    /**
     * Switches to backup (password) screen. This either can happen when fingerprint is not
     * available or the user chooses to use the password authentication method by pressing the
     * button. This can also happen when the user had too many fingerprint attempts.
     */
    private void goToBackup(MaterialDialog dialog) {
        mStage = Stage.PASSWORD;
        updateStage(dialog);
        mPassword.requestFocus();

        // Show the keyboard.
        mPassword.postDelayed(mShowKeyboardRunnable, 500);

        // Fingerprint is not used anymore. Stop listening for it.
        if (mFingerprintUiHelper != null)
            mFingerprintUiHelper.stopListening();
    }

    private void verifyPassword() {
//        TODO MaterialDialog dialog = (MaterialDialog) getDialog();
//        dialog.getActionButton(DialogAction.POSITIVE).setEnabled(false);
//        dialog.getActionButton(DialogAction.NEGATIVE).setEnabled(false);
//        mCallback.onDigitusValidatePassword(Digitus.get(), mPassword.getText().toString());
    }

    public void notifyPasswordValidation(boolean valid) {
        final MaterialDialog dialog = (MaterialDialog) getDialog();
        final View positive = dialog.getActionButton(DialogAction.POSITIVE);
        final View negative = dialog.getActionButton(DialogAction.NEGATIVE);
        positive.setEnabled(true);
        negative.setEnabled(true);

        if (valid) {
            if (mStage == Stage.NEW_FINGERPRINT_ENROLLED &&
                    mUseFingerprintFutureCheckBox.isChecked()) {
                // Re-create the key so that fingerprints including new ones are validated.
                Digitus.get().recreateKey();
                mStage = Stage.FINGERPRINT;
            }
            mPassword.setText("");
            mCallback.onDigitusAuthenticated(Digitus.get());
            dismiss();
        } else {
            mPasswordDescriptionTextView.setText(R.string.password_not_recognized);
            final int red = ContextCompat.getColor(getActivity(), R.color.material_red_500);
            MDTintHelper.setTint(mPassword, red);
            ((TextView) positive).setTextColor(red);
            ((TextView) negative).setTextColor(red);
        }
    }

    private final Runnable mShowKeyboardRunnable = new Runnable() {
        @Override
        public void run() {
            mInputMethodManager.showSoftInput(mPassword, 0);
        }
    };

    private void updateStage(@Nullable MaterialDialog dialog) {
        if (dialog == null)
            dialog = (MaterialDialog) getDialog();
        if (dialog == null) return;
        switch (mStage) {
            case FINGERPRINT:
                dialog.setActionButton(DialogAction.POSITIVE, android.R.string.cancel);
                dialog.setActionButton(DialogAction.NEGATIVE, R.string.use_password);
                mFingerprintContent.setVisibility(View.VISIBLE);
                mBackupContent.setVisibility(View.GONE);
                break;
            case NEW_FINGERPRINT_ENROLLED:
                // Intentional fall through
            case PASSWORD:
                dialog.setActionButton(DialogAction.POSITIVE, android.R.string.cancel);
                dialog.setActionButton(DialogAction.NEGATIVE, android.R.string.ok);
                mFingerprintContent.setVisibility(View.GONE);
                mBackupContent.setVisibility(View.VISIBLE);
                if (mStage == Stage.NEW_FINGERPRINT_ENROLLED) {
                    mPasswordDescriptionTextView.setVisibility(View.GONE);
                    mNewFingerprintEnrolledTextView.setVisibility(View.VISIBLE);
                    mUseFingerprintFutureCheckBox.setVisibility(View.VISIBLE);
                }
                break;
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_GO) {
            verifyPassword();
            return true;
        }
        return false;
    }

    @Override
    public void onAuthenticated() {
        // Callback from FingerprintUiHelper. Let the activity know that authentication was
        // successful.
        mCallback.onDigitusAuthenticated(Digitus.get());
        dismiss();
    }

    @Override
    public void onError() {
        goToBackup(null);
    }

    /**
     * Enumeration to indicate which authentication method the user is trying to authenticate with.
     */
    public enum Stage {
        FINGERPRINT,
        NEW_FINGERPRINT_ENROLLED,
        PASSWORD
    }
}