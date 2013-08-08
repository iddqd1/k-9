package pl.wp.cloud.service;


import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import pl.wp.cloud.authenticate.AccountGeneral;
import pl.wp.cloud.service.helper.Transfer;

import java.util.ArrayList;

public class AutoSynchronizationService extends Service implements AutoSynchronizationEventListener {

    private static final String TAG = AutoSynchronizationService.class.getSimpleName();

    public static final String ACTION_RESCAN_IMAGES =
            "pl.wp.cloud.files.autoupload.ACTION_RESCAN_IMAGES";

    public static final String EXTRA_NEW_ONLY = "new_only";

    private HandlerThread serviceThread;
    private Handler serviceHandler;

    private static final int MSG_ON_CHANGE = 1;

    private static final Uri IMAGES_CONTENT_URIS[] = {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Images.Media.INTERNAL_CONTENT_URI
    };

    private ArrayList<MediaObserver> observers =
            new ArrayList<MediaObserver>(IMAGES_CONTENT_URIS.length);

    private ConnectivityManager connectivityManager;

    private SharedPreferences.OnSharedPreferenceChangeListener prefsChangeListener;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
        Log.i(TAG, "Starting AutoSynchronizationService");

        Preferences prefs = Preferences.getPreferences(AutoSynchronizationService.this);
        //for (Account account : prefs.getAvailableAccounts()) {
            //Log.i(TAG, "Account transport uri: "+ account.getTransportUri());
            //Account account = new Account("tomek@wp.pl", AccountGeneral.ACCOUNT_TYPE);
            //String password = "password";
            //AccountManager accountManager = AccountManager.get(getApplicationContext());
            //accountManager.addAccountExplicitly(account,password,null);

        //}

        serviceThread = new HandlerThread("AutoUploadThread");
        serviceThread.start();
        serviceHandler = new Handler(serviceThread.getLooper()) {

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_ON_CHANGE:
                        Runnable scanTask = (Runnable) msg.obj;
                        scanTask.run();
                        break;

                    default:
                        super.handleMessage(msg);
                        break;
                }
            }

        };


        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        registerContentObservers();
        //rescanMedia(null);
    }

    @Override
    public void onAutoSynchronizationEventReceived() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    private class MediaObserver extends ContentObserver {
        private final Uri uri;
        private Runnable scanRunnable;

        public MediaObserver(Uri uri, Handler handler) {
            super(handler);
            this.uri = uri;
            this.scanRunnable = new Runnable() {
                @Override
                public void run() {
                    rescanMedia(MediaObserver.this.uri);
                }
            };
        }

        @Override
        public void onChange(boolean selfChange) {
            if (K9.DEBUG) {
                Log.d(TAG," onChange");
            }
            super.onChange(selfChange);
            synchronized (this) {
                if (! serviceHandler.hasMessages(MSG_ON_CHANGE, scanRunnable)) {
                    Log.i(TAG, "Starting auto upload scan of " + uri);
                    Message msg = serviceHandler.obtainMessage(
                            MSG_ON_CHANGE, scanRunnable);
                    serviceHandler.sendMessageDelayed(msg, 1000);
                    // Delay: if there are pics taken in same second,
                    // we want them all be in database already.
                } else {
                    Log.i(TAG, "Scan already scheduled for " + uri);
                }
            }
        }
    }


    private void rescanMedia(Uri uri) {

         String[] mProjection = {MediaStore.Images.Media._ID,
                                MediaStore.Images.Media.DATA,
                                MediaStore.Images.Media.SIZE};

         Cursor mCursor = getContentResolver().query(uri,
                               mProjection,
                               null,
                               null,
                               MediaStore.Images.Media.DEFAULT_SORT_ORDER);

        Log.i(TAG,"mCursor: "+ mCursor);
        mCursor.moveToFirst();
        while (mCursor!=null && mCursor.moveToNext()) {
            String file = mCursor.getString(mCursor.getColumnIndex(MediaStore.Images.Media.DATA));
            int size = mCursor.getInt(mCursor.getColumnIndex(MediaStore.Images.Media.SIZE));
            Log.i(TAG, file);

            Transfer.sendFileToServer( "http://192.168.1.4:8888/", file);
        }
        mCursor.close();
        Log.i(TAG,"mCursor : "+ mCursor +" closed.");
        // TODO This needs work, should be much more lightweight.
        // But it's 3 AM, we're releasing tomorrow, so have to stick with it now.
        //MediaImportUtils.importImageBuckets(this);
        //if (uri.toString().contains("images")) {
            //rescanImages(uri);
        //}
    }

    private void registerContentObservers() {
        for (Uri uri : IMAGES_CONTENT_URIS) {
            MediaObserver observer = new MediaObserver(uri, serviceHandler);
            observers.add(observer);
        }

        ContentResolver resolver = getContentResolver();
        for (MediaObserver o : observers) {
            resolver.registerContentObserver(o.uri, true, o);
            Log.d(TAG, "Auto Upload now enabled for " + o.uri);
        }
    }
}
