package commandmanager;

import android.content.Context;
import android.os.Bundle;

/**
 * Created by User on 9/19/2017.
 */

public class CommandManager {
    public final static String MAKE_CALL_COMMAND = "make.call.command";
    public final static String MAKE_EMAIL_COMMAND = "make.email.command";
    public final static String TAKE_IMAGE_COMMAND = "take.image.command";
    private String command;
    private ContactCommand contactCommand;
    private Context mContext;
    private CommandCallbacks mCallbacks;

    public CommandManager(Context context, CommandCallbacks mCallbacks) {
        mContext = context;
        this.mCallbacks = mCallbacks;
        contactCommand = new ContactCommand(mContext, mCallbacks);
    }

    public void setCommandType(String command) {
        this.command = command;
    }

    public void executeCommand(String name) {
        switch (command) {
            case MAKE_CALL_COMMAND:
                contactCommand.setCommandData(name, MAKE_CALL_COMMAND);
                contactCommand.executeCommand();
                break;
            case MAKE_EMAIL_COMMAND:
                contactCommand.setCommandData(name, MAKE_EMAIL_COMMAND);
                contactCommand.executeCommand();
                break;
            case TAKE_IMAGE_COMMAND:
        }
    }

    public ContactCommand getContactCommand() {
        return contactCommand;
    }

    public void setCallbacks(CommandCallbacks mCallbacks) {
        this.mCallbacks = mCallbacks;
    }

    public ClientCommandCallback getCallback() {
        return contactCommand;
    }

}
