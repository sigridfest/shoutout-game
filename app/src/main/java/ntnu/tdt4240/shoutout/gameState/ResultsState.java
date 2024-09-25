package ntnu.tdt4240.shoutout.gameState;

import android.view.View;

import ntnu.tdt4240.shoutout.GameActivity;

public class ResultsState implements GameState {
    private GameActivity gameActivity;

    public ResultsState(GameActivity gameActivity) {
        this.gameActivity = gameActivity;
    }

    @Override
    public void enterState() {
        gameActivity.resultsLayout.setVisibility(View.VISIBLE);
        gameActivity.waitingLayout.setVisibility(View.GONE);
        gameActivity.questionLayout.setVisibility(View.GONE);
    }

    @Override
    public void exitState() {
        gameActivity.resultsLayout.setVisibility(View.GONE);
    }
}