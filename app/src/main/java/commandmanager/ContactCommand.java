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
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import static commandmanager.CommandManager.MAKE_CALL_COMMAND;


public class ContactCommand implements LoaderManager.LoaderCallbacks<Cursor>,
        ClientCommandCallback, ActivityCompat.OnRequestPermissionsResultCallback {

    private final static int REQUEST_PHONE_CALL = 5;
    private final List<String> names = new ArrayList<>();
    private final List<String> phoneNumbers = new ArrayList<>();
    private final List<String> emails = new ArrayList<>();
    private String name, operation;
    private Context mContext;
    private CommandCallbacks mCallbacks;
    private LoaderManager loaderManager;
    private boolean enable = false;
    private Intent callIntent = new Intent(Intent.ACTION_CALL);

    ContactCommand(Context context, CommandCallbacks mCallbacks) {
        mContext = context;
        this.mCallbacks = mCallbacks;
        loaderManager = ((Activity) mContext).getLoaderManager();
    }

    public void setCommandData(String name, String command) {
        this.name = name;
        operation = command;
    }

    public void executeCommand() {
        Log.i("here", "execute call");
        if (loaderManager.getLoader(0) != null) {
            loaderManager.restartLoader(0, null, this);
        } else {
            loaderManager.initLoader(0, null, this);
        }
    }

    private CursorLoader getPhoneNumberLoader(String name) {

        String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " like'%" + name + "%'";

        String[] projection = new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME};

        return new CursorLoader(mContext,
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection,
                selection, null, null);
    }

    private CursorLoader getEmailAddressLoader(String name) {
/*
        String selection = ContactsContract.CommonDataKinds.Email.DISPLAY_NAME + " like'%" + name + "%'";

        String[] projection = new String[]{ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Email.DATA,
                ContactsContract.CommonDataKinds.Email.CONTACT_ID};

        return new CursorLoader(mContext,
                ContactsContract.CommonDataKinds.Email.CONTENT_URI, projection,
                null, null, null);
                */
        String selection = ContactsContract.Contacts.DISPLAY_NAME + " like'%" + name + "%'";
        String[] PROJECTION = new String[]{
                ContactsContract.RawContacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Email.DATA,
                ContactsContract.CommonDataKinds.Photo.CONTACT_ID};
        String order = "CASE WHEN "
                + ContactsContract.Contacts.DISPLAY_NAME
                + " NOT LIKE '%@%' THEN 1 ELSE 2 END, "
                + ContactsContract.Contacts.DISPLAY_NAME
                + ", "
                + ContactsContract.CommonDataKinds.Email.DATA
                + " COLLATE NOCASE";
        return new CursorLoader(mContext, ContactsContract.CommonDataKinds.Email.CONTENT_URI, PROJECTION,
                selection, null, order);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        Log.i("OnCreate", "At onCreateLoader");
        if (MAKE_CALL_COMMAND.equals(operation)) {
            return getPhoneNumberLoader(name);
        } else {
            return getEmailAddressLoader(name);
        }
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        phoneNumbers.clear();
        names.clear();
        emails.clear();
        String phoneNumber, email;
        final int name_index = data.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
        Log.i("finish", "onLoadFinished: " + data.getCount() + " operation " + operation);
        if (MAKE_CALL_COMMAND.equals(operation)) {
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
        } else {
            //      final int email_address = data.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS);
            //  final int emailIndex = data.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA);
            if (data.getCount() > 1) {
                Log.i("Number", "You have more than contact with same name");
                while (data.moveToNext()) {
                    email = data.getString(3);
                    emails.add(email);
                    name = data.getString(name_index);
                    Log.i("Name : ", name);
                    Log.i("Email", "Email address " + email);
                }
                mCallbacks.onCommandWaitResponse(names, null, emails);
            } else {
                if (data.moveToFirst()) {
                    makeEmailMessage(data.getString(2));
                } else {
                    mCallbacks.onCommandFailed(name);
                }
            }
        }
        loaderManager.destroyLoader(0);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    public void onSelectData(String result) {
        if (MAKE_CALL_COMMAND.equals(operation)) {
            makePhoneCall(result);
        } else {
            makeEmailMessage(result);
        }

    }

    private void makeEmailMessage(String result) {
        Log.i("Email : ", result);
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.putExtra(Intent.EXTRA_EMAIL, "emailaddress@emailaddress.com");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Subject");
        intent.putExtra(Intent.EXTRA_TEXT, "Hi Ibrahim how are you");
        intent.setType("text/plain");
        mContext.startActivity(Intent.createChooser(intent, "Send Email"));
        mCallbacks.onCommandSuccess(null);
    }

    public void makePhoneCall(String result) {

        callIntent.setData(Uri.parse("tel:" + result));
        if (!hasPermission(Manifest.permission.CALL_PHONE)) {
            requestPermission(Manifest.permission.CALL_PHONE);
        } else {
            mContext.startActivity(callIntent);
        }
        mCallbacks.onCommandSuccess(null);
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

}
