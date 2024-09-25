package ntnu.tdt4240.shoutout.gameState;

import android.view.View;

import ntnu.tdt4240.shoutout.GameActivity;

public class QuestionState implements GameState {
    private GameActivity gameActivity;

    public QuestionState(GameActivity gameActivity) {
        this.gameActivity = gameActivity;
    }

    @Override
    public void enterState() {
        gameActivity.questionLayout.setVisibility(View.VISIBLE);
        gameActivity.waitingLayout.setVisibility(View.GONE);
        gameActivity.resultsLayout.setVisibility(View.GONE);
    }

    @Override
    public void exitState() {
        gameActivity.questionLayout.setVisibility(View.GONE);
    }
}
