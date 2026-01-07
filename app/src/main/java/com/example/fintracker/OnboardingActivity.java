package com.example.fintracker;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private LinearLayout dotsLayout;
    private Button btnNext;
    private Button btnSkip;

    private final int[] layouts = {
            R.layout.onboarding_slide1,
            R.layout.onboarding_slide2,
            R.layout.onboarding_slide3
    };

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        // Initialize views
        viewPager = findViewById(R.id.viewPager);
        dotsLayout = findViewById(R.id.dotsLayout);
        btnNext = findViewById(R.id.btnNext);
        btnSkip = findViewById(R.id.btnSkip);

        // Setup ViewPager
        OnboardingAdapter adapter = new OnboardingAdapter(this, layouts);
        viewPager.setAdapter(adapter);

        // Add dots
        addDots(0);

        // ViewPager change listener
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                addDots(position);

                // Change button text on last slide
                if (position == layouts.length - 1) {
                    btnNext.setText("Get Started");
                    btnSkip.setVisibility(View.GONE);
                } else {
                    btnNext.setText("Next");
                    btnSkip.setVisibility(View.VISIBLE);
                }
            }
        });

        // Next button click
        btnNext.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem() + 1;
            if (current < layouts.length) {
                viewPager.setCurrentItem(current);
            } else {
                finishOnboarding();
            }
        });

        // Skip button click
        btnSkip.setOnClickListener(v -> finishOnboarding());
    }

    private void addDots(int currentPage) {
        dotsLayout.removeAllViews();

        for (int i = 0; i < layouts.length; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    i == currentPage ? 40 : 20,
                    20
            );
            params.setMargins(8, 0, 8, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(
                    i == currentPage ? R.drawable.dot_active : R.drawable.dot_inactive
            );
            dotsLayout.addView(dot);
        }
    }

    private void finishOnboarding() {
        // Save that user has seen onboarding
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        prefs.edit().putBoolean("hasSeenOnboarding", true).apply();

        // Navigate to login
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
