package com.example.fintracker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("ALL")
public class TransactionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_DATE_HEADER = 0;
    private static final int TYPE_TRANSACTION = 1;

    public interface Listener {
        void onEditClick(@NonNull Transaction transaction);
        void onRowClick(@NonNull Transaction transaction);
    }

    @NonNull private final Context context;
    @NonNull private final List<TransactionListItem> itemList = new ArrayList<>();
    @NonNull private final List<Transaction> transactionList;
    @NonNull private final Listener listener;
    private final boolean showEditButton;

    // Constructor with edit button control
    public TransactionAdapter(@NonNull Context context,
                              @NonNull List<Transaction> transactionList,
                              @NonNull Listener listener,
                              boolean showEditButton) {
        this.context = context;
        this.transactionList = transactionList;
        this.listener = listener;
        this.showEditButton = showEditButton;
        groupByDate(transactionList);
    }

    // Default constructor (shows edit button)
    public TransactionAdapter(@NonNull Context context,
                              @NonNull List<Transaction> transactionList,
                              @NonNull Listener listener) {
        this(context, transactionList, listener, true);
    }

    public void updateTransactions(@NonNull List<Transaction> transactions) {
        itemList.clear();
        groupByDate(transactions);
        notifyDataSetChanged();
    }

    private void groupByDate(@NonNull List<Transaction> transactions) {
        Map<String, List<Transaction>> grouped = new LinkedHashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());

        for (Transaction t : transactions) {
            String dateKey = dateFormat.format(new Date(t.getTimestamp()));
            grouped.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(t);
        }

        for (Map.Entry<String, List<Transaction>> entry : grouped.entrySet()) {
            itemList.add(new TransactionListItem(entry.getKey()));
            for (Transaction t : entry.getValue()) {
                itemList.add(new TransactionListItem(t));
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        return itemList.get(position).isHeader() ? TYPE_DATE_HEADER : TYPE_TRANSACTION;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_DATE_HEADER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_date_header, parent, false);
            return new DateHeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
            return new TransactionViewHolder(view);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        TransactionListItem item = itemList.get(position);

        if (holder instanceof DateHeaderViewHolder) {
            ((DateHeaderViewHolder) holder).tvDateHeader.setText(item.getDateHeader());
            return;
        }

        // Your original transaction binding code
        TransactionViewHolder vh = (TransactionViewHolder) holder;
        Transaction transaction = item.getTransaction();

        vh.tvCategory.setText(transaction.getCategory());
        String desc = transaction.getDescription();
        if (desc != null && !desc.trim().isEmpty()) {
            vh.tvDescription.setText(desc);
            vh.tvDescription.setVisibility(View.VISIBLE);
        } else {
            vh.tvDescription.setText("");
            vh.tvDescription.setVisibility(View.GONE);
        }

        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
        boolean isIncome = "Income".equals(transaction.getType());
        vh.tvAmount.setText((isIncome ? "+ " : "- ") + formatter.format(transaction.getAmount()));
        vh.tvAmount.setTextColor(ContextCompat.getColor(
                context,
                isIncome ? android.R.color.holo_green_dark : android.R.color.holo_red_dark
        ));

        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        vh.tvTime.setText(timeFormat.format(new Date(transaction.getTimestamp())));

        setCategoryIcon(vh, transaction.getCategory());

        // Show/hide edit button
        if (showEditButton) {
            vh.btnEdit.setVisibility(View.VISIBLE);
            vh.btnEdit.setOnClickListener(v -> listener.onEditClick(transaction));
        } else {
            vh.btnEdit.setVisibility(View.GONE);
        }

        vh.itemView.setOnClickListener(v -> listener.onRowClick(transaction));
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    private void setCategoryIcon(@NonNull TransactionViewHolder holder, @NonNull String category) {
        int iconRes;
        int bgColor;
        switch (category) {
            case "Salary":
                iconRes = R.drawable.attach_money_24px;
                bgColor = 0xFF4CAF50;
                break;
            case "Food":
                iconRes = R.drawable.award_meal_24px;
                bgColor = 0xFFFF9800;
                break;
            case "Shopping":
                iconRes = R.drawable.shopping_bag_24px;
                bgColor = 0xFF2196F3;
                break;
            case "Subscription":
                iconRes = R.drawable.subscriptions_24px;
                bgColor = 0xFF9C27B0;
                break;
            case "Transport":
                iconRes = R.drawable.emoji_transportation_24px;
                bgColor = 0xFF9C27B0;
                break;
            default:
                iconRes = R.drawable.other_admission_24px;
                bgColor = 0xFF607D8B;
                break;
        }

        holder.ivCategoryIcon.setImageResource(iconRes);
        holder.ivCategoryIcon.setColorFilter(0xFFFFFFFF);
        holder.cvCategoryIcon.setCardBackgroundColor(bgColor);
    }

    static class DateHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvDateHeader;
        DateHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateHeader = itemView.findViewById(R.id.tvDateHeader);
        }
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvDescription, tvAmount, tvTime;
        CardView cvCategoryIcon;
        ImageView ivCategoryIcon;
        ImageButton btnEdit;
        TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvTime = itemView.findViewById(R.id.tvTime);
            cvCategoryIcon = itemView.findViewById(R.id.cvCategoryIcon);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
            btnEdit = itemView.findViewById(R.id.btnEdit);
        }
    }
}
