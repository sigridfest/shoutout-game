package ntnu.tdt4240.shoutout.exceptions;

import android.view.Display;

import com.google.firebase.firestore.DocumentSnapshot;

import ntnu.tdt4240.shoutout.models.Model;

public class DocumentAlreadyExistsException extends Exception {
    DocumentSnapshot document;
    public DocumentAlreadyExistsException(String message, DocumentSnapshot document) {
        super(message);
        this.document = document;
    }

    public DocumentSnapshot getDocument() {
        return document;
    }
}
