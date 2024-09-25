package ntnu.tdt4240.shoutout.models;

import java.util.List;

import ntnu.tdt4240.shoutout.enums.GameMode;

public class CategoryModel extends Model {
    private String name;
    private GameMode gameMode;
    private List<String> questions;

    public CategoryModel() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public List<String> getQuestions() {
        return questions;
    }

    public void setQuestions(List<String> questions) {
        this.questions = questions;
    }
}
