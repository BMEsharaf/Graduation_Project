package first.assist.merda.project;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import conversation.ibmwatson.sonv.BTVService;
import conversation.ibmwatson.sonv.OnWatsonResponseFinish;
import manager.texttospeech.tts.TTsManager;
import manager.texttospeech.tts.VoiceType;
import manager.texttospeech.tts.Voices;
import manager.texttospeech.tts.google.TextToSpeechInitializer;
import manager.texttospeech.tts.google.TextToSpeechStartupListener;
import static android.content.ContentValues.TAG;
import static android.speech.SpeechRecognizer.ERROR_SERVER;
import static android.speech.tts.TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID;
import static android.speech.tts.TextToSpeech.QUEUE_ADD;


public class VoiceAssistService extends Service implements RecognitionListener,
        TextToSpeechStartupListener, OnWatsonResponseFinish {


    public static final String DEFAULT_UTTERANCE_VALUE = "speak";
    final static int START_LISTEN = 1, STOP_LISTEN = 0;
    final static String REQUEST_INSTALL = "request.install.action",
                        REQUEST_WAIT="request.wait.action",
            RECOGNITION_NOT_AVAILABLE = "request.recognition.notavailable",
            START_RECEIVING_SENSOR = "start.receiving.sensor",
            MAKE_CALL = "make.call",
            MAKE_EMAIL = "make.email",
            EXTRA_DESCRIPTION = "extra.description",
            REQUEST_PHONE_CALL = "request.phone.call";
    private static final String ASK_USER_UTTERANCE = "ask";
    private static final String DONT_LISTEN = "don't.listen";
    SpeechRecognizer mSpeechRecognizer;
    Intent recognizerIntent, backIntent, currentIntent;
    Handler mHandler;
    boolean start = false, ask = false;
    VoiceType currentType;
    TTsManager tTsManager;
    ArrayList<String> names = new ArrayList<>();
    BTVService btvService;
    ArrayList<String> phones = new ArrayList<>();
    @Override
    public void onCreate() {
        Log.i("Services", "At onCreate");
        btvService = new BTVService(this);
        backIntent = new Intent();
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
        Log.i("Services", "OnStartCommand");
        currentIntent = intent;

        if (intent.getStringExtra("Voice") != null) {
            String voiceName = intent.getStringExtra("Voice");
            if ("Mike".equals(voiceName)) {
                currentType = Voices.Mike;
            } else if ("Jesse".equals(voiceName)) {
                currentType = Voices.Jesse;
            } else if ("Suso".equals(voiceName)) {
                currentType = Voices.Suso;
            } else {
                currentType = Voices.Suso;
            }
            if (tTsManager == null) {
                tTsManager = new TTsManager(currentType, this, this);
            } else {
                tTsManager.setVoiceType(currentType);
            }

        }
        if (intent.getStringExtra("ContactCommand") != null) {
            //Failed to find this name
            tTsManager.speak("It seems that you don't have " +
                    "contact with name " +
                    intent.getStringExtra("ContactCommand"), DEFAULT_UTTERANCE_VALUE, QUEUE_ADD);
        } else if ((names = intent.getStringArrayListExtra("user names")) != null) {
                /*tTsManager.speak("You have more than one user with this name "
                        ,DONT_LISTEN);
                for(String name :names)
                tTsManager.speak(name,DONT_LISTEN);
                tTsManager.speak("Please choice one of them",ASK_USER_UTTERANCE);
                phones=intent.getStringArrayListExtra("phones");*/
            String quaring = "You have more than one user with this name " + names + "Please select one of them";
            tTsManager.speak(quaring, ASK_USER_UTTERANCE);
            phones = intent.getStringArrayListExtra("phones");
        } else if (tTsManager != null) {
            tTsManager.speak("Hi can i help you", DEFAULT_UTTERANCE_VALUE);
        }
        return START_REDELIVER_INTENT;
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
        Log.i(TAG, error + "");
        if(error== SpeechRecognizer.ERROR_NETWORK_TIMEOUT){
            Log.i(TAG,"ERROR_NETWORK_TIMEOUT");
            tTsManager.speak("Network time out error", DEFAULT_UTTERANCE_VALUE);
            mSpeechRecognizer.startListening(recognizerIntent);
        }else if(error== SpeechRecognizer.ERROR_NETWORK){
            Log.i(TAG,"ERROR_NETWORK");
            tTsManager.speak("Network error", "stop");
        }else if(error== SpeechRecognizer.ERROR_AUDIO){
            Log.i(TAG,"ERROR_AUDIO");
            tTsManager.speak("Audio error", DEFAULT_UTTERANCE_VALUE);
        }else if(error== SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS){
            Log.i(TAG,"ERROR_INSUFFICIENT_PERMISSIONS");
            tTsManager.speak("Please accept audio permission", "stop");
        }else if(error== SpeechRecognizer.ERROR_SPEECH_TIMEOUT){
            Log.i(TAG,"ERROR_SPEECH_TIMEOUT");
            tTsManager.speak("time out", DEFAULT_UTTERANCE_VALUE);
        }else if(error== SpeechRecognizer.ERROR_RECOGNIZER_BUSY){
            Log.i(TAG,"ERROR_RECOGNIZER_BUSY");
            tTsManager.speak("Busy", "stop");
        }else if(error== SpeechRecognizer.ERROR_NO_MATCH){
            Log.i(TAG,"ERROR_NO_MATCH");
            tTsManager.speak("sorry i missed what you call", DEFAULT_UTTERANCE_VALUE);
        } else if (error == ERROR_SERVER) {
            Log.i(TAG, "ERROR_SERVER");
            // speak("Please check network connection and start again","stop");
            tTsManager.speak("Please check network connection and start again", "stop");
        }
    }

    @Override
    public void onResults(Bundle results) {
        start = true ;
        String result =
                results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).get(0);
        Log.i("TAG", "Result : " + results);

        if ("stop".equals(result)) {
            tTsManager.speak("Good by", "stop");
            stopSelf();
        }
        if (ask) {
            ask = false;
            result = result.toLowerCase();
            backIntent.setAction(REQUEST_PHONE_CALL);
            if (result.contains("first")) {
                backIntent.putExtra("number", phones.get(0));
                LocalBroadcastManager.getInstance(this).sendBroadcast(backIntent);
                stopSelf();
            } else if (result.contains("second")) {
                backIntent.putExtra("number", phones.get(1));
                LocalBroadcastManager.getInstance(this).sendBroadcast(backIntent);
                stopSelf();
            } else if (result.contains("third")) {
                backIntent.putExtra("number", phones.get(2));
                LocalBroadcastManager.getInstance(this).sendBroadcast(backIntent);
                stopSelf();
            }
        }
        btvService.sendText(result);
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
        if (tTsManager != null) {
            tTsManager.speak("Hi can i help you", DEFAULT_UTTERANCE_VALUE);
        }
    }

    @Override
    public void onRequireLanguageData() {
        backIntent.setAction(REQUEST_INSTALL);
        LocalBroadcastManager.getInstance(this).sendBroadcast(backIntent);
    }

    @Override
    public void onWaitingForLanguageData() {

    }

    @Override
    public void onFailedToInit() {
        stopSelf();
    }

    @Override
    public void onSpeakDone(String utteranceId) {

        if (DEFAULT_UTTERANCE_VALUE.equals(utteranceId)) {
            mHandler.obtainMessage(START_LISTEN).sendToTarget();
        } else if ("stop".equals(utteranceId)) {
            stopSelf();
        } else if (ASK_USER_UTTERANCE.equals(utteranceId)) {
            ask = true;
            mHandler.obtainMessage(START_LISTEN).sendToTarget();
        }
    }

    @Override
    public void onReceivingSentence(String text) {
        if (text == null) {
            return;
        }
        if (!"I didn't understand. You can try rephrasing.".equals(text) || !"goodbye".equals(text))
            tTsManager.speak(text, DEFAULT_UTTERANCE_VALUE);
    }

    @Override
    public void onReceivingName(String name) {
        if (name != null) {
            Log.i("Name", name == null ? "null" : name);
            //  backIntent.setAction(MAKE_CALL);
            backIntent.setAction(MAKE_EMAIL);
            backIntent.putExtra("name", name);
            LocalBroadcastManager.getInstance(this).sendBroadcast(backIntent);
        }
    }

    @Override
    public void onReceivingEmail(String name) {
        if (name != null) {
            Log.i("Name", name);
            backIntent.setAction(MAKE_EMAIL);
            backIntent.putExtra("name", name);
            LocalBroadcastManager.getInstance(this).sendBroadcast(backIntent);
        }
    }

    @Override
    public void onReceivingPhone(String name) {
        if (name != null) {
            Log.i("Name", name);
            backIntent.setAction(MAKE_CALL);
            backIntent.putExtra("name", name);
            LocalBroadcastManager.getInstance(this).sendBroadcast(backIntent);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("TAG", "Service is destroyed");
        backIntent.setAction(START_RECEIVING_SENSOR);
        LocalBroadcastManager.getInstance(this).sendBroadcast(backIntent);
        mSpeechRecognizer.destroy();
        tTsManager.shutDownMicrosoftTTs();
    }
}