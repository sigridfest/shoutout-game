package ntnu.tdt4240.shoutout;

import android.app.Application;

import com.google.firebase.FirebaseApp;

import ntnu.tdt4240.shoutout.models.GameModel;
import ntnu.tdt4240.shoutout.models.PlayerModel;

public class ShoutOutApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseApp.initializeApp(this);
    }
}
