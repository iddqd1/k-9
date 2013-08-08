package pl.wp.cloud.authenticate;


import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class WpAuthenticatorService extends Service {

    @Override
    public IBinder onBind(Intent intent) {

        WpAuthenticator authenticator = new WpAuthenticator(this);
        return authenticator.getIBinder();
    }

}
