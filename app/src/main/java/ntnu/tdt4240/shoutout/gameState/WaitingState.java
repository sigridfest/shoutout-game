package ntnu.tdt4240.shoutout.gameState;

import android.view.View;

import ntnu.tdt4240.shoutout.GameActivity;

public class WaitingState implements GameState {
    private GameActivity gameActivity;

    public WaitingState(GameActivity gameActivity) {
        this.gameActivity = gameActivity;
    }

    @Override
    public void enterState() {
        gameActivity.waitingLayout.setVisibility(View.VISIBLE);
        gameActivity.resultsLayout.setVisibility(View.GONE);
        gameActivity.questionLayout.setVisibility(View.GONE);
    }

    @Override
    public void exitState() {
        gameActivity.waitingLayout.setVisibility(View.GONE);
    }
}
