package first.assist.merda.project;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

import commandmanager.CommandCallbacks;
import commandmanager.CommandManager;
import commandmanager.ContactCommand;
import manager.texttospeech.tts.VoiceType;
import manager.texttospeech.tts.google.TextToSpeechDialog;
import manager.texttospeech.tts.google.TextToSpeechInitializer;

import static commandmanager.CommandManager.MAKE_CALL_COMMAND;
import static commandmanager.CommandManager.MAKE_EMAIL_COMMAND;
import static first.assist.merda.project.VoiceAssistService.EXTRA_DESCRIPTION;
import static first.assist.merda.project.VoiceAssistService.MAKE_CALL;
import static first.assist.merda.project.VoiceAssistService.MAKE_EMAIL;
import static first.assist.merda.project.VoiceAssistService.RECOGNITION_NOT_AVAILABLE;
import static first.assist.merda.project.VoiceAssistService.REQUEST_INSTALL;
import static first.assist.merda.project.VoiceAssistService.REQUEST_PHONE_CALL;
import static first.assist.merda.project.VoiceAssistService.REQUEST_WAIT;
import static first.assist.merda.project.VoiceAssistService.START_RECEIVING_SENSOR;


public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback
        , TextToSpeechDialog.onDialogButtonListener, OnActivateSuccessListener,
        CommandCallbacks, OnActivationDialogAcceptListener, OnVoiceTypeChangedListener {

    ActivationManager activator;
    CommandManager commandManager;
    Button start, stop;
    int VOICE_RECORD_REQUEST = 1;
    ServiceBroadCastReceiver mBroadCast;
    boolean enable = false;
    Intent serviceIntent;
    TextToSpeechDialog dialog;
    SettingDialog settingDialog;
    SharedPreferences setting;
    boolean shaking, pressing;
    VoiceType currentType;
    ContactCommand contactCommand;
    String voiceName = null;
    String name;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        serviceIntent = new Intent(getBaseContext(), VoiceAssistService.class);
        setting = getPreferences(MODE_PRIVATE);
        settingDialog = new SettingDialog();
        shaking = setting.getBoolean("Shaking", true);
        pressing = setting.getBoolean("Pressing", false);
        voiceName = setting.getString("voiceName", "Mike");
        serviceIntent.putExtra("Voice", voiceName);
        settingDialog.setActivationData(shaking, pressing);
        //Activate service when shaking
        activator = new ActivationManager(this, this);
        activator.setActivationType(shaking, pressing);
        //Register for activation listener "Accelerometer data listener"  activator.setActivationType(shaking,pressing);
        activator.activate();
        commandManager = new CommandManager(this, this);
        commandManager.setCallbacks(this);
        contactCommand = commandManager.getContactCommand();
        registerBroadCastReceivers();

        checkAudioPermission();
        start = (Button) findViewById(R.id.btn);
        stop = (Button) findViewById(R.id.stop);
        dialog = new TextToSpeechDialog();

    }

    private void checkAudioPermission() {
        if (!hasPermission(Manifest.permission.RECORD_AUDIO)) {
            requestPermission(Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET);
        }
    }

    private boolean hasPermission(String perm) {
        if (ActivityCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) {
            enable = true;
        } else {
            enable = false;
        }
        return enable;
    }

    private void requestPermission(String... perms) {
        ActivityCompat.requestPermissions(this, perms, 0);

    }

    private void registerBroadCastReceivers() {
        mBroadCast = new ServiceBroadCastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadCast, new IntentFilter(REQUEST_INSTALL));
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadCast, new IntentFilter(REQUEST_WAIT));
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadCast, new IntentFilter(RECOGNITION_NOT_AVAILABLE));
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadCast, new IntentFilter(START_RECEIVING_SENSOR));
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadCast, new IntentFilter(MAKE_CALL));
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadCast, new IntentFilter(REQUEST_PHONE_CALL));
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadCast, new IntentFilter(MAKE_EMAIL));

    }

    @Override
    public void onAccept(String title) {
        if ("Confirm".equals(title)) {
            TextToSpeechInitializer.installLanguageData(this);
        } else if ("Info".equals(title)) {
            //Wait
        }
    }

    @Override
    public void onCancel(String title) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == VOICE_RECORD_REQUEST && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enable = true;
        }
    }

    //User shaking the device
    @Override
    public void onActivatedService() {
        startService(serviceIntent);
    }

    @Override
    public void onCommandSuccess(Bundle data) {
        stopService(serviceIntent);
    }

    @Override
    public void onCommandWaitResponse(List<String> names, List<String> phoneNumbers, List<String> emails) {
        Log.i("Send", "Send command wait to service");
        serviceIntent.putStringArrayListExtra("user names", (ArrayList<String>) names);
        serviceIntent.putStringArrayListExtra("phones", (ArrayList<String>) phoneNumbers);
        startService(serviceIntent);
        serviceIntent.removeExtra("user names");
        serviceIntent.removeExtra("phone");
    }

    @Override
    public void onPositiveButtonClick(ArrayList<Integer> mSelectedItems) {
        if (mSelectedItems.size() == 2) {
            //Activate both shaking and pressing
            //Save setting
            setting.edit()
                    .putBoolean("Shaking", true)
                    .putBoolean("Pressing", true)
                    .apply();
            activator.setActivationType(true, true);
            activator.activate();
        } else if (mSelectedItems.size() == 1) {
            if (mSelectedItems.contains(0)) {
                setting.edit()
                        .putBoolean("Shaking", true)
                        .putBoolean("Pressing", false)
                        .apply();
                activator.deActivate();
                activator.setActivationType(true, false);
                activator.activate();
            } else {
                setting.edit()
                        .putBoolean("Shaking", false)
                        .putBoolean("Pressing", true)
                        .apply();
                activator.deActivate();
                activator.setActivationType(false, true);
                activator.activate();
            }
        } else {
            setting.edit()
                    .putBoolean("Shaking", false)
                    .putBoolean("Pressing", false)
                    .apply();
            activator.deActivate();
            activator.setActivationType(false, false);
            activator.activate();
        }
    }

    @Override
    public void onVoiceTypeChanged(String type) {
        setting.edit()
                .putString("voiceName", type)
                .apply();
        serviceIntent.putExtra("Voice", type);
        startService(serviceIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadCast);
        activator.deActivate();
    }

    @Override
    public void onResume() {
        super.onResume();
        //  activator.activate();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.setting_id:
                showSettingDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSettingDialog() {
        settingDialog.show(getFragmentManager(), "Setting");
    }

    @Override
    public void onCommandFailed(String name) {
        serviceIntent.putExtra("ContactCommand", name);
        startService(serviceIntent);
        serviceIntent.removeExtra("ContactCommand");
    }

    private String getName(String name) {
        if (name.equals("aslam") || name.equals("islam")) {
            name = "Eslam";
        } else if (name.equals("ibrahim") || name.equals("ebrahim") || name.equals("abrahim")) {
            name = "Ibrahim";
        } else if (name.equals("mohammed") || name.equals("mohammad") || name.equals("mohamed")
                || name.equals("mohammad") || name.equals("mohamad")) {
            name = "Mohamed";
        }
        return name;
    }

    class ServiceBroadCastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(REQUEST_INSTALL)) {
                dialog.setMessage(R.string.download_data);
                dialog.setTitle("Confirm");
                dialog.show(getSupportFragmentManager(), "Install");
            } else if (intent.getAction().equals(REQUEST_WAIT)) {
                dialog.setMessage(R.string.Info);
                dialog.setTitle("Info");
                dialog.show(getSupportFragmentManager(), "Wait");
            } else if (intent.getAction().equals(RECOGNITION_NOT_AVAILABLE)) {
                dialog.setMessage(R.string.Open_Network);
                dialog.setTitle("Recognition not available");
                dialog.show(getSupportFragmentManager(), "Open");
            } else if (intent.getAction().equals(START_RECEIVING_SENSOR)) {
                activator.activate();
            } else if (intent.getAction().equals(MAKE_CALL)) {
                name = getName(intent.getStringExtra("name").toLowerCase());
                Log.i("Call to ", "make call to" + name);
                commandManager.setCommandType(MAKE_CALL_COMMAND);
            } else if (intent.getAction().equals(MAKE_EMAIL)) {
                name = getName(intent.getStringExtra("name").toLowerCase());
                Log.i("Call to ", "make call to" + name);
                contactCommand.setCommandData(name, MAKE_EMAIL_COMMAND);
                contactCommand.executeCommand();
            } else if (intent.getAction().equals(EXTRA_DESCRIPTION)) {
                commandManager.getCallback().onSelectData(intent.getStringExtra("name"));
            } else if (intent.getAction().equals(REQUEST_PHONE_CALL)) {
                contactCommand.makePhoneCall(intent.getStringExtra("number"));
            }
        }
    }
}
