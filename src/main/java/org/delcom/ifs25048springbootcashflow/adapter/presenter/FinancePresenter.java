package org.delcom.ifs25048springbootcashflow.adapter.presenter;

import org.delcom.ifs25048springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25048springbootcashflow.domain.entity.TransactionType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.Locale;

public class FinancePresenter {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public String formatAmount(long amount) {

        NumberFormat formatter =
                NumberFormat.getNumberInstance(Locale.US);

        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);

        return "Rp " + formatter.format(amount);
    }

    public String formatCurrency(long amount) {

        return formatAmount(amount);
    }

    public String formatDateTime(LocalDateTime dateTime) {

        if (dateTime == null) {
            return "-";
        }

        return dateTime.format(DATE_FORMAT);
    }

    public String formatType(TransactionType type) {

        if (type == null) {
            return "-";
        }

        return type.label();
    }

    public String formatTransaction(Transaction transaction) {

        return "[" + transaction.getId() + "] "
                + transaction.getDescription()
                + " | "
                + formatAmount(transaction.getAmount())
                + " | ["
                + formatType(transaction.getType())
                + "]";
    }

    public String formatBalance(long balance) {

        return "Saldo: " + formatAmount(balance);
    }

    public String successAdd(Transaction transaction) {

        return "Berhasil menambah transaksi: "
                + formatTransaction(transaction);
    }

    public String successDelete(int id) {

        return "Berhasil menghapus transaksi [" + id + "]";
    }

    public String successGenerate(int count) {

        return "Berhasil menambahkan "
                + count
                + " transaksi hasil AI";
    }

    public String errorMessage(Exception exception) {

        if (exception == null
                || exception.getMessage() == null) {

            return "Terjadi kesalahan";
        }

        return exception.getMessage();
    }
}