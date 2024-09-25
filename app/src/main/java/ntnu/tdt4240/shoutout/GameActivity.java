package ntnu.tdt4240.shoutout;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import ntnu.tdt4240.shoutout.gameState.GameState;
import ntnu.tdt4240.shoutout.firestore.FireStoreHelper;
import ntnu.tdt4240.shoutout.firestore.interfaces.CollectionCallback;
import ntnu.tdt4240.shoutout.firestore.interfaces.CollectionEventListener;
import ntnu.tdt4240.shoutout.firestore.interfaces.DocumentEventListener;
import ntnu.tdt4240.shoutout.firestore.interfaces.OnCompleteCallback;
import ntnu.tdt4240.shoutout.gameState.QuestionState;
import ntnu.tdt4240.shoutout.gameState.ResultsState;
import ntnu.tdt4240.shoutout.gameState.WaitingState;
import ntnu.tdt4240.shoutout.models.AnswerModel;
import ntnu.tdt4240.shoutout.models.GameModel;
import ntnu.tdt4240.shoutout.models.PlayerModel;
import ntnu.tdt4240.shoutout.models.QuestionModel;
import ntnu.tdt4240.shoutout.models.ResultModel;

public class GameActivity extends AppCompatActivity {
    public LinearLayout questionLayout;
    public LinearLayout waitingLayout;
    public LinearLayout resultsLayout;

    private TextView superShoutOutTextView;
    private TextView shoutoutTextView;
    private FireStoreHelper fsHelper;
    private TextView questionTextView;
    private LinearLayout buttonsLayout;
    private TextView categoryTextView;
    private LinearLayout resultGrid;
    private GameModel gameModel;
    private String playerId;
    private PlayerModel currentPlayer;
    private List<PlayerModel> players;
    private List<QuestionModel> questions;
    private QuestionModel currentQuestion;
    private List<AnswerModel> currentAnswers;
    private QuestionState questionState;
    private WaitingState waitingState;
    private ResultsState resultsState;
    private GameState currentState;
    private Typeface typeface;
    private Button nextQuestionButton;
    private TextView timerTextView;
    private TextView pinTextView;
    private CountDownTimer countDownTimer;
    private final long COUNTDOWN_MILLIS = 20 * 1000; // 20 seconds
    private long countdownMillis = 20 * 1000; // 20 seconds
    private boolean superShoutOut = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questions);

        fsHelper = FireStoreHelper.getInstance();

        questionLayout = findViewById(R.id.question_layout);
        waitingLayout = findViewById(R.id.waiting_layout);
        resultsLayout = findViewById(R.id.results_layout);

        nextQuestionButton = findViewById(R.id.next_question_button);

        timerTextView = findViewById(R.id.timer_text_view);

        // Initialize state instances
        questionState = new QuestionState(this);
        waitingState = new WaitingState(this);
        resultsState = new ResultsState(this);

        // Set the initial state
        setState(waitingState);

        nextQuestionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fsHelper.nextQuestion(gameModel.getId());
            }
        });

        // Get game model and current player from the previous activity
        gameModel = (GameModel) getIntent().getSerializableExtra("game_model");
        playerId = (String) getIntent().getSerializableExtra("player_id");

        String pinText = "PIN: " + gameModel.getPin();
        pinTextView = findViewById(R.id.pin_text_view);
        pinTextView.setText(pinText);

        currentAnswers = new ArrayList<>();

        // Initialize UI elements
        categoryTextView = findViewById(R.id.category_text_view);
        questionTextView = findViewById(R.id.question_text_view);
        shoutoutTextView = findViewById(R.id.shoutout_text);
        superShoutOutTextView = findViewById(R.id.super_shoutout_text);
        superShoutOutTextView.setVisibility(View.GONE);

        // Find the GridLayout view by its ID
        buttonsLayout = findViewById(R.id.buttons_grid);
        resultGrid = findViewById(R.id.result_grid);

        initializeValuesFromFirestore();
    }

    private synchronized void setState(GameState newState) {
        // We don't want to move to waiting state if we're in the result state
        if (currentState == resultsState && newState == waitingState) {
            return;
        }
        if (currentState != null) {
            currentState.exitState();
        }
        currentState = newState;
        currentState.enterState();
    }

    private void initializeValuesFromFirestore() {
        subscribeToGame();
        fsHelper.fetchQuestionsForGame(gameModel.getId(), new CollectionCallback<QuestionModel>() {
            @Override
            public void onCollectionReady(List<QuestionModel> objects) {
                questions = objects;
                Collections.sort(questions, new QuestionModel.IndexComparator());
                currentQuestion = findCurrentQuestion();
                if (currentQuestion == null) {
                    setState(waitingState);
                } else if (currentQuestion.getIndex() == 0) {
                    setState(questionState);
                } else if (currentQuestion.getStartTime() == null) {
                    setState(resultsState);
                }
                String categoryText = "Category: " + currentQuestion.getCategory();
                categoryTextView.setText(categoryText);
                questionTextView.setText(currentQuestion.getQuestion());
                subscribeToQuestions();
                subscribeToAnswers();
            }
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(GameActivity.this, "Failed to fetch questions", Toast.LENGTH_SHORT).show();
            }
        });

        fsHelper.fetchPlayersForGame(gameModel.getId(), new CollectionCallback<PlayerModel>() {
            @Override
            public void onCollectionReady(List<PlayerModel> objects) {
                players = objects;
                for (PlayerModel player : players) {
                    if (player.getId().equals(playerId)) {
                        currentPlayer = player;
                    }
                }
                setAnswerButtons();
                subscribeToPlayers();
            }
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(GameActivity.this, "Failed to fetch players", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setAnswerButtons() {
        // Set the number of columns in the GridLayout based on the number of players
        int numPlayers = players.size();

        buttonsLayout.removeAllViews();

        // Create and add the buttons to the GridLayout
        for (PlayerModel player : players) {
            String playerName = "";
            if (player != null) {
                playerName = player.getId();
            }

            // Set padding between buttons
            buttonsLayout.setPadding(10, 10, 10, 10);
            buttonsLayout.setClipToPadding(false);
            ScrollView sv = new ScrollView(this);
            //Add your widget as a child of the ScrollView.
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            Button button = new Button(this);
            button.setText(playerName);
            typeface = Typeface.createFromAsset(getAssets(),"changaone_regular.ttf");
            button.setTypeface(typeface);
            button.setTextSize(25);
            button.setBackgroundColor(Color.parseColor("#2980B9"));
            button.setTextColor(Color.WHITE);
            params.setMargins(10, 10, 10, 10);
            params.width = GridLayout.LayoutParams.MATCH_PARENT;
            params.height = GridLayout.LayoutParams.MATCH_PARENT;
            button.setLayoutParams(params);
            buttonsLayout.addView(button);

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fsHelper.sendAnswer(gameModel.getId(), currentQuestion.getId(), new AnswerModel(currentPlayer.getId(), player.getId()), new OnCompleteCallback() {
                        @Override
                        public void onSuccess() {
                            setState(waitingState);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(GameActivity.this, "Failed to send answer", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        }
    }
    private View makeResultBox(String playerName, String answerName, String addedScore) {
        LinearLayout resultBox = new LinearLayout(this);
        LinearLayout firstLine = new LinearLayout(this);

        LinearLayout.LayoutParams vertical_params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        vertical_params.setMargins(5, 40, 5, 20);
        LinearLayout.LayoutParams horizontal_params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        resultBox.setOrientation(LinearLayout.VERTICAL);
        resultBox.setBackgroundColor(getResources().getColor(R.color.button));
        resultBox.setPadding(70, 30, 70, 30);
        resultBox.setLayoutParams(vertical_params);

        firstLine.setOrientation(LinearLayout.HORIZONTAL);
        firstLine.setPadding(0, 0, 0, 30);
        firstLine.setLayoutParams(horizontal_params);


        TextView nameContainer = new TextView(this);
        TextView addedScoreContainer = new TextView(this);
        TextView playerVoteContainer = new TextView(this);

        nameContainer.setTextAppearance(R.style.M_text);
        nameContainer.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        nameContainer.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));


        addedScoreContainer.setTextAppearance(R.style.M_text);
        addedScoreContainer.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        addedScoreContainer.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));


        playerVoteContainer.setTextAppearance(R.style.S_text);
        playerVoteContainer.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        playerVoteContainer.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));


        nameContainer.setText(playerName);
        addedScoreContainer.setText("+" + addedScore);
        if (answerName == null) {
            playerVoteContainer.setText("No answer");
        } else {
            playerVoteContainer.setText("Voted for " + answerName);
        }

        firstLine.addView(nameContainer);
        firstLine.addView(addedScoreContainer);

        resultBox.addView(firstLine);
        resultBox.addView(playerVoteContainer);
        return resultBox;
    }
    private void handleResultBox(int scoreMultiplier) {

        List<ResultModel> scoreboard = calculateResults(scoreMultiplier);
        Comparator<ResultModel> byScore = Comparator.comparing(ResultModel::getScore).reversed();
        Collections.sort(scoreboard, byScore);
        String shoutoutText = "";

        if(superShoutOut == true){
            superShoutOutTextView.setVisibility(View.VISIBLE);
            superShoutOut = false;
        }
        else {
            superShoutOutTextView.setVisibility(View.GONE);
        }
        if (scoreboard.get(0).getScore() != 0) {
            shoutoutText = "Shoutout to ";
            List<String> shoutouts = new ArrayList<>();
            shoutouts.add(scoreboard.get(0).getAnswerId());
            int topScore = scoreboard.get(0).getScore();
            for (ResultModel player : scoreboard) {
                if (player.getScore() < topScore) {
                    break;
                }
                if (!shoutouts.contains(player.getAnswerId())) {
                    shoutouts.add(player.getAnswerId());
                }
            }
            for (int i = 0; i < shoutouts.size(); i++) {
                if (i == shoutouts.size() - 1) {
                    shoutoutText += shoutouts.get(i);
                } else {
                    shoutoutText += shoutouts.get(i) + " & ";
                }
            }
            shoutoutText += "!";
        }
        shoutoutTextView.setText(shoutoutText);

        resultGrid.removeAllViews();

        // Create and add the buttons to the GridLayout
        for (ResultModel player : scoreboard) {
            if (player != null) {
                String playerName = player.getPlayerId();
                String answerName = player.getAnswerId();
                String addedScore = player.getScore().toString();

                resultGrid.addView(makeResultBox(playerName, answerName, addedScore));
            }
        }
    }
    private List<ResultModel> calculateResults(int scoreMultiplier) {
        //store score
        List<ResultModel> list = new ArrayList<>();

        for (AnswerModel answer : currentAnswers) {
            String player = answer.getPlayerId();
            String choice = answer.getAnswer();

            int count = 0;
            for (AnswerModel otheranswer : currentAnswers) {
                if (otheranswer.getAnswer().equals(choice)) {
                    count++;
                }
            }
            if (count == players.size()){
                superShoutOut = true;
            }
            int score = count * 100 * scoreMultiplier;
            if(count == 1){
                score = 0;
            }
            list.add(new ResultModel(player,choice,score));
        }
        if (list.size() < players.size()) {
            players.stream().filter(player -> list.stream().noneMatch(result -> result.getPlayerId().equals(player.getId()))).forEach(player -> list.add(new ResultModel(player.getId(), null, 0)));
        }
        return list;
    }

    private synchronized QuestionModel findCurrentQuestion() {
        Collections.sort(questions, new QuestionModel.IndexComparator());
        for (QuestionModel question : questions) {
            if (question.getEndTime() == null) {
                return question;
            }
        }
        return null;
    }

    private void startTimer(long millis) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerTextView.setText(String.format(Locale.getDefault(), "%02d:%02d", millisUntilFinished / 60000, (millisUntilFinished % 60000) / 1000));
            }

            @Override
            public void onFinish() {
                setState(waitingState);
            }
        }.start();
    }

    private void resetTimer(long millis) {
        startTimer(millis);
    }

    private void subscribeToAnswers() {
        fsHelper.subscribeToAnswerUpdates(gameModel.getId(), currentQuestion.getId(), new CollectionEventListener<AnswerModel>() {
            @Override
            public void onEvent(List<AnswerModel> snapshot, Exception e) {
                if (e != null || snapshot == null) {
                    Toast.makeText(GameActivity.this, "Failed to receive answers from other players", Toast.LENGTH_SHORT).show();
                }
                currentAnswers = snapshot;
            }
        });
    }

    private void subscribeToQuestions() {
        fsHelper.subscribeToQuestionUpdates(gameModel.getId(), new CollectionEventListener<QuestionModel>() {
            @Override
            public void onEvent(List<QuestionModel> snapshot, Exception e) {
                if (e != null || snapshot == null) {
                    Toast.makeText(GameActivity.this, "Failed to receive question updates", Toast.LENGTH_SHORT).show();
                    return;
                }
                questions = snapshot;
                QuestionModel nextQuestion = findCurrentQuestion();
                if (nextQuestion == null) {
                    // There is no next question
                    setState(resultsState);
                    return;
                }
                if (nextQuestion.getId().equals(currentQuestion.getId())) {
                    // The question has not changed
                    currentQuestion = nextQuestion;
                    if (currentQuestion.getStartTime() != null  && currentState != waitingState) {
                        // The question has started
                        setState(questionState);
                        startTimer((currentQuestion.getStartTime().getTime() - System.currentTimeMillis()) + COUNTDOWN_MILLIS);
                    }
                    return;
                }


                if (nextQuestion.getStartTime() == null) {
                    // The question has not yet started. Show results for previous question
                    setState(resultsState);
                    handleResultBox(currentQuestion.getScoreMultiplier());
                } else {
                    // The question has started
                    setState(questionState);
                    startTimer((nextQuestion.getStartTime().getTime() - System.currentTimeMillis()) + COUNTDOWN_MILLIS);
                }
                currentQuestion = nextQuestion;
                questionTextView.setText(currentQuestion.getQuestion());
                categoryTextView.setText(currentQuestion.getCategory());
                subscribeToAnswers();
            }
        });
    }

    private void endOfGame() {
        fsHelper.unsubscribeFromAnswerUpdates();
        fsHelper.unsubscribeFromQuestionUpdates();
        fsHelper.unsubscribeFromPlayerUpdates();
        Intent intent = new Intent(this, FinalResultsActivity.class);
        intent.putExtra("gameId", gameModel.getId());
        startActivity(intent);
    }

    private void subscribeToGame() {
        fsHelper.subscribeToGameUpdates(gameModel.getId(), new DocumentEventListener<GameModel>() {
            @Override
            public void onEvent(GameModel snapshot, Exception e) {
                if (e != null || snapshot == null) {
                    Toast.makeText(GameActivity.this, "Failed to receive game updates", Toast.LENGTH_SHORT).show();
                    return;
                }
                gameModel = snapshot;
                if (gameModel.getGameEnded()) {
                    endOfGame();
                }
            }
        });
    }

    private void subscribeToPlayers() {
        fsHelper.subscribeToPlayerUpdates(gameModel.getId(), new CollectionEventListener<PlayerModel>() {
            @Override
            public void onEvent(List<PlayerModel> snapshot, Exception e) {
                if (e != null || snapshot == null) {
                    Toast.makeText(GameActivity.this, "Failed to receive player updates", Toast.LENGTH_SHORT).show();
                    return;
                }
                players = snapshot;
                for (PlayerModel player : players) {
                    if (player.getId().equals(playerId)) {
                        currentPlayer = player;
                    }
                }
                setAnswerButtons();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (gameModel != null) {
            fsHelper.setPlayerIsReady(gameModel.getId(), playerId, false);
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameModel != null) {
            fsHelper.setPlayerIsReady(gameModel.getId(), playerId, false);
        }
    }

    protected void onResume() {
        super.onResume();
        if (gameModel != null) {
            fsHelper.setPlayerIsReady(gameModel.getId(), playerId, true);
        }
    }
}
