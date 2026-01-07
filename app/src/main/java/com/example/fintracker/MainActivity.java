package com.example.fintracker;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // User is logged in, show main UI
        setContentView(R.layout.activity_main);

        // ============================================
        // FONT CHECK - Check if Poppins is working
        // ============================================
        checkPoppinsFont();
        // ============================================

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new HomeFragment())
                    .commit();
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_transaction) {
                selectedFragment = new TransactionFragment();
            } else if (itemId == R.id.nav_budget) {
                selectedFragment = new BudgetFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, selectedFragment)
                        .commit();
                return true;
            }

            return false;
        });
    }

    /**
     * Check if Poppins font is properly loaded
     */
    private void checkPoppinsFont() {
        try {
            // Create a test TextView programmatically
            TextView testTextView = new TextView(this);
            testTextView.setTypeface(getResources().getFont(R.font.poppins));

            Typeface typeface = testTextView.getTypeface();

            Log.d("FONT_CHECK", "=====================");
            Log.d("FONT_CHECK", "Poppins Font Check:");
            Log.d("FONT_CHECK", "Typeface: " + typeface.toString());
            Log.d("FONT_CHECK", "Is Bold: " + typeface.isBold());
            Log.d("FONT_CHECK", "Is Italic: " + typeface.isItalic());
            Log.d("FONT_CHECK", "=====================");
            Log.d("FONT_CHECK", "✅ Poppins font loaded successfully!");

        } catch (Exception e) {
            Log.e("FONT_CHECK", "=====================");
            Log.e("FONT_CHECK", "❌ ERROR: Poppins font failed to load!");
            Log.e("FONT_CHECK", "Error: " + e.getMessage());
            Log.e("FONT_CHECK", "=====================");
            e.printStackTrace();
        }
    }
}