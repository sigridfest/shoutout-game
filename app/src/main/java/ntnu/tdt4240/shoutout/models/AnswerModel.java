package ntnu.tdt4240.shoutout.models;

public class AnswerModel extends Model {
    private String playerId;
    private String answer;

    public AnswerModel() {
    }

    public AnswerModel(String playerId, String answer) {
        this.playerId = playerId;
        this.answer = answer;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
