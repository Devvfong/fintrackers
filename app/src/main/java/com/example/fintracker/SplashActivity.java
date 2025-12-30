package com.example.fintracker;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 3000; // 3 seconds
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Find views
        ImageView logo = findViewById(R.id.ivSplashLogo);
        TextView appName = findViewById(R.id.tvAppName);

        // Load animations
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);

        // Apply animations
        logo.startAnimation(fadeIn);
        appName.startAnimation(slideUp);

        // Navigate after delay
        new Handler().postDelayed(() -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();

            Intent intent;
            if (currentUser != null) {
                // User is logged in, go to MainActivity
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                // Check if user has seen onboarding
                boolean hasSeenOnboarding = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                        .getBoolean("hasSeenOnboarding", false);

                if (hasSeenOnboarding) {
                    // Go directly to login
                    intent = new Intent(SplashActivity.this, LoginActivity.class);
                } else {
                    // Show onboarding first
                    intent = new Intent(SplashActivity.this, OnboardingActivity.class);
                }
            }

            startActivity(intent);
            finish();
        }, SPLASH_DURATION);
    }
}