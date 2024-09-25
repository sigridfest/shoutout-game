package ntnu.tdt4240.shoutout.firestore.interfaces;

import java.util.List;

public interface CollectionEventListener<T> {
    void onEvent(List<T> snapshot, Exception e);
}
