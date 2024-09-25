package ntnu.tdt4240.shoutout.firestore.interfaces;

public interface OnCompleteCallback {
    void onSuccess();
    void onFailure(Exception e);
}
