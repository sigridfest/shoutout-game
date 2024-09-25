package ntnu.tdt4240.shoutout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import ntnu.tdt4240.shoutout.firestore.FireStoreHelper;
import ntnu.tdt4240.shoutout.firestore.interfaces.CollectionCallback;
import ntnu.tdt4240.shoutout.firestore.interfaces.CollectionEventListener;
import ntnu.tdt4240.shoutout.firestore.interfaces.DocumentCallback;
import ntnu.tdt4240.shoutout.firestore.interfaces.DocumentEventListener;
import ntnu.tdt4240.shoutout.firestore.interfaces.OnCompleteCallback;
import ntnu.tdt4240.shoutout.models.GameModel;
import ntnu.tdt4240.shoutout.models.PlayerModel;

public class LobbyActivity extends AppCompatActivity  {

    private TextView gamePinTextView;
    private TextView readyOverview;
    private RecyclerView playersRecyclerView;
    private Button startGameButton;

    private PlayerAdapter playerAdapter;
    private List<PlayerModel> playerList = new ArrayList<>();
    private Button leaveLobbyButton;
    private Button readyButton;
    private FireStoreHelper fireStoreHelper;
    private GameModel gameModel;
    private String playerId;
    private PlayerModel playerModel;
    private boolean playerLeft = false;
    private boolean isStartingGame = false;

    private final int PLAYER_LOWER_LIMIT = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);
        gamePinTextView = findViewById(R.id.gamePinTextView);
        playersRecyclerView = findViewById(R.id.playersRecyclerView);
        startGameButton = findViewById(R.id.startGameButton);
        leaveLobbyButton = findViewById(R.id.leaveLobby);
        readyButton = findViewById(R.id.readyButton);
        readyOverview = findViewById(R.id.readyOverview);

        fireStoreHelper = FireStoreHelper.getInstance();

        gameModel = (GameModel) getIntent().getSerializableExtra("game_model");
        playerId = (String) getIntent().getSerializableExtra("player_id");

        Log.i("LobbyStart","Player id in lobby: " + playerId);
        Log.i("LobbyStart","Game model in lobby: " + gameModel.toString());

        if (gameModel != null) {
            gamePinTextView.setText("Game PIN: " + gameModel.getPin());
            setupPlayersRecyclerView();
            fireStoreHelper.fetchPlayersForGame(gameModel.getId(), new CollectionCallback<PlayerModel>() {
                @Override
                public void onCollectionReady(List<PlayerModel> objects) {
                    updatePlayerList(objects);
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(LobbyActivity.this, "Failed to fetch players", Toast.LENGTH_SHORT).show();
                }
            });
            fireStoreHelper.subscribeToPlayerUpdates(gameModel.getId(), new CollectionEventListener<PlayerModel>() {
                @Override
                public void onEvent(List<PlayerModel> snapshot, Exception e) {
                    if (e != null || snapshot == null) {
                        Toast.makeText(LobbyActivity.this, "Failed to receive player update", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updatePlayerList(snapshot);
                }
            });
            fireStoreHelper.subscribeToGameUpdates(gameModel.getId(), new DocumentEventListener<GameModel>() {
                @Override
                public void onEvent(GameModel snapshot, Exception e) {
                    if (e != null || snapshot == null) {
                        Toast.makeText(LobbyActivity.this, "Failed to receive game update", Toast.LENGTH_SHORT).show();
                    }
                    if (snapshot.getGameStarted()) {
                        startGame();
                    }
                }
            });
        }

        startGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean allPlayersReady = true;
                for (PlayerModel player : playerList) {
                    allPlayersReady = allPlayersReady && player.getIsReady();
                }
                if (playerList.size() >= PLAYER_LOWER_LIMIT && allPlayersReady) {
                    fireStoreHelper.startGame(gameModel.getId(), new OnCompleteCallback() {
                        @Override
                        public void onSuccess() {
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(LobbyActivity.this, "Failed to start game", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                else {
                    startGameButton.setEnabled(false);
                    startGameButton.setBackgroundColor(Color.GRAY);
                }
            }
        });

        leaveLobbyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                leaveLobby();
            }
        });


        readyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (playerModel != null) {
                    fireStoreHelper.setPlayerIsReady(gameModel.getId(), playerId, !playerModel.getIsReady());
                }
            }
        });
    }

    private void setupPlayersRecyclerView() {
        playersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        playerAdapter = new PlayerAdapter(playerList);
        playersRecyclerView.setAdapter(playerAdapter);
    }

    private void updatePlayerList(List<PlayerModel> newList) {
        int counter = 0;
        playerList.clear();
        playerList.addAll(newList);
        playerAdapter.notifyDataSetChanged();
        boolean allPlayersReady = true;

        for (PlayerModel player : playerList) {
            if (player.getId() != null && player.getId().equals(playerId)) {
                playerModel = player;
                if (playerModel.getIsReady()) {
                    readyButton.setBackgroundColor(Color.GREEN);
                } else {
                    readyButton.setBackgroundColor(Color.GRAY);
                }
            }
            if (player.getIsReady()){
                counter++;
            } else {
                 allPlayersReady = false;
            }
        }
        readyOverview.setText(counter + " of " + playerList.size() + " players are ready!");

        if (playerList.size() >= PLAYER_LOWER_LIMIT && allPlayersReady) {
            startGameButton.setEnabled(true);
            startGameButton.setBackgroundColor(Color.GREEN);
        }
        else {
            startGameButton.setEnabled(false);
            startGameButton.setBackgroundColor(Color.GRAY);
        }
    }

    @Override
    public void onBackPressed() {
        if (!gameModel.getGameStarted()) {
            leaveLobby();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!playerLeft && !isStartingGame && !gameModel.getGameStarted()) {
            removePlayerFromGame();
        }
    }

    protected void onResume() {
        super.onResume();
        if (playerLeft && playerModel != null && gameModel != null) {
            joinLobby();
        }
    }


    private void leaveLobby() {
        fireStoreHelper.leaveGame(playerModel.getId(), gameModel.getId(), new OnCompleteCallback() {
            @Override
            public void onSuccess() {
                Intent intent = new Intent(LobbyActivity.this, MainActivity.class);
                startActivity(intent);
            }

            @Override
            public void onFailure(Exception e) {

            }
        });
    }

    private void joinLobby() {
        fireStoreHelper.joinGame(gameModel.getId(), playerModel, new OnCompleteCallback() {
            @Override
            public void onSuccess() {
                playerLeft = false;
            }

            @Override
            public void onFailure(Exception e) {

            }
        });

    }

    private void removePlayerFromGame() {
        fireStoreHelper.leaveGame(playerModel.getId(), gameModel.getId(), new OnCompleteCallback() {
            @Override
            public void onSuccess() {
                playerLeft = true;
            }

            @Override
            public void onFailure(Exception e) {
            }
        });
    }

    private void unsubscribeFromUpdates() {
        fireStoreHelper.unsubscribeFromPlayerUpdates();
        fireStoreHelper.unsubscribeFromGameUpdates();
    }

    private void startGame() {
        Toast.makeText(this, "Starting game", Toast.LENGTH_SHORT).show();
        unsubscribeFromUpdates();
        isStartingGame = true;
        Intent intent = new Intent(LobbyActivity.this, GameActivity.class);
        intent.putExtra("game_model", gameModel);
        intent.putExtra("player_id", playerId);
        startActivity(intent);
    }
}
