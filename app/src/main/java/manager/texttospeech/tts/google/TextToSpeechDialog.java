package manager.texttospeech.tts.google;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;

/**
 * Created by User on 9/13/2017.
 */

public class TextToSpeechDialog extends DialogFragment {

    onDialogButtonListener listener;
    private int id ;
    private String title ;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try{
            listener = (onDialogButtonListener) context;
        }catch (ClassCastException er){
            er.printStackTrace();
        }
    }

    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(id)
                .setTitle(title)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listener.onAccept(title);
                    }
                })
                .setNegativeButton("test", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listener.onCancel(title);
                        getDialog().cancel();
                    }
                });
        return builder.create();
    }

    public void setMessage(int id){
       this.id = id ;
    }

    public void setTitle(String title){
        this.title = title ;
    }

    public interface onDialogButtonListener {
        public void onAccept(String title);

        public void onCancel(String title);
    }
}
