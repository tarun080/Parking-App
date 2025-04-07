// app/src/main/java/com/smartparking/viewmodels/AuthViewModel.java
package com.smartparking.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.google.firebase.auth.FirebaseUser;
import com.smartparking.models.User;
import com.smartparking.repositories.FirebaseRepository;
import com.smartparking.repositories.RoomRepository;

public class AuthViewModel extends AndroidViewModel {
    private final FirebaseRepository firebaseRepository;
    private final RoomRepository roomRepository;

    public AuthViewModel(@NonNull Application application) {
        super(application);
        firebaseRepository = new FirebaseRepository(application);
        roomRepository = new RoomRepository(application);
    }

    public void register(String email, String password, String name, String phoneNumber) {
        firebaseRepository.register(email, password, name, phoneNumber);
    }

    public void login(String email, String password) {
        firebaseRepository.login(email, password);
    }

    public void logout() {
        firebaseRepository.logout();
    }

    public void resetPassword(String email) {
        firebaseRepository.resetPassword(email);
    }

    public LiveData<FirebaseUser> getUserLiveData() {
        return firebaseRepository.getUserLiveData();
    }

    public LiveData<Boolean> getLoggedOutLiveData() {
        return firebaseRepository.getLoggedOutLiveData();
    }

    public LiveData<User> getUserData() {
        return firebaseRepository.getUserData();
    }

    public void syncUserToLocal(User user) {
        roomRepository.insertUser(user);
    }

    public LiveData<User> getLocalUserById(String userId) {
        return roomRepository.getUserById(userId);
    }
}