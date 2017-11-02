package commandmanager;

import android.os.Bundle;

import java.util.List;


public interface CommandCallbacks {
    public void onCommandSuccess(Bundle data);

    public void onCommandWaitResponse(List<String> names, List<String> phoneNumbers, List<String> emails);

    void onCommandFailed(String name);
}
