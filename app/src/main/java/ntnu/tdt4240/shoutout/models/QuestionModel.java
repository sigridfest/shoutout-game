package ntnu.tdt4240.shoutout.models;

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;

public class QuestionModel extends Model {
    private String question;
    private String category;
    private int index;
    private int scoreMultiplier;
    private int answerCount;
    private Date startTime;
    private Date endTime;

    public QuestionModel() {
    }

    public QuestionModel(String question, String category, int index) {
        this.question = question;
        this.category = category;
        this.index = index;
        this.scoreMultiplier = 1;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getScoreMultiplier() {
        return scoreMultiplier;
    }

    public void setScoreMultiplier(int scoreMultiplier) {
        this.scoreMultiplier = scoreMultiplier;
    }

    public int getAnswerCount() {
        return answerCount;
    }

    public void setAnswerCount(int answerCount) {
        this.answerCount = answerCount;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public static class IndexComparator implements Comparator<QuestionModel> {
        @Override
        public int compare(QuestionModel q1, QuestionModel q2) {
            return Integer.compare(q1.getIndex(), q2.getIndex());
        }
    }
}
