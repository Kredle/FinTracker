package com.example.fintracker.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fintracker.R;
import com.example.fintracker.dal.local.database.AppDatabase;
import com.example.fintracker.dal.local.entities.TagEntity;
import com.example.fintracker.dal.local.entities.TransactionEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * TransactionAdapter — для відображення списку транзакцій у RecyclerView.
 * Показує назву, суму та дату транзакції.
 */
public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private List<TransactionEntity> transactions = new ArrayList<>();
    private com.example.fintracker.dal.local.entities.AccountEntity account;

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        TransactionEntity transaction = transactions.get(position);
        holder.bind(transaction, account);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public void setTransactions(List<TransactionEntity> transactions) {
        this.transactions = transactions;
        this.account = null;
        notifyDataSetChanged();
    }

    public void setTransactions(List<TransactionEntity> transactions, com.example.fintracker.dal.local.entities.AccountEntity account) {
        this.transactions = transactions;
        this.account = account;
        notifyDataSetChanged();
    }

    /**
     * ViewHolder для кожної транзакції
     */
    static class TransactionViewHolder extends RecyclerView.ViewHolder {

        private final TextView transactionTitle;
        private final TextView transactionAmount;
        private final TextView transactionDate;
        private final TextView transactionTag;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            transactionTitle = itemView.findViewById(R.id.transaction_title);
            transactionAmount = itemView.findViewById(R.id.transaction_amount);
            transactionDate = itemView.findViewById(R.id.transaction_date);
            transactionTag = itemView.findViewById(R.id.transaction_tag);
        }

        public void bind(TransactionEntity transaction, com.example.fintracker.dal.local.entities.AccountEntity account) {
            // For shared accounts, show user name for all transactions
            String title = transaction.title;
            if (account != null && account.isShared) {
                try {
                    com.example.fintracker.dal.local.entities.UserEntity user = AppDatabase.getInstance(itemView.getContext().getApplicationContext())
                            .userDao().getUserByIdSync(transaction.userId);
                    if (user != null) {
                        title = transaction.title + " (" + user.name + ")";
                    }
                } catch (Exception e) {
                    // If user lookup fails, just show the title
                    title = transaction.title;
                }
            }
            transactionTitle.setText(title);
            
            transactionAmount.setText(String.format("%.2f ₴", transaction.amount));
            transactionDate.setText(formatDateTime(transaction.timestamp));
            
            // Resolve tag name from tagId
            String tagName = "Без тегу";
            if (transaction.tagId != null && !transaction.tagId.isEmpty()) {
                try {
                    TagEntity tag = AppDatabase.getInstance(itemView.getContext().getApplicationContext()).tagDao().getTagByIdSync(transaction.tagId);
                    if (tag != null) {
                        tagName = tag.name;
                    }
                } catch (Exception e) {
                    // If tag lookup fails, show the tagId as fallback
                    tagName = transaction.tagId;
                }
            }
            transactionTag.setText(tagName);
            
            // Change color based on transaction type
            if ("EXPENSE".equals(transaction.type)) {
                transactionAmount.setTextColor(itemView.getContext().getColor(android.R.color.holo_red_light));
            } else {
                transactionAmount.setTextColor(itemView.getContext().getColor(android.R.color.holo_green_light));
            }
        }

        private String formatDateTime(String timestamp) {
            if (timestamp == null || timestamp.isEmpty()) {
                return "";
            }
            try {
                // Parse ISO format: 2026-03-29T11:35:39Z
                java.text.SimpleDateFormat isoFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US);
                java.util.Date date = isoFormat.parse(timestamp);
                
                // Format to desired format: 11:35 29.03.2026
                java.text.SimpleDateFormat displayFormat = new java.text.SimpleDateFormat("HH:mm dd.MM.yyyy", java.util.Locale.US);
                return displayFormat.format(date);
            } catch (Exception e) {
                // If parsing fails, return original
                return timestamp;
            }
        }
    }
}
