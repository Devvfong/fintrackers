package com.example.fintracker;

import android.annotation.SuppressLint;
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
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private TextView tvAccountBalance, tvIncome, tvExpense;
    private LineChart spendFrequencyChart;
    private TransactionAdapter adapter;
    private List<Transaction> transactionList;
    private List<Transaction> allTransactions;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private double totalIncome = 0;
    private double totalExpense = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tvAccountBalance = view.findViewById(R.id.tvAccountBalance);
        tvIncome = view.findViewById(R.id.tvIncome);
        tvExpense = view.findViewById(R.id.tvExpense);
        RecyclerView rvRecentTransactions = view.findViewById(R.id.rvRecentTransactions);
        spendFrequencyChart = view.findViewById(R.id.spendFrequencyChart);

        transactionList = new ArrayList<>();
        allTransactions = new ArrayList<>();
        adapter = new TransactionAdapter(getContext(), transactionList);
        rvRecentTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecentTransactions.setAdapter(adapter);

        setupChart();
        loadTransactions();

        return view;
    }

    private void setupChart() {
        spendFrequencyChart.getDescription().setEnabled(false);
        spendFrequencyChart.setDrawGridBackground(false);
        spendFrequencyChart.setDrawBorders(false);
        spendFrequencyChart.setTouchEnabled(true);
        spendFrequencyChart.setDragEnabled(true);
        spendFrequencyChart.setScaleEnabled(false);
        spendFrequencyChart.setPinchZoom(false);

        XAxis xAxis = spendFrequencyChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.parseColor("#91919F"));

        YAxis leftAxis = spendFrequencyChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#F1F1FA"));
        leftAxis.setTextColor(Color.parseColor("#91919F"));
        leftAxis.setDrawAxisLine(false);

        spendFrequencyChart.getAxisRight().setEnabled(false);
        spendFrequencyChart.getLegend().setEnabled(false);
        spendFrequencyChart.setExtraOffsets(10, 10, 10, 10);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadTransactions() {
        assert mAuth.getCurrentUser() != null;
        String userId = mAuth.getCurrentUser().getUid();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -30);
        long thirtyDaysAgo = calendar.getTimeInMillis();

        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .whereGreaterThan("timestamp", thirtyDaysAgo)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

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

        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    transactionList.clear();
                    totalIncome = 0;
                    totalExpense = 0;

                    if (value != null) {
                        for (QueryDocumentSnapshot document : value) {
                            Transaction transaction = document.toObject(Transaction.class);
                            transaction.setId(document.getId());
                            transactionList.add(transaction);

                            if ("Income".equals(transaction.getType())) {
                                totalIncome += transaction.getAmount();
                            } else {
                                totalExpense += transaction.getAmount();
                            }
                        }
                    }

                    updateBalanceUI();
                    adapter.notifyDataSetChanged();
                });
    }

    private void updateChart() {
        if (allTransactions.isEmpty()) {
            spendFrequencyChart.clear();
            return;
        }

        // Get only EXPENSE transactions
        List<Transaction> expenseTransactions = new ArrayList<>();
        for (Transaction transaction : allTransactions) {
            if ("Expense".equals(transaction.getType())) {
                expenseTransactions.add(transaction);
            }
        }

        if (expenseTransactions.isEmpty()) {
            spendFrequencyChart.clear();
            return;
        }

        // Sort by timestamp (oldest to newest)
        expenseTransactions.sort(Comparator.comparingLong(Transaction::getTimestamp));

        // Get last 5 transactions
        int startIndex = Math.max(0, expenseTransactions.size() - 5);
        List<Transaction> lastFive = expenseTransactions.subList(startIndex, expenseTransactions.size());

        // Create entries and labels for each transaction
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        for (int i = 0; i < lastFive.size(); i++) {
            Transaction t = lastFive.get(i);
            entries.add(new Entry(i, (float) t.getAmount()));

            // Label shows time
            String label = timeFormat.format(new Date(t.getTimestamp()));
            labels.add(label);
        }

        // 🎯 CREATE CURVED LINE CONNECTING YOUR LAST 5 EXPENSES
        LineDataSet dataSet = new LineDataSet(entries, "");

        // ✨ SMOOTH CURVE - SET FIRST!
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.2f);

        // Styling
        dataSet.setColor(Color.parseColor("#7F3DFF"));
        dataSet.setLineWidth(3f);

        // ⚪ WHITE CIRCLES = EACH TRANSACTION
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(Color.parseColor("#7F3DFF"));
        dataSet.setCircleRadius(6f);
        dataSet.setCircleHoleRadius(4f);
        dataSet.setCircleHoleColor(Color.WHITE);

        dataSet.setDrawValues(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#E8D5FF"));
        dataSet.setFillAlpha(80);

        LineData lineData = new LineData(dataSet);
        spendFrequencyChart.setData(lineData);

        // Update labels
        spendFrequencyChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        spendFrequencyChart.getXAxis().setLabelCount(labels.size(), true);

        spendFrequencyChart.animateX(1200);
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
        if (mAuth.getCurrentUser() != null) {
            loadTransactions();
        }
    }
}
