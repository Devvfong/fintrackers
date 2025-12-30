package com.example.fintracker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private final Context context;
    private final List<Transaction> transactionList;

    public TransactionAdapter(Context context, List<Transaction> transactionList) {
        this.context = context;
        this.transactionList = transactionList;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactionList.get(position);

        holder.tvCategory.setText(transaction.getCategory());

        String desc = transaction.getDescription();
        if (desc != null && !desc.trim().isEmpty()) {
            holder.tvDescription.setText(desc);
            holder.tvDescription.setVisibility(View.VISIBLE);
        } else {
            holder.tvDescription.setVisibility(View.GONE);
        }

        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);

        String amountText;
        if ("Income".equals(transaction.getType())) {
            amountText = "+ " + formatter.format(transaction.getAmount());
            holder.tvAmount.setTextColor(context.getResources().getColor(R.color.green_income));
        } else {
            amountText = "- " + formatter.format(transaction.getAmount());
            holder.tvAmount.setTextColor(context.getResources().getColor(R.color.red_expense));
        }
        holder.tvAmount.setText(amountText);

        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        holder.tvTime.setText(timeFormat.format(new Date(transaction.getTimestamp())));

        setCategoryIcon(holder, transaction.getCategory());
    }

    private void setCategoryIcon(TransactionViewHolder holder, String category) {
        int iconRes;
        int bgColor;

        switch (category) {
            case "Shopping":
                iconRes = R.drawable.ic_shopping;
                bgColor = R.color.category_shopping;
                break;
            case "Subscription":
                iconRes = R.drawable.ic_subscription;
                bgColor = R.color.category_subscription;
                break;
            case "Food":
                iconRes = R.drawable.ic_food;
                bgColor = R.color.category_food;
                break;
            case "Transport":
                iconRes = R.drawable.ic_shopping; // TODO replace with transport icon
                bgColor = R.color.category_transport;
                break;
            case "Salary":
                iconRes = R.drawable.ic_income;
                bgColor = R.color.green_income;
                break;
            default:
                iconRes = R.drawable.ic_shopping;
                bgColor = R.color.purple_primary;
                break;
        }

        holder.ivCategoryIcon.setImageResource(iconRes);
        holder.cvCategoryIcon.setCardBackgroundColor(context.getResources().getColor(bgColor));
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        CardView cvCategoryIcon;
        ImageView ivCategoryIcon;
        TextView tvCategory, tvDescription, tvAmount, tvTime;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            cvCategoryIcon = itemView.findViewById(R.id.cvCategoryIcon);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}
