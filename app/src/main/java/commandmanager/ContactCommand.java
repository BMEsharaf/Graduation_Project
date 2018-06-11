package commandmanager;

import android.Manifest;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import command.email.send.GMailSender;

import static commandmanager.CommandManager.MAKE_CALL_COMMAND;


public class ContactCommand implements LoaderManager.LoaderCallbacks<Cursor>,
        ClientCommandCallback, ActivityCompat.OnRequestPermissionsResultCallback {

    private final static int REQUEST_PHONE_CALL = 5;
    private static final int CALL_ID = 0;
    private static final int EMAIL_ID = 1;
    private final List<String> names = new ArrayList<>();
    private final List<String> phoneNumbers = new ArrayList<>();
    private final List<String> emails = new ArrayList<>();
    private String name, operation, subject, content;
    private Context mContext;
    private CommandCallbacks mCallbacks;
    private LoaderManager loaderManager;
    private boolean enable = false;
    private Intent callIntent = new Intent(Intent.ACTION_CALL);
    private GMailSender sender = new GMailSender();
    private String user_email, password;

    ContactCommand(Context context, CommandCallbacks mCallbacks) {
        mContext = context;
        this.mCallbacks = mCallbacks;
        loaderManager = ((Activity) mContext).getLoaderManager();
    }

    public String getUser_email() {
        return user_email;
    }

    public void setUser_email(String user_email) {
        this.user_email = user_email;
        sender.setEmail(user_email);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
        sender.setPassword(password);
    }


    public void setCommandData(String command, String... name) {

        this.name = name[0];
        if (name.length > 1) {
            this.subject = name[1];
            this.content = name[2];
        }
        operation = command;
    }

    public void executeCallCommand(String name) {
        Log.i("here", "execute call");
        this.name = name;
        if (loaderManager.getLoader(CALL_ID) != null) {
            loaderManager.restartLoader(CALL_ID, null, this);
        } else {
            loaderManager.initLoader(CALL_ID, null, this);
        }
    }

    public void executeEmailCommand(String name, String subject, String content) {
        this.name = name;
        this.subject = subject;
        this.content = content;
        Log.i("here", "execute email");
        if (loaderManager.getLoader(EMAIL_ID) != null) {
            loaderManager.restartLoader(EMAIL_ID, null, this);
        } else {
            loaderManager.initLoader(EMAIL_ID, null, this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        Log.i("OnCreate", "At onCreateLoader");
        if (id == CALL_ID) {
            return getPhoneNumberLoader(name);
        } else if (id == EMAIL_ID) {
            return getEmailAddressLoader(name);
        }
        return null;
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        phoneNumbers.clear();
        names.clear();
        emails.clear();
        String phoneNumber, email;
        int id = loader.getId();
        Log.d("Debug", "onLoadFinished: here ");
        final int name_index = data.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);

        if (id == CALL_ID) {
            if (data.getCount() > 1) {
                while (data.moveToNext()) {
                    phoneNumber = data.getString(0);
                    phoneNumbers.add(phoneNumber);
                    name = data.getString(name_index);
                    names.add(name);
                    Log.i("Name : ", name);
                    Log.i("Phone", "Phone number equal " + phoneNumber);
                }
                mCallbacks.onCommandWaitResponse(names, phoneNumbers, null);
            } else {

                if (data.moveToFirst()) {
                    Log.i("call", "call to one person" + name);
                    makePhoneCall(data.getString(0));
                } else {
                    mCallbacks.onCommandFailed(name);
                }
            }
        } else if (id == EMAIL_ID) {

            if (data.getCount() > 1) {
                Log.i("Number", "You have more than contact with same name");
                while (data.moveToNext()) {
                    email = data.getString(2);
                    emails.add(email);
                    name = data.getString(name_index);
                    names.add(name);
                    Log.i("Name : ", name);
                    Log.i("Email", "Email address " + email);
                }
                mCallbacks.onCommandWaitResponse(names, null, emails);
            } else {
                if (data.moveToFirst()) {
                    makeEmailMessage(data.getString(2), subject, content);
                } else {
                    mCallbacks.onCommandFailed(name);
                }
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    public void onSelectData(String result) {
        if (MAKE_CALL_COMMAND.equals(operation)) {
            makePhoneCall(result);
        } else {
            makeEmailMessage(result, subject, content);
        }

    }

    public void makeEmailMessage(String email, String subject, String content) {
        Log.i("Email : ", email);
        sender.setEmail(this.user_email);
        sender.setPassword(password);
        new MyAsyncClass().execute(subject, content, user_email, email);
        mCallbacks.onEmailSuccess(null);
        loaderManager.destroyLoader(EMAIL_ID);
    }

    public void makePhoneCall(String result) {

        Uri uri = Uri.parse("tel:")
                .buildUpon()
                .appendPath(result)
                .build();
        callIntent.setData(uri);
        if (!hasPermission(Manifest.permission.CALL_PHONE)) {
            requestPermission(Manifest.permission.CALL_PHONE);
        } else {
            mContext.startActivity(callIntent);
        }
        mCallbacks.onCommandSuccess(null);

        loaderManager.destroyLoader(CALL_ID);

    }

    private boolean hasPermission(String perm) {
        if (ActivityCompat.checkSelfPermission(mContext, perm) == PackageManager.PERMISSION_GRANTED) {
            enable = true;
        } else {
            enable = false;
        }
        return enable;
    }

    private void requestPermission(String... perms) {
        ActivityCompat.requestPermissions((Activity) mContext, perms, 0);

    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PHONE_CALL && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enable = true;
            mContext.startActivity(callIntent);
        }
    }

    private CursorLoader getPhoneNumberLoader(String name) {

        String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " like'" + name + "%'";

        String[] projection = new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME};

        return new CursorLoader(mContext,
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection,
                selection, null, null);
    }

    private CursorLoader getEmailAddressLoader(String name) {

        String selection = ContactsContract.Contacts.DISPLAY_NAME + " like'" + name + "%'";
        String[] PROJECTION = new String[]{
                ContactsContract.RawContacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Photo.CONTACT_ID};
        String order = "CASE WHEN "
                + ContactsContract.Contacts.DISPLAY_NAME
                + " NOT LIKE '%@%' THEN 1 ELSE 2 END, "
                + ContactsContract.Contacts.DISPLAY_NAME
                + ", "
                + ContactsContract.CommonDataKinds.Email.DATA
                + " COLLATE NOCASE";
        Log.d("debug", "getEmailAddressLoader: ");
        return new CursorLoader(mContext, ContactsContract.CommonDataKinds.Email.CONTENT_URI, PROJECTION,
                selection, null, null);
    }

    private class MyAsyncClass extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... mApi) {
            try {
                // Add subject, Body, your mail Id, and receiver mail Id.
                sender.sendMail(mApi[0], mApi[1], user_email, mApi[3]);

            } catch (Exception ex) {
                Log.i("Error", "in mail");
                mCallbacks.onUserNamePasswordFailed();
            }
            return null;
        }
    }
}