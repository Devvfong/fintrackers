package com.example.fintracker;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {

    private TextView tvAccountBalance, tvIncome, tvExpense;
    private RecyclerView rvRecentTransactions;
    private LineChart spendFrequencyChart;
    private TransactionAdapter adapter;
    private List<Transaction> transactionList;
    private List<Transaction> allTransactions; // For chart

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private double totalIncome = 0;
    private double totalExpense = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        tvAccountBalance = view.findViewById(R.id.tvAccountBalance);
        tvIncome = view.findViewById(R.id.tvIncome);
        tvExpense = view.findViewById(R.id.tvExpense);
        rvRecentTransactions = view.findViewById(R.id.rvRecentTransactions);
        spendFrequencyChart = view.findViewById(R.id.spendFrequencyChart);

        // Setup RecyclerView
        transactionList = new ArrayList<>();
        allTransactions = new ArrayList<>();
        adapter = new TransactionAdapter(getContext(), transactionList);
        rvRecentTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecentTransactions.setAdapter(adapter);

        // Setup Chart
        setupChart();

        // Load data
        loadTransactions();

        return view;
    }

    private void setupChart() {
        spendFrequencyChart.getDescription().setEnabled(false);
        spendFrequencyChart.setDrawGridBackground(false);
        spendFrequencyChart.setTouchEnabled(true);
        spendFrequencyChart.setDragEnabled(true);
        spendFrequencyChart.setScaleEnabled(false);
        spendFrequencyChart.setPinchZoom(false);

        // X-axis styling
        XAxis xAxis = spendFrequencyChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.GRAY);

        // Y-axis styling
        spendFrequencyChart.getAxisLeft().setTextColor(Color.GRAY);
        spendFrequencyChart.getAxisLeft().setDrawGridLines(true);
        spendFrequencyChart.getAxisRight().setEnabled(false);
        spendFrequencyChart.getLegend().setEnabled(false);
    }

    private void loadTransactions() {
        String userId = mAuth.getCurrentUser().getUid();

        // Load all transactions for chart (last 30 days)
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -30);
        long thirtyDaysAgo = calendar.getTimeInMillis();

        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .whereGreaterThan("timestamp", thirtyDaysAgo)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        return;
                    }

                    allTransactions.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot document : value) {
                            Transaction transaction = document.toObject(Transaction.class);
                            transaction.setId(document.getId());
                            allTransactions.add(transaction);
                        }
                    }
                    updateChart();
                });

        // Load recent transactions for list (limit 10)
        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        return;
                    }

                    transactionList.clear();
                    totalIncome = 0;
                    totalExpense = 0;

                    if (value != null) {
                        for (QueryDocumentSnapshot document : value) {
                            Transaction transaction = document.toObject(Transaction.class);
                            transaction.setId(document.getId());
                            transactionList.add(transaction);

                            // Calculate totals
                            if ("Income".equals(transaction.getType())) {
                                totalIncome += transaction.getAmount();
                            } else {
                                totalExpense += transaction.getAmount();
                            }
                        }
                    }

                    // Update UI
                    updateBalanceUI();
                    adapter.notifyDataSetChanged();
                });
    }

    private void updateChart() {
        if (allTransactions.isEmpty()) {
            spendFrequencyChart.clear();
            spendFrequencyChart.invalidate();
            return;
        }

        // Group transactions by day
        Map<String, Float> dailyExpenses = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM", Locale.getDefault());

        for (Transaction transaction : allTransactions) {
            if ("Expense".equals(transaction.getType())) {
                String date = dateFormat.format(transaction.getTimestamp());
                float currentAmount = dailyExpenses.getOrDefault(date, 0f);
                dailyExpenses.put(date, currentAmount + (float) transaction.getAmount());
            }
        }

        // Create entries for chart
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>(dailyExpenses.keySet());

        for (int i = 0; i < labels.size() && i < 7; i++) {
            String label = labels.get(i);
            entries.add(new Entry(i, dailyExpenses.get(label)));
        }

        if (entries.isEmpty()) {
            spendFrequencyChart.clear();
            spendFrequencyChart.invalidate();
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Spending");
        dataSet.setColor(Color.parseColor("#7F3DFF"));
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleColor(Color.parseColor("#7F3DFF"));
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(10f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#E8D5FF"));
        dataSet.setFillAlpha(100);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData lineData = new LineData(dataSet);
        spendFrequencyChart.setData(lineData);

        // Set custom X-axis formatter
        XAxis xAxis = spendFrequencyChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < labels.size()) {
                    return labels.get(index);
                }
                return "";
            }
        });

        spendFrequencyChart.animateX(1000);
        spendFrequencyChart.invalidate();
    }

    private void updateBalanceUI() {
        double balance = totalIncome - totalExpense;

        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);

        tvAccountBalance.setText(formatter.format(balance));
        tvIncome.setText(formatter.format(totalIncome));
        tvExpense.setText(formatter.format(totalExpense));
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload data when fragment becomes visible
        if (mAuth.getCurrentUser() != null) {
            loadTransactions();
        }
    }
}