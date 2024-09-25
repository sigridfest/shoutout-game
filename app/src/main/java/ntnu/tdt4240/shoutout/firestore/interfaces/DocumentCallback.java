package ntnu.tdt4240.shoutout.firestore.interfaces;

import ntnu.tdt4240.shoutout.models.Model;

public interface DocumentCallback<T extends Model> {
    void onDocumentReady(T object);
    void onFailure(Exception e);
}
