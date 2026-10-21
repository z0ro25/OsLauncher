package com.ezla.oslauncher.beautylauncher.utils;//package com.ezt.argame.Utils;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ezla.oslauncher.beautylauncher.BuildConfig;
import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.ConsentForm;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.FormError;
import com.google.android.ump.UserMessagingPlatform;

public class GDPRRequestable {
    public ConsentInformation consentInformation;
    public static ConsentForm consentForm;
    private Activity context;
    static GDPRRequestable gdprRequestable;
    public static String YOUR_TEST_DEVICE_ID = "";
    public static Boolean concentStatus = false;

    public GDPRRequestable(Activity context) {
        this.context = context;
    }

    public static GDPRRequestable getGdprRequestable(Activity activity) {
        if (gdprRequestable == null) {
            return gdprRequestable = new GDPRRequestable(activity);
        } else return gdprRequestable;
    }

    public interface RequestGDPRCompleted {
        void onRequestGDPRCompleted(FormError formError);
    }

    private RequestGDPRCompleted onRequestGDPRCompleted;

    public void setOnRequestGDPRCompleted(RequestGDPRCompleted onRequestGDPRCompleted) {
        this.onRequestGDPRCompleted = onRequestGDPRCompleted;
    }

    public void requestGDPR() {
        ConsentDebugSettings.Builder consentDebugSettingsBuilder = new ConsentDebugSettings
                .Builder(context);
        if (BuildConfig.DEBUG) {
            consentDebugSettingsBuilder
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                    .addTestDeviceHashedId(YOUR_TEST_DEVICE_ID);
        }

        ConsentDebugSettings consentDebugSettings = consentDebugSettingsBuilder.build();

        ConsentRequestParameters params = new ConsentRequestParameters
                .Builder()
                .setConsentDebugSettings(consentDebugSettings)
                .setTagForUnderAgeOfConsent(false)
                .build();

        consentInformation = UserMessagingPlatform.getConsentInformation(context);
        consentInformation.requestConsentInfoUpdate(
                context,
                params,
                new ConsentInformation.OnConsentInfoUpdateSuccessListener() {
                    @Override
                    public void onConsentInfoUpdateSuccess() {
                        if (consentInformation.isConsentFormAvailable()) {
                            loadForm();
                        } else {
                            onRequestGDPRCompleted.onRequestGDPRCompleted(null);
                            concentStatus = true;
                        }
                    }
                },
                new ConsentInformation.OnConsentInfoUpdateFailureListener() {
                    @Override
                    public void onConsentInfoUpdateFailure(@NonNull FormError formError) {
                        onRequestGDPRCompleted.onRequestGDPRCompleted(formError);
                        concentStatus = false;
                    }
                }
        );
    }

    private void loadForm() {
        UserMessagingPlatform.loadConsentForm(context, consentForm -> {
            GDPRRequestable.consentForm = consentForm;
            if (consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.REQUIRED) {
                GDPRRequestable.consentForm.show(context, new ConsentForm.OnConsentFormDismissedListener() {
                    @Override
                    public void onConsentFormDismissed(@Nullable FormError formError) {
                        if (consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.OBTAINED) {
                            onRequestGDPRCompleted.onRequestGDPRCompleted(null);
                            concentStatus = true;
                        }
                        //  loadForm();
                    }
                });
            } else if (consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.OBTAINED) {
                onRequestGDPRCompleted.onRequestGDPRCompleted(null);
                concentStatus = true;
            } else {
                onRequestGDPRCompleted.onRequestGDPRCompleted(null);
                concentStatus = true;
            }
        }, new UserMessagingPlatform.OnConsentFormLoadFailureListener() {
            @Override
            public void onConsentFormLoadFailure(@NonNull FormError formError) {
                onRequestGDPRCompleted.onRequestGDPRCompleted(formError);
                concentStatus = false;
            }
        });
    }
}

