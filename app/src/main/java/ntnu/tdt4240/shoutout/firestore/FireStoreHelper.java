package ntnu.tdt4240.shoutout.firestore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import ntnu.tdt4240.shoutout.enums.GameLength;
import ntnu.tdt4240.shoutout.enums.GameMode;
import ntnu.tdt4240.shoutout.exceptions.DocumentAlreadyExistsException;
import ntnu.tdt4240.shoutout.exceptions.GameFullException;
import ntnu.tdt4240.shoutout.firestore.interfaces.CollectionCallback;
import ntnu.tdt4240.shoutout.firestore.interfaces.CollectionEventListener;
import ntnu.tdt4240.shoutout.firestore.interfaces.DocumentCallback;
import ntnu.tdt4240.shoutout.firestore.interfaces.DocumentEventListener;
import ntnu.tdt4240.shoutout.firestore.interfaces.OnCompleteCallback;
import ntnu.tdt4240.shoutout.models.AnswerModel;
import ntnu.tdt4240.shoutout.models.CategoryModel;
import ntnu.tdt4240.shoutout.models.GameModel;
import ntnu.tdt4240.shoutout.models.Model;
import ntnu.tdt4240.shoutout.models.PlayerModel;
import ntnu.tdt4240.shoutout.models.QuestionModel;

public class FireStoreHelper {
    private FirebaseFirestore db;
    private ListenerRegistration gameUpdateListenerRegistration;
    private ListenerRegistration playerUpdateListenerRegistration;
    private ListenerRegistration questionUpdateListenerRegistration;
    private ListenerRegistration answerUpdateListenerRegistration;

    private final int PLAYER_UPPER_LIMIT = 12;

    private static FireStoreHelper instance;
    private FireStoreHelper() {
        db = FirebaseFirestore.getInstance();
    }

    public void TestFunction() {
        if (db != null) {
            System.out.println("Android connected to database");
        }
        else {
            System.out.println("Error when connecting to database");
        }
    }

    private <T extends Model> ListenerRegistration subscribeToDocument(String collectionPath, String documentId, Class<T> clazz, DocumentEventListener<T> eventListener) {
        return db.collection(collectionPath).document(documentId)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            eventListener.onEvent(null, error);
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            T object = documentSnapshot.toObject(clazz);
                            eventListener.onEvent(object, null);
                        } else {
                            eventListener.onEvent(null, new Exception("Not found"));
                        }
                    }
                });
    }

    private <T extends Model> ListenerRegistration subscribeToCollection(String collectionPath, Class<T> clazz, CollectionEventListener<T> eventListener) {
        return db.collection(collectionPath)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot documentSnapshot, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            eventListener.onEvent(null, error);
                            return;
                        }

                        List<T> updatedList = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : documentSnapshot) {
                            T object = doc.toObject(clazz);
                            updatedList.add(object);
                        }

                        eventListener.onEvent(updatedList, null);
                    }
                });
    }

    private <T extends Model> void getDocumentByUniqueField(String collectionPath, String field, Object value, Class<T> clazz, DocumentCallback<T> documentCallback) {
        db.collection(collectionPath).whereEqualTo(field, value)
                .get()
                .addOnCompleteListener(new OnCompleteListener<com.google.firebase.firestore.QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<com.google.firebase.firestore.QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            com.google.firebase.firestore.QuerySnapshot querySnapshot = task.getResult();
                            if (querySnapshot.isEmpty()) {
                                documentCallback.onFailure(new Exception("Document not found"));
                            } else {
                                T object = querySnapshot.getDocuments().get(0).toObject(clazz);
                                documentCallback.onDocumentReady(object);
                            }
                        } else {
                            documentCallback.onFailure(task.getException());
                        }
                    }
                });
    }

    private <T extends Model> void getCollection(String collectionPath, Class<T> clazz, CollectionCallback<T> callback) {
        db.collection(collectionPath)
                .get()
                .addOnCompleteListener(new OnCompleteListener<com.google.firebase.firestore.QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<com.google.firebase.firestore.QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            com.google.firebase.firestore.QuerySnapshot querySnapshot = task.getResult();
                            List<T> list = new ArrayList<>();
                            for (QueryDocumentSnapshot doc : querySnapshot) {
                                T object = doc.toObject(clazz);
                                list.add(object);
                            }
                            callback.onCollectionReady(list);
                        } else {
                            callback.onFailure(task.getException());
                        }
                    }
                });
    }

    private <T extends Model> Task<T> createDocument(String collection, T model) {
        TaskCompletionSource<T> taskCompletionSource = new TaskCompletionSource<>();

        db.collection(collection)
                .add(model)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        String newId = documentReference.getId();
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("id", newId);
                        model.setId(newId);

                        Task<Void> task = updateDocument(collection, newId, map);

                        task.addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                taskCompletionSource.setResult(model);
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(Exception e) {
                                taskCompletionSource.setException(e);
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        taskCompletionSource.setException(e);
                    }
                });

        return taskCompletionSource.getTask();
    }

    private Task<Void> createDocument(String collection, String documentId, Model model) {
        TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();

        DocumentReference docRef = db.collection(collection).document(documentId);

        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        taskCompletionSource.setException(new DocumentAlreadyExistsException("Document already exists", document));
                    } else {
                        docRef.set(model).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                taskCompletionSource.setResult(null);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                taskCompletionSource.setException(e);
                            }
                        });
                    }
                } else {
                    taskCompletionSource.setException(new Exception("Error getting document"));
                }
            }
        });
        return taskCompletionSource.getTask();
    }

    private Task<Void> updateDocument(String collection, String documentId, Map<String, Object> values) {
        TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
        DocumentReference docRef = db.collection(collection).document(documentId);
        docRef
                .update(values)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            taskCompletionSource.setResult(null);
                        } else {
                            taskCompletionSource.setException(task.getException());
                        }
                    }
                });
        return taskCompletionSource.getTask();
    }

    private void deleteDocument(String collection, String documentId, OnCompleteCallback onCompleteCallback) {
        db.collection(collection).document(documentId)
                .delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            onCompleteCallback.onSuccess();
                        } else {
                            onCompleteCallback.onFailure(task.getException());
                        }
                    }
                });
    }

    public void createGame(GameModel gameModel, GameLength gameLength, DocumentCallback callback) {
        generateUniquePin(6, new OnUniquePinGeneratedCallback() {
            @Override
            public void onUniquePinGenerated(String uniquePin) {
                gameModel.setPin(uniquePin);
                Task<GameModel> task = createDocument("games", gameModel);

                task.addOnSuccessListener(new OnSuccessListener<GameModel>() {
                    @Override
                    public void onSuccess(GameModel gameModel) {
                        selectQuestions(gameModel, gameLength, callback);
                    }
                });
                task.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callback.onFailure(e);
                    }
                });
            }
        });
    }

    private interface OnUniquePinGeneratedCallback {
        void onUniquePinGenerated(String uniquePin);
    }

    private void generateUniquePin(int length, OnUniquePinGeneratedCallback callback) {
        String pin = generatePin(length);
        getDocumentByUniqueField("games", "pin", pin, GameModel.class, new DocumentCallback<GameModel>() {
            @Override
            public void onDocumentReady(GameModel document) {
                // The pin is not unique, so we generate a new one
                generateUniquePin(length, callback);
            }

            @Override
            public void onFailure(Exception e) {
                // The pin is unique, so we continue
                callback.onUniquePinGenerated(pin);
            }
        });
    }

    private String generatePin(int length) {
        String availableChars = "0123456789";
        StringBuilder pinBuilder = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(availableChars.length());
            pinBuilder.append(availableChars.charAt(index));
        }

        return pinBuilder.toString();
    }

    private void selectQuestions(GameModel gameModel, GameLength gameLength, DocumentCallback callback) {
        int questionsPerCategory;
        switch (gameLength) {
            case SHORT:
                questionsPerCategory = 3;
                break;
            case MEDIUM:
                questionsPerCategory = 5;
                break;
            case LONG:
                questionsPerCategory = 7;
                break;
            default:
                questionsPerCategory = 5;
        }
        getCollection("categories", CategoryModel.class, new CollectionCallback<CategoryModel>() {
            @Override
            public void onCollectionReady(List<CategoryModel> categories) {
                List<CategoryModel> selectedCategories = new ArrayList<>();
                List<CategoryModel> partyCategories = categories.stream().filter(category -> category.getGameMode() == GameMode.PARTY).collect(Collectors.toList());
                List<CategoryModel> casualCategories = categories.stream().filter(category -> category.getGameMode() == GameMode.CASUAL).collect(Collectors.toList());
                switch (gameModel.getGameMode()) {
                    case PARTY:
                        selectedCategories.addAll(selectRandomItems(partyCategories, 2));
                        break;
                    case CASUAL:
                        selectedCategories.addAll(selectRandomItems(casualCategories, 2));
                        break;
                    case MIX:
                        selectedCategories.addAll(selectRandomItems(casualCategories, 1));
                        selectedCategories.addAll(selectRandomItems(partyCategories, 1));
                        break;
                }
                List<Task<QuestionModel>> tasks = new ArrayList<>();
                int index = 0;
                for (CategoryModel category : selectedCategories) {
                    List<String> selectedQuestions = selectRandomItems(category.getQuestions(), questionsPerCategory);
                    for (String question : selectedQuestions) {
                        tasks.add(createDocument("games/" + gameModel.getId() + "/questions", new QuestionModel(question, category.getName(), index++)));
                    }
                }
                Tasks.whenAllSuccess(tasks).addOnSuccessListener(new OnSuccessListener<List<Object>>() {
                    @Override
                    public void onSuccess(List<Object> objects) {
                        callback.onDocumentReady(gameModel);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    private <T> List<T> selectRandomItems(List<T> list, int numberOfItems) {
        if (list.size() < numberOfItems) {
            throw new IllegalArgumentException("List size is smaller than the number of items requested.");
        }

        Random random = new Random();
        Set<Integer> randomIndicesSet = new HashSet<>();
        while (randomIndicesSet.size() < numberOfItems) {
            randomIndicesSet.add(random.nextInt(list.size()));
        }

        List<T> selectedItems = new ArrayList<>(numberOfItems);
        for (int index : randomIndicesSet) {
            selectedItems.add(list.get(index));
        }

        return selectedItems;
    }

    public void getGameByPin(String pin, DocumentCallback<GameModel> documentCallback) {
        getDocumentByUniqueField("games", "pin", pin, GameModel.class, new DocumentCallback<GameModel>() {
            @Override
            public void onDocumentReady(GameModel object) {
                fetchPlayersForGame(object.getId(), new CollectionCallback<PlayerModel>() {
                    @Override
                    public void onCollectionReady(List<PlayerModel> list) {
                        documentCallback.onDocumentReady(object);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        documentCallback.onFailure(e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                documentCallback.onFailure(e);
            }
        });

    }

    public void fetchPlayersForGame(String gameId, CollectionCallback<PlayerModel> callback) {
        getCollection("games/" + gameId + "/players", PlayerModel.class, callback);
    }

    public void fetchQuestionsForGame(String gameId, CollectionCallback<QuestionModel> callback) {
        getCollection("games/" + gameId + "/questions", QuestionModel.class, callback);
    }

    public void joinGame(String gameId, PlayerModel playerModel, OnCompleteCallback callback) {
        getCollection("games/" + gameId + "/players", PlayerModel.class, new CollectionCallback<PlayerModel>() {
            @Override
            public void onCollectionReady(List<PlayerModel> list) {
                if (list.size() >= PLAYER_UPPER_LIMIT) {
                    callback.onFailure(new GameFullException("The game is full"));
                } else {
                    Task<Void> task = createDocument("games/" + gameId + "/players", playerModel.getId(), playerModel);
                    task.addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            callback.onSuccess();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            callback.onFailure(e);
                        }
                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    public void setPlayerIsReady(String gameId, String playerId, boolean isReady) {
        HashMap<String, Object> data = new HashMap<>();
        data.put("isReady", isReady);
        Task<Void> task = updateDocument("games/" + gameId + "/players", playerId, data);
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // TODO: Handle failure
            }
        });
    }

    public void sendAnswer(String gameId, String questionId, AnswerModel answer, OnCompleteCallback callback) {
        Task<AnswerModel> task = createDocument("games/" + gameId + "/questions/" + questionId + "/answers", answer);
        task.addOnSuccessListener(new OnSuccessListener<AnswerModel>() {
            @Override
            public void onSuccess(AnswerModel answerModel) {
                callback.onSuccess();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                callback.onFailure(e);
            }
        });
    }


    public void subscribeToGameUpdates(String gameId, DocumentEventListener<GameModel> eventListener) {
        gameUpdateListenerRegistration = subscribeToDocument("games", gameId, GameModel.class, eventListener);
    }


    public void unsubscribeFromGameUpdates() {
        if (gameUpdateListenerRegistration != null) {
            gameUpdateListenerRegistration.remove();
            gameUpdateListenerRegistration = null;
        }
    }

    public void subscribeToPlayerUpdates(String gameId, CollectionEventListener<PlayerModel> eventListener) {
        playerUpdateListenerRegistration = subscribeToCollection("games/" + gameId + "/players", PlayerModel.class, eventListener);
    }

    public void unsubscribeFromPlayerUpdates() {
        if (playerUpdateListenerRegistration != null) {
            playerUpdateListenerRegistration.remove();
            playerUpdateListenerRegistration = null;
        }
    }

    public void subscribeToAnswerUpdates(String gameId, String questionId, CollectionEventListener<AnswerModel> eventListener) {
        answerUpdateListenerRegistration = subscribeToCollection("games/" + gameId + "/questions/" + questionId + "/answers", AnswerModel.class, eventListener);
    }

    public void unsubscribeFromAnswerUpdates() {
        if (answerUpdateListenerRegistration != null) {
            answerUpdateListenerRegistration.remove();
            answerUpdateListenerRegistration = null;
        }
    }

    public void subscribeToQuestionUpdates(String gameId, CollectionEventListener<QuestionModel> eventListener) {
        questionUpdateListenerRegistration = subscribeToCollection("games/" + gameId + "/questions", QuestionModel.class, eventListener);
    }

    public void unsubscribeFromQuestionUpdates() {
        if (questionUpdateListenerRegistration != null) {
            questionUpdateListenerRegistration.remove();
            questionUpdateListenerRegistration = null;
        }
    }

    public void startGame(String gameId, OnCompleteCallback onCompleteCallback) {
        HashMap<String, Object> data = new HashMap<>();
        data.put("gameStarted", true);
        Task<Void> task = updateDocument("games", gameId, data);
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                fetchQuestionsForGame(gameId, new CollectionCallback<QuestionModel>() {
                    @Override
                    public void onCollectionReady(List<QuestionModel> objects) {
                        Collections.sort(objects, new QuestionModel.IndexComparator());
                        HashMap<String, Object> values = new HashMap<>();
                        values.put("startTime", FieldValue.serverTimestamp());
                        Task<Void> task = updateDocument("games/" + gameId + "/questions", objects.get(0).getId(), values);
                        task.addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                onCompleteCallback.onSuccess();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                onCompleteCallback.onFailure(e);
                            }
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {

                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                onCompleteCallback.onFailure(e);
            }
        });
    }

    public void nextQuestion(String gameId) {
        fetchQuestionsForGame(gameId, new CollectionCallback<QuestionModel>() {
            @Override
            public void onCollectionReady(List<QuestionModel> questions) {
                Collections.sort(questions, new QuestionModel.IndexComparator());
                for (QuestionModel question : questions) {
                    if (question.getStartTime() == null && question.getEndTime() == null) {
                        HashMap<String, Object> values = new HashMap<>();
                        values.put("startTime", FieldValue.serverTimestamp());
                        updateDocument("games/" + gameId + "/questions", question.getId(), values);
                        return;
                    }
                }
                HashMap<String, Object> values = new HashMap<>();
                values.put("gameEnded", true);
                updateDocument("games", gameId, values);
            }
            @Override
            public void onFailure(Exception e) {

            }
        });
    }

    public void leaveGame(String playerId, String gameId, OnCompleteCallback onCompleteCallback) {
        unsubscribeFromGameUpdates();
        unsubscribeFromPlayerUpdates();
        deleteDocument("games/" + gameId + "/players", playerId, onCompleteCallback);
    }

    public static FireStoreHelper getInstance() {
        if (instance == null) {
            instance = new FireStoreHelper();
        }
        return instance;
    }
}
