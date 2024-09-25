package ntnu.tdt4240.shoutout.models;

import java.io.Serializable;
import java.util.UUID;

public abstract class Model implements Serializable {
    protected String id;
    public Model() {
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
}
