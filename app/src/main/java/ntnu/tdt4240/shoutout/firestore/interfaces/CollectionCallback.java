package ntnu.tdt4240.shoutout.firestore.interfaces;

import java.util.List;
import ntnu.tdt4240.shoutout.models.Model;

public interface CollectionCallback<T extends Model> {
    void onCollectionReady(List<T> objects);
    void onFailure(Exception e);
}
