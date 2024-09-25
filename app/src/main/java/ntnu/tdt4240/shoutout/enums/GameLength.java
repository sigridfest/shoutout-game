package ntnu.tdt4240.shoutout.enums;

public enum GameLength {
    SHORT, MEDIUM, LONG;

    // Conversion method for GameLength
    public static GameLength getGameLengthFromSpinner(int position) {
        switch (position) {
            case 0:
                return GameLength.SHORT;
            case 1:
                return GameLength.MEDIUM;
            case 2:
                return GameLength.LONG;
            default:
                return GameLength.MEDIUM; // Set default value
        }
    }
}