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

@SuppressWarnings("ALL")
public class HomeFragment extends Fragment {

    private TextView tvAccountBalance, tvIncome, tvExpense;
    private LineChart spendFrequencyChart;
    private TransactionAdapter adapter;

    private final List<Transaction> recentTransactions = new ArrayList<>();
    private final List<Transaction> allTransactions = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private double totalIncome = 0;
    private double totalExpense = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tvAccountBalance = view.findViewById(R.id.tvAccountBalance);
        tvIncome = view.findViewById(R.id.tvIncome);
        tvExpense = view.findViewById(R.id.tvExpense);
        spendFrequencyChart = view.findViewById(R.id.spendFrequencyChart);
        RecyclerView rvRecentTransactions = view.findViewById(R.id.rvRecentTransactions);

        rvRecentTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecentTransactions.setNestedScrollingEnabled(false);

        // ✅ FIXED: Uses 2-param constructor (backward compatible)
        adapter = new TransactionAdapter(
        requireContext(),
        recentTransactions,
        null   // no click listener on home
);
        rvRecentTransactions.setAdapter(adapter);

        setupChart();
        if (mAuth.getCurrentUser() != null) {
            loadTransactions();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadTransactions() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();

        // 1) Chart data (last 30 days expenses)
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
                            Transaction t = document.toObject(Transaction.class);
                            t.setId(document.getId());
                            allTransactions.add(t);
                        }
                    }
                    updateChart();
                });

        // 2) Recent list (TOP 5 newest) + totals
        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(5)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    recentTransactions.clear();
                    totalIncome = 0;
                    totalExpense = 0;

                    if (value != null) {
                        for (QueryDocumentSnapshot document : value) {
                            Transaction t = document.toObject(Transaction.class);
                            t.setId(document.getId());
                            recentTransactions.add(t);

                            if ("Income".equals(t.getType())) {
                                totalIncome += t.getAmount();
                            } else {
                                totalExpense += t.getAmount();
                            }
                        }
                    }

                    // ✅ FIXED: Uses updateTransactions() method
                    adapter.updateTransactions(recentTransactions);
                    updateBalanceUI();
                });
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

    private void updateChart() {
        if (allTransactions.isEmpty()) {
            spendFrequencyChart.clear();
            return;
        }

        // Only expense transactions for chart
        List<Transaction> expenseTransactions = new ArrayList<>();
        for (Transaction t : allTransactions) {
            if ("Expense".equals(t.getType())) {
                expenseTransactions.add(t);
            }
        }

        if (expenseTransactions.isEmpty()) {
            spendFrequencyChart.clear();
            return;
        }

        expenseTransactions.sort(Comparator.comparingLong(Transaction::getTimestamp));
        int startIndex = Math.max(0, expenseTransactions.size() - 5);
        List<Transaction> lastFive = expenseTransactions.subList(startIndex, expenseTransactions.size());

        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        for (int i = 0; i < lastFive.size(); i++) {
            Transaction t = lastFive.get(i);
            entries.add(new Entry(i, (float) t.getAmount()));
            labels.add(timeFormat.format(new Date(t.getTimestamp())));
        }

        LineDataSet dataSet = new LineDataSet(entries, "");
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.2f);
        dataSet.setColor(Color.parseColor("#7F3DFF"));
        dataSet.setLineWidth(3f);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(Color.parseColor("#7F3DFF"));
        dataSet.setCircleRadius(6f);
        dataSet.setCircleHoleRadius(4f);
        dataSet.setCircleHoleColor(Color.WHITE);
        dataSet.setDrawValues(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#E8D5FF"));
        dataSet.setFillAlpha(80);

        spendFrequencyChart.setData(new LineData(dataSet));
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
