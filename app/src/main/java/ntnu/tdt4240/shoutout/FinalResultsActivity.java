package ntnu.tdt4240.shoutout;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.Collections;
import java.util.List;

import ntnu.tdt4240.shoutout.firestore.FireStoreHelper;
import ntnu.tdt4240.shoutout.firestore.interfaces.CollectionCallback;
import ntnu.tdt4240.shoutout.models.PlayerModel;

public class FinalResultsActivity extends AppCompatActivity {

    private String gameId;
    private List<PlayerModel> players;
    private FireStoreHelper fsHelper;

    private LinearLayout resultGrid;
    private Typeface typeface;
    private Button menuButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_final_results);

        gameId = getIntent().getStringExtra("gameId");

        fsHelper = FireStoreHelper.getInstance();

        resultGrid = findViewById(R.id.final_result_grid);
        menuButton = findViewById(R.id.menu_button);

        fsHelper.fetchPlayersForGame(gameId, new CollectionCallback<PlayerModel>() {
            @Override
            public void onCollectionReady(List<PlayerModel> objects) {
                players = objects;
                setResults();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(FinalResultsActivity.this, "Failed to fetch players", Toast.LENGTH_SHORT).show();
            }
        });

        menuButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });
    }

    private void setResults() {
        Collections.sort(players, new PlayerModel.ScoreComparator());
        for (PlayerModel player : players) {
            String resultText = player.getId() + ": " + player.getScore();
            resultGrid.setPadding(10, 10, 10, 10);
            resultGrid.setClipToPadding(false);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();

            Button button = new Button(this);
            button.setText(resultText);
            button.setTextSize(25);
            typeface = Typeface.createFromAsset(getAssets(),"changaone_regular.ttf");
            button.setTypeface(typeface);
            button.setBackgroundColor(Color.parseColor("#2980B9"));
            button.setTextColor(Color.WHITE);
            params.setMargins(10, 10, 10, 10);
            params.width = GridLayout.LayoutParams.MATCH_PARENT;
            params.height = GridLayout.LayoutParams.MATCH_PARENT;
            button.setLayoutParams(params);
            resultGrid.addView(button);
        }
    }
}