package commandmanager;

import android.content.Context;
import android.os.Bundle;


public class CommandManager {
    public final static String MAKE_CALL_COMMAND = "make.call.command";
    public final static String MAKE_EMAIL_COMMAND = "make.email.command";
    private final static String TAKE_IMAGE_COMMAND = "take.image.command";
    private String command;
    private ContactCommand2 contactCommand;
    private Context mContext;
    private CommandCallbacks mCallbacks;

    public CommandManager(Context context, CommandCallbacks mCallbacks) {
        mContext = context;
        this.mCallbacks = mCallbacks;
        contactCommand = new ContactCommand2(mContext, mCallbacks);
    }

    public ContactCommand2 getContactCommand() {
        return contactCommand;
    }

    public void setCallbacks(CommandCallbacks mCallbacks) {
        this.mCallbacks = mCallbacks;
    }

    public ClientCommandCallback getCallback() {
        return contactCommand;
    }

}
