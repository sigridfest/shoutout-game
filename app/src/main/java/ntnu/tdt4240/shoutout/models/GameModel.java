package ntnu.tdt4240.shoutout.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.Exclude;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ntnu.tdt4240.shoutout.enums.GameMode;

public class GameModel extends Model implements Serializable {
    private String pin;
    private Date created;
    private GameMode gameMode;
    private boolean gameStarted;
    private boolean gameEnded;

    public GameModel() {
    }

    public GameModel(GameMode gameMode) {
        super();
        this.gameMode = gameMode;
        this.pin = "";
        this.created = new Date();
        this.gameStarted = false;
        this.gameEnded = false;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public boolean getGameStarted() {
        return gameStarted;
    }

    public void setGameStarted(boolean gameStarted) {
        this.gameStarted = gameStarted;
    }

    public boolean getGameEnded() {
        return gameEnded;
    }

    public void setGameEnded(boolean gameEnded) {
        this.gameEnded = gameEnded;
    }

    @Override
    public String toString() {
        return "Game{" +
                "gameId='" + id + '\'' +
                ", pin='" + pin + '\'' +
                ", created=" + created +
                ", gameStarted=" + gameStarted +
                '}';
    }

}
