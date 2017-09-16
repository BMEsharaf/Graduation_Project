package first.assist.merda.project;

import android.content.Context;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

/**
 * Created by User on 9/13/2017.
 */

public class TextToSpeechInitializer {

    private Context context;
    private TextToSpeechStartupListener callback;
    private TextToSpeech tts;
    private final static String TAG = "TEXT_TO_SPEECH_TAG";

    TextToSpeechInitializer(Context context, TextToSpeechStartupListener callback) {
        this.context = context;
        this.callback = callback;
    }

    public void createTextToSpeech(final Locale locale) {
        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    setTextToSpeechSettings(locale);
                } else {
                    Log.e("TAG", "error creating text to speech");
                    callback.onFailedToInit();
                }
            }
        });
    }

    private void setTextToSpeechSettings(final Locale locale) {
        Locale defaultOrPassedIn = locale;
        if (locale == null) {
            defaultOrPassedIn = Locale.getDefault();
        }

        switch (tts.isLanguageAvailable(defaultOrPassedIn)) {
            case TextToSpeech.LANG_AVAILABLE:
            case TextToSpeech.LANG_COUNTRY_AVAILABLE:
            case TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE:
                Log.d(TAG, "SUPPORTED");
                tts.setLanguage(locale);
                callback.onSuccessfulInit(tts);
                break;
            case TextToSpeech.LANG_MISSING_DATA:
                Log.d(TAG, "MISSING_DATA");
                // check if waiting, by checking
                // a shared preference
                if (LanguageDataInstallBroadcastReceiver
                        .isWaiting(context)) {
                    Log.d(TAG, "waiting for data...");
                    callback.onWaitingForLanguageData();
                } else {
                    Log.d(TAG, "require data...");
                    callback.onRequireLanguageData();
                }
                break;
            case TextToSpeech.LANG_NOT_SUPPORTED:
                Log.d(TAG, "NOT SUPPORTED");
                callback.onFailedToInit();
                break;
        }
    }

    public static String getLanguageAvailableDescription(TextToSpeech tts) {
        StringBuilder sb = new StringBuilder();
        for (Locale loc : Locale.getAvailableLocales()) {
            int availableCheck = tts.isLanguageAvailable(loc);
            sb.append(loc.toString()).append(" ");
            switch (availableCheck) {
                case TextToSpeech.LANG_AVAILABLE:
                    break;
                case TextToSpeech.LANG_COUNTRY_AVAILABLE:
                    sb.append("COUNTRY_AVAILABLE");
                    break;
                case TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE:
                    sb.append("COUNTRY_VAR_AVAILABLE");
                    break;
                case TextToSpeech.LANG_MISSING_DATA:
                    sb.append("MISSING_DATA");
                    break;
                case TextToSpeech.LANG_NOT_SUPPORTED:
                    sb.append("NOT_SUPPORTED");
                    break;
            }
            sb.append("\n");
        }
        return sb.toString();
    }
    public static void installLanguageData(Context mContext)
    {
          // set waiting for the download
        LanguageDataInstallBroadcastReceiver.setWaiting(mContext, true);
        Intent installIntent = new Intent();
        installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
        mContext.startActivity(installIntent);
    }
}

