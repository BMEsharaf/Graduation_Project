package first.assist.merda.interpret;

import android.content.Intent;

/**
 * Created by User on 9/15/2017.
 */

public interface InterpretListener {
    public void onReceivingCommand(Intent intent);
    public void onReceivingText(String  text);
}
