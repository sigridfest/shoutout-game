package ntnu.tdt4240.shoutout.enums;

public enum GameMode {
    CASUAL, PARTY, MIX;

    // Conversion method for GameMode
    public static GameMode getGameModeFromSpinner(int position) {
        switch (position) {
            case 0:
                return GameMode.CASUAL;
            case 1:
                return GameMode.PARTY;
            case 2:
                return GameMode.MIX;
            default:
                return GameMode.MIX; // Set default value
        }
    }
}