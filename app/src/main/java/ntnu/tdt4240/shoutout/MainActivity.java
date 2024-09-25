package ntnu.tdt4240.shoutout;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.firebase.FirebaseApp;

import ntnu.tdt4240.shoutout.firestore.FireStoreHelper;

public class MainActivity extends AppCompatActivity {

    private FireStoreHelper fireStoreHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button createGameButton = findViewById(R.id.create_game_button);
        Button joinGameButton = findViewById(R.id.join_game_button);
        Button howToPlayButton = findViewById(R.id.how_to_play_button);

        createGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ChooseNameActivity.class).putExtra("action", "create"));
            }
        });

        joinGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ChooseNameActivity.class).putExtra("action", "join"));
            }
        });

        howToPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, HowToPlayActivity.class));
            }
        });
    }
}