package first.assist.merda.project;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import static first.assist.merda.project.MyService.RECOGNITION_NOT_AVAILABLE;
import static first.assist.merda.project.MyService.REQUEST_INSTALL;
import static first.assist.merda.project.MyService.REQUEST_WAIT;


public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback
                                    ,TextToSpeechDialog.onDialogButtonListener {

    Button start , stop ;
    int VOICE_RECORD_REQUEST =1;
    ServiceBroadCastReceiver mBroadCast;
    boolean enable =false ;
    Intent intent ;
    TextToSpeechDialog dialog ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        registerBroadCastReceivers();
        checkAudioPermission();

        start = (Button) findViewById(R.id.btn);
        stop = (Button) findViewById(R.id.stop);
        dialog = new TextToSpeechDialog();



        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent = new Intent(getBaseContext(),MyService.class);
                if(enable)
                startService(intent);
            }
        });
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                stopService(intent);
            }
        });
    }

    private void checkAudioPermission() {
        if(!hasPermission(Manifest.permission.RECORD_AUDIO)) {
            requestPermission(Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET);
        }
    }

    private boolean hasPermission(String perm){
        if(ActivityCompat.checkSelfPermission(this, perm)== PackageManager.PERMISSION_GRANTED){
            enable =true ;
        }else{
            enable =false;
        }
        return enable;
    }
    private void requestPermission(String... perms){
        ActivityCompat.requestPermissions(this,perms,0);

    }

    private void registerBroadCastReceivers() {
        mBroadCast = new ServiceBroadCastReceiver() ;
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadCast, new IntentFilter(REQUEST_INSTALL));
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadCast, new IntentFilter(REQUEST_WAIT));
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadCast, new IntentFilter(RECOGNITION_NOT_AVAILABLE));
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
    public void onDestroy(){
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadCast);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults){
        if(requestCode==VOICE_RECORD_REQUEST&&grantResults[0]== PackageManager.PERMISSION_GRANTED){
            enable = true ;
        }
    }



    class ServiceBroadCastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(REQUEST_INSTALL)){
                dialog.setMessage(R.string.download_data);
                dialog.setTitle("Confirm");
                dialog.show(getSupportFragmentManager(),"Install");
            }else if(intent.getAction().equals(REQUEST_WAIT)){
                dialog.setMessage(R.string.Info);
                dialog.setTitle("Info");
                dialog.show(getSupportFragmentManager(),"Wait");
            }else if(intent.getAction().equals(RECOGNITION_NOT_AVAILABLE)){
                dialog.setMessage(R.string.Open_Network);
                dialog.setTitle("Recognition not available");
                dialog.show(getSupportFragmentManager(),"Open");
            }
        }
    }
}
