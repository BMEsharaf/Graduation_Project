package manager.texttospeech.tts.google;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;

import first.assist.merda.project.LanguageDataInstallBroadcastReceiver;

import static android.speech.tts.TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID;

/**
 * Created by User on 9/13/2017.
 */

public class TextToSpeechInitializer implements TextToSpeech.OnInitListener {

    private final static String TAG = "TEXT_TO_SPEECH_TAG";
    private Context context;
    private TextToSpeechStartupListener callback;
    private TextToSpeech tts;
    private boolean isSpeaking = false, CanSpeak = false;
    private Locale locale;
    private String text, utterance;
    private boolean dataFlag = false;
    public TextToSpeechInitializer(Context context, TextToSpeechStartupListener callback) {
        this.context = context;
        this.callback = callback;
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

    public static void installLanguageData(Context mContext) {
        // set waiting for the download
        LanguageDataInstallBroadcastReceiver.setWaiting(mContext, true);
        Intent installIntent = new Intent();
        installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
        mContext.startActivity(installIntent);
    }

    public void createTextToSpeech(final Locale locale) {
        this.locale = locale;
        tts = new TextToSpeech(context, this);
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

    public void shutDownTts() {
        tts.stop();
        tts.shutdown();
        CanSpeak = false;
    }

    public void stop() {
        CanSpeak = false;
        tts.stop();
    }

    public void speak(String text, String utteranceId) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            speakGreeter21(text, utteranceId);
        } else {
            speakUnder20(text, utteranceId);
        }
    }

    public void speak(String text, String utteranceId, int queue) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            speakGreeter21(text, utteranceId, queue);
        } else {
            speakUnder20(text, utteranceId, queue);
        }
    }

    @SuppressWarnings("deprecation")
    private void speakUnder20(String text, String utteranceId) {
        final HashMap<String, String> map = new HashMap<>();
        map.put(KEY_PARAM_UTTERANCE_ID, utteranceId);

        int status = tts.speak(text, TextToSpeech.QUEUE_FLUSH, map);
        if (status != TextToSpeech.SUCCESS) {
            dataFlag = true;
            this.text = text;
            this.utterance = utteranceId;
            tts = new TextToSpeech(context, this);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void speakGreeter21(String text, String utteranceId) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
    }

    @SuppressWarnings("deprecation")
    private void speakUnder20(String text, String utteranceId, int queue) {
        final HashMap<String, String> map = new HashMap<>();
        map.put(KEY_PARAM_UTTERANCE_ID, utteranceId);
        int status = tts.speak(text, queue, map);
        if (status != TextToSpeech.SUCCESS) {
            dataFlag = true;
            this.text = text;
            this.utterance = utteranceId;
            tts = new TextToSpeech(context, this);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void speakGreeter21(String text, String utteranceId, int queue) {
        tts.speak(text, queue, null, utteranceId);
    }

    public boolean isSpeaking() {
        return isSpeaking;
    }


    @Override
    public void onInit(int status) {

        if (status == TextToSpeech.SUCCESS) {
            setTextToSpeechSettings(locale);
            if (dataFlag == true) {
                dataFlag = false;
                speak(text, utterance);
            }
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    isSpeaking = true;
                }

                @Override
                public void onDone(String utteranceId) {
                    callback.onSpeakDone(utteranceId);
                    isSpeaking = false;
                }

                @Override
                public void onError(String utteranceId) {
                    isSpeaking = false;
                }
            });
            callback.onSuccessfulInit(tts);
        } else {
            Log.e("TAG", "error creating text to speech");
            tts.shutdown();
            callback.onFailedToInit();
        }

    }
}

