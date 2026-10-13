package com.hcdc.qualipea;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

/** Turns Firebase errors into short, friendly messages. */
public final class AuthErrors {
    private AuthErrors() {}

    public static boolean isNetwork(Exception e) {
        return e instanceof FirebaseNetworkException;
    }

    public static String friendly(Exception e, boolean signingUp) {
        if (isNetwork(e)) return "No internet connection. Check your network and try again.";
        if (e instanceof FirebaseAuthUserCollisionException)
            return "An account with this email already exists.";
        if (e instanceof FirebaseAuthWeakPasswordException)
            return "Choose a stronger password (at least 6 characters).";
        if (e instanceof FirebaseAuthInvalidUserException
                || e instanceof FirebaseAuthInvalidCredentialsException) {
            return signingUp ? "That email address doesn't look valid."
                    : "Incorrect email or password.";
        }
        return "Something went wrong. Please try again.";
    }
}