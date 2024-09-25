package ntnu.tdt4240.shoutout.models;

import java.io.Serializable;
import java.util.Comparator;

public class PlayerModel extends Model implements Serializable {
    private int score;
    private boolean isReady;

    public PlayerModel() {
    }

    public PlayerModel(String id) {
        super();
        this.id = id;
        score = 0;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public boolean getIsReady() {
        return isReady;
    }

    public void setIsReady(boolean ready) {
        isReady = ready;
    }

    @Override
    public String toString() {
        return "Player{" +
                "id='" + id + '\'' +
                ", score=" + score +
                ", isReady=" + isReady +
                '}';
    }

    public static class ScoreComparator implements Comparator<PlayerModel> {
        @Override
        public int compare(PlayerModel o1, PlayerModel o2) {
            return o2.getScore() - o1.getScore();
        }
    }
}
