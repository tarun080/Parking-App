// app/src/main/java/com/smartparking/repositories/FirebaseRepository.java
package com.smartparking.repositories;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.smartparking.models.User;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class FirebaseRepository {
    private static final String TAG = "FirebaseRepository";

    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;
    private final MutableLiveData<FirebaseUser> userLiveData;
    private final MutableLiveData<Boolean> loggedOutLiveData;
    private final Executor executor;

    public FirebaseRepository(Application application) {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        userLiveData = new MutableLiveData<>();
        loggedOutLiveData = new MutableLiveData<>();
        executor = Executors.newSingleThreadExecutor();

        if (firebaseAuth.getCurrentUser() != null) {
            userLiveData.postValue(firebaseAuth.getCurrentUser());
            loggedOutLiveData.postValue(false);
        }
    }

    public void register(String email, String password, String name, String phoneNumber) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        userLiveData.postValue(firebaseAuth.getCurrentUser());
                        createUserInFirestore(email, name, phoneNumber);
                    } else {
                        Log.e(TAG, "Registration failed: " + task.getException().getMessage());
                    }
                });
    }

    private void createUserInFirestore(String email, String name, String phoneNumber) {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            User user = new User(
                    firebaseUser.getUid(),
                    email,
                    name,
                    phoneNumber
            );

            firestore.collection("users")
                    .document(firebaseUser.getUid())
                    .set(user)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "User added to Firestore");
//                        roomRepository.insertUser(user);
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error adding user to Firestore", e));
        }
    }

    public void login(String email, String password) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        userLiveData.postValue(firebaseAuth.getCurrentUser());
                        loggedOutLiveData.postValue(false);
                    } else {
                        Log.e(TAG, "Login failed: " + task.getException().getMessage());
                    }
                });
    }

    public void logout() {
        firebaseAuth.signOut();
        loggedOutLiveData.postValue(true);
    }

    public void resetPassword(String email) {
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Password reset email sent");
                    } else {
                        Log.e(TAG, "Error sending password reset email", task.getException());
                    }
                });
    }

    public LiveData<User> getUserData() {
        MutableLiveData<User> userDataLiveData = new MutableLiveData<>();
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        if (firebaseUser != null) {
            DocumentReference docRef = firestore.collection("users").document(firebaseUser.getUid());
            docRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        User user = document.toObject(User.class);
                        userDataLiveData.setValue(user);
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            });
        }

        return userDataLiveData;
    }

    public LiveData<FirebaseUser> getUserLiveData() {
        return userLiveData;
    }

    public LiveData<Boolean> getLoggedOutLiveData() {
        return loggedOutLiveData;
    }
}