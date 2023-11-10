package be.kuleuven.distributedsystems.cloud.controller;

import be.kuleuven.distributedsystems.cloud.Application;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;

/*
* IDK if this class is really needed, and I have not tested it
* perhaps some of this can be static in the application class, who knows
* */
public class FirebaseWrapper {

    public FirestoreOptions firestoreOptions;
    public Firestore db;

    final String firestoreHost = "0.0.0.0:8084";

    public FirebaseWrapper(){
        try {
            firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
                    .setEmulatorHost(firestoreHost)
                    .setProjectId(Application.projectId())
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .build();

            db = firestoreOptions.getService();
        }catch (Exception e){
            System.out.println("Error configuring database");
        }
    }


}
