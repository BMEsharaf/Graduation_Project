package first.assist.merda.project;

import android.speech.tts.TextToSpeech;

public interface TextToSpeechStartupListener
{
    /**
     * tts is initialized and ready for use
     *
     * @param tts
     * the fully initialized object
     */
    public void onSuccessfulInit(TextToSpeech tts);
    /**
     * language data is required, to install call
     * {@link TextToSpeechInitializer#installLanguageData()}
     */
    public void onRequireLanguageData();
    /**
     * The app has already requested language data, and is waiting for it.
     */
    public void onWaitingForLanguageData();
    /**
     * initialization failed and can never complete.
     */
    public void onFailedToInit();
}