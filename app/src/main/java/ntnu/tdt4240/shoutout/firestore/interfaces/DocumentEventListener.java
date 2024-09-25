package ntnu.tdt4240.shoutout.firestore.interfaces;

import ntnu.tdt4240.shoutout.models.Model;

public interface DocumentEventListener<T extends Model> {
    void onEvent(T snapshot, Exception e);
}
