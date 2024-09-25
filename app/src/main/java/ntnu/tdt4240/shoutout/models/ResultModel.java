package ntnu.tdt4240.shoutout.models;

public class ResultModel extends Model {
    private String playerId;
    private String answerId;
    private Integer score;

    public ResultModel() {
    }

    public ResultModel(String playerId, String answerId, Integer score) {
        this.playerId = playerId;
        this.answerId = answerId;
        this.score = score;
    }
    public String getPlayerId() {
        return playerId;
    }
    public String getAnswerId() {return answerId;}
    public Integer getScore() {return score;}
}