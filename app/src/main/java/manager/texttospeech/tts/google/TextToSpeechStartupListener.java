package manager.texttospeech.tts.google;

import android.speech.tts.TextToSpeech;

import manager.texttospeech.tts.VoiceType;

public interface TextToSpeechStartupListener
{
    /**
     * tts is initialized and ready for use
     *
     * @param tts
     * the fully initialized object
     */
    public void onSuccessfulInit(TextToSpeech tts);


    public void onRequireLanguageData();
    /**
     * The app has already requested language data, and is waiting for it.
     */
    public void onWaitingForLanguageData();
    /**
     * initialization failed and can never complete.
     */
    public void onFailedToInit();

    public void onSpeakDone(String utteranceId);

}