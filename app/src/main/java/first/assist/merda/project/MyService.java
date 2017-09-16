package first.assist.merda.project;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;

import static android.content.ContentValues.TAG;


public class MyService extends Service implements RecognitionListener,TextToSpeechStartupListener {

    SpeechRecognizer mSpeechRecognizer ;
    final static int START_LISTEN = 1, STOP_LISTEN = 0;
    Intent recognizerIntent;
    Handler mHandler;
    boolean start = false ;
    TextToSpeechInitializer initializer ;
    TextToSpeech tts =null ;
    final static String REQUEST_INSTALL = "request.install.action",
                        REQUEST_WAIT="request.wait.action",
                        RECOGNITION_NOT_AVAILABLE="request.recognition.notavailable";
    @Override
    public void onCreate() {
        Log.i("TAG", "At onCreate");
        initializer = new TextToSpeechInitializer(this,this);
        initializer.createTextToSpeech(Locale.ENGLISH);
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizerIntent =
                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,3000);
        checkSpeechRecognitionAvailable();
        mSpeechRecognizer.setRecognitionListener(this);
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == START_LISTEN) {
                    Log.i(TAG,"start listen");
                    mSpeechRecognizer.startListening(recognizerIntent);
                } else if (msg.what == STOP_LISTEN) {
                    mSpeechRecognizer.cancel();
                }
            }
        };
    }

    private void checkSpeechRecognitionAvailable() {
        if(!SpeechRecognizer.isRecognitionAvailable(this)){
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(RECOGNITION_NOT_AVAILABLE));
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }


    @Nullable

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        Log.i("TAG", "onReadyForSpeech");
    }

    @Override
    public void onBeginningOfSpeech() {

        Log.i("TAG", "onBeginningSpeech");
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        Log.i("TAG", "onRms");
    }

    @Override
    public void onBufferReceived(byte[] buffer) {

    }

    @Override
    public void onEndOfSpeech() {
        Log.i("TAG", "end speech Listen ready for speech");
    }

    @Override
    public void onError(int error) {
        Log.i(TAG, Thread.currentThread().getName());
        mSpeechRecognizer.cancel();
        if(error== SpeechRecognizer.ERROR_NETWORK_TIMEOUT){
            Log.i(TAG,"ERROR_NETWORK_TIMEOUT");
            speak("Network time out error");
            mSpeechRecognizer.startListening(recognizerIntent);
        }else if(error== SpeechRecognizer.ERROR_NETWORK){
            Log.i(TAG,"ERROR_NETWORK");
            speak("Network error");
            mSpeechRecognizer.startListening(recognizerIntent);
        }else if(error== SpeechRecognizer.ERROR_AUDIO){
            Log.i(TAG,"ERROR_AUDIO");
            speak("Audio error");
            mSpeechRecognizer.startListening(recognizerIntent);
        }else if(error== SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS){
            Log.i(TAG,"ERROR_INSUFFICIENT_PERMISSIONS");
            mSpeechRecognizer.startListening(recognizerIntent);
        }else if(error== SpeechRecognizer.ERROR_SPEECH_TIMEOUT){
            Log.i(TAG,"ERROR_SPEECH_TIMEOUT");
            speak("time out");
            mSpeechRecognizer.startListening(recognizerIntent);
        }else if(error== SpeechRecognizer.ERROR_RECOGNIZER_BUSY){
            Log.i(TAG,"ERROR_RECOGNIZER_BUSY");
            speak("Busy");
        }else if(error== SpeechRecognizer.ERROR_NO_MATCH){
            Log.i(TAG,"ERROR_NO_MATCH");
            speak("sorry i missed what you call");
        }
    }

    @Override
    public void onResults(Bundle results) {
        start = true ;
        final String result =
                results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).get(0);
        Log.i("TAG", "Result : " + result);
        //Make class for interpret
        if ("stop".equals(result)) {
            speak("Good by");
            stopSelf();
        } else if("make call".equals(result)){
            speak("Who you want to call");
        } else{
            speak(result);
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {

    }

    @Override
    public void onEvent(int eventType, Bundle params) {

    }
    //TextToSpeech events
    @Override
    public void onSuccessfulInit(TextToSpeech mTTs) {
        this.tts = mTTs ;
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {

            }

            @Override
            public void onDone(String utteranceId) {
                mHandler.obtainMessage(START_LISTEN).sendToTarget();
            }

            @Override
            public void onError(String utteranceId) {

            }
        });
        speak("Hi how can i help you");
    }

    @Override
    public void onRequireLanguageData() {
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(REQUEST_INSTALL));
    }

    @Override
    public void onWaitingForLanguageData() {

    }

    @Override
    public void onFailedToInit() {
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("TAG", "Service is destroyed");
        mSpeechRecognizer.destroy();
        if(tts!=null){
            Log.i(TAG,"Here stop tts");
            tts.stop();
            tts.shutdown();
        }
    }
    private void speak(String text){
        HashMap<String, String> map = new HashMap<>();
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "UniqueID");
        if(tts!=null)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, map);
    }
}
