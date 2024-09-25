package ntnu.tdt4240.shoutout;

import static ntnu.tdt4240.shoutout.enums.GameLength.getGameLengthFromSpinner;
import static ntnu.tdt4240.shoutout.enums.GameMode.getGameModeFromSpinner;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import ntnu.tdt4240.shoutout.exceptions.DocumentAlreadyExistsException;
import ntnu.tdt4240.shoutout.exceptions.GameFullException;
import ntnu.tdt4240.shoutout.firestore.FireStoreHelper;
import ntnu.tdt4240.shoutout.firestore.interfaces.DocumentCallback;
import ntnu.tdt4240.shoutout.firestore.interfaces.OnCompleteCallback;
import ntnu.tdt4240.shoutout.models.GameModel;
import ntnu.tdt4240.shoutout.models.PlayerModel;

public class ChooseNameActivity extends AppCompatActivity {

    private EditText playerNameEditText;
    private EditText pinEditText;
    private Button continueButton;
    private String action;
    private Button backToMain;
    private FireStoreHelper fireStoreHelper;

    private Spinner gameModeSpinner, gameLengthSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_name);

        fireStoreHelper = FireStoreHelper.getInstance();

        playerNameEditText = findViewById(R.id.playerNameEditText);
        pinEditText = findViewById(R.id.pinEditText);
        continueButton = findViewById(R.id.continueButton);
        backToMain = findViewById(R.id.backToMain);

        initializeSpinners();

        Intent intent = getIntent();
        action = intent.getStringExtra("action");

        if (action.equals("create")) {
            pinEditText.setVisibility(View.GONE);
        } else if (action.equals("join")) {
            gameModeSpinner.setEnabled(false);
            gameLengthSpinner.setEnabled(false);
            gameModeSpinner.setVisibility(View.GONE);
            gameLengthSpinner.setVisibility(View.GONE);
        }

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                continueButton.setEnabled(false);

                String playerName = playerNameEditText.getText().toString().trim();
                String pin = pinEditText.getText().toString().trim();

                if (playerName.isEmpty()) {
                    playerNameEditText.setError("Name is required");
                    playerNameEditText.requestFocus();
                    continueButton.setEnabled(true);
                    return;
                }

                if (action.equals("join") && pin.isEmpty()) {
                    pinEditText.setError("PIN is required");
                    pinEditText.requestFocus();
                    continueButton.setEnabled(true);
                    return;
                }

                PlayerModel player = new PlayerModel(playerName);

                if (action.equals("create")) {
                    createAndJoinGame(player);
                } else if (action.equals("join")) {
                    joinGame(pin, player);
                }
            }
        });

        backToMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent lobbyLeave = new Intent(ChooseNameActivity.this, MainActivity.class);
                startActivity(lobbyLeave);
            }
        });


    }

    private void initializeSpinners() {
        gameModeSpinner = findViewById(R.id.gameModeSpinner);
        gameLengthSpinner = findViewById(R.id.gameLengthSpinner);

        ArrayAdapter<CharSequence> gameModeAdapter = ArrayAdapter.createFromResource(this,
                R.array.game_modes, android.R.layout.simple_spinner_item);
        gameModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gameModeSpinner.setAdapter(gameModeAdapter);

        ArrayAdapter<CharSequence> gameLengthAdapter = ArrayAdapter.createFromResource(this,
                R.array.game_lengths, android.R.layout.simple_spinner_item);
        gameLengthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gameLengthSpinner.setAdapter(gameLengthAdapter);

        gameModeSpinner.setSelection(0); // Set default game mode to Casual (index 0)
        gameLengthSpinner.setSelection(1); // Set default game length to Medium (index 1)

    }

    private void createAndJoinGame(PlayerModel player) {
        GameModel game = new GameModel(getGameModeFromSpinner(gameModeSpinner.getSelectedItemPosition()));
        fireStoreHelper.createGame(game, getGameLengthFromSpinner(gameLengthSpinner.getSelectedItemPosition()), new DocumentCallback<GameModel>() {
            @Override
            public void onDocumentReady(GameModel gameModel) {
                fireStoreHelper.joinGame(gameModel.getId(), player, new OnCompleteCallback() {
                    @Override
                    public void onSuccess() {
                        startLobbyActivity(gameModel, player);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        e.printStackTrace();
                        Toast.makeText(ChooseNameActivity.this, "Failed to join the game", Toast.LENGTH_SHORT).show();
                    }
                });


                backToMain.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent lobbyLeave = new Intent(ChooseNameActivity.this, MainActivity.class);
                        startActivity(lobbyLeave);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ChooseNameActivity.this, "Failed to create the game", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void joinGame(String pin, PlayerModel player) {
        fireStoreHelper.getGameByPin(pin, new DocumentCallback<GameModel>() {
            @Override
            public void onDocumentReady(GameModel game) {
                fireStoreHelper.joinGame(game.getId(), player, new OnCompleteCallback() {
                    @Override
                    public void onSuccess() {
                        startLobbyActivity(game, player);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (e instanceof DocumentAlreadyExistsException) {
                            PlayerModel playerModel = ((DocumentAlreadyExistsException) e).getDocument().toObject(PlayerModel.class);
                            if (game.getGameStarted() && playerModel != null && !playerModel.getIsReady()) {
                                startGameActivity(game, playerModel);
                            } else {
                                Toast.makeText(ChooseNameActivity.this, "This player already exists. Choose a different name", Toast.LENGTH_SHORT).show();
                                continueButton.setEnabled(true);
                            }
                        } else if (e instanceof GameFullException){
                            Toast.makeText(ChooseNameActivity.this, "Game is full", Toast.LENGTH_SHORT).show();
                            continueButton.setEnabled(true);
                        } else {
                            Toast.makeText(ChooseNameActivity.this, "Failed to join the game", Toast.LENGTH_SHORT).show();
                            continueButton.setEnabled(true);
                        }
                    }
                });
            }
            @Override
            public void onFailure(Exception e) {
                e.printStackTrace();
                Toast.makeText(ChooseNameActivity.this, "Game not found", Toast.LENGTH_SHORT).show();
                continueButton.setEnabled(true);
            }
        });
    }

    private void startGameActivity(GameModel game, PlayerModel playerModel) {
        Intent gameIntent = new Intent(ChooseNameActivity.this, GameActivity.class);
        gameIntent.putExtra("player_id", playerModel.getId());
        gameIntent.putExtra("game_model", game);
        startActivity(gameIntent);
    }

    private void startLobbyActivity(GameModel game, PlayerModel player) {
        Intent lobbyIntent = new Intent(ChooseNameActivity.this, LobbyActivity.class);
        lobbyIntent.putExtra("player_id", player.getId());
        lobbyIntent.putExtra("game_model", game);
        startActivity(lobbyIntent);
    }
}
