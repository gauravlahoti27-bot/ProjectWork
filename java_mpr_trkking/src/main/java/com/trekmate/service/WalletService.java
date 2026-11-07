package com.trekmate.service;

import com.trekmate.model.User;
import com.trekmate.model.WalletTransaction;
import com.trekmate.repository.UserRepository;
import com.trekmate.repository.WalletTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WalletService {

    private final UserRepository userRepository;
    private final WalletTransactionRepository transactionRepository;

    @Autowired
    public WalletService(UserRepository userRepository, WalletTransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    public BigDecimal getBalance(Long userId) {
        return userRepository.findById(userId)
                .map(User::getWalletBalance)
                .orElse(BigDecimal.ZERO);
    }

    public boolean hasSufficientBalance(Long userId, BigDecimal amountRequired) {
        BigDecimal balance = getBalance(userId);
        return balance.compareTo(amountRequired) >= 0;
    }

    public BigDecimal getDeficitAmount(Long userId, BigDecimal amountRequired) {
        BigDecimal balance = getBalance(userId);
        BigDecimal diff = amountRequired.subtract(balance);
        return diff.compareTo(BigDecimal.ZERO) > 0 ? diff : BigDecimal.ZERO;
    }

    @Transactional
    public WalletTransaction allocateDemoCredits(Long userId, BigDecimal amount, String reason) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Allocation amount must be greater than zero");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        BigDecimal newBalance = user.getWalletBalance().add(amount);
        user.setWalletBalance(newBalance);
        userRepository.save(user);

        String refCode = "TOPUP-" + System.currentTimeMillis();
        WalletTransaction tx = new WalletTransaction(
                user,
                "CREDIT",
                amount,
                newBalance,
                reason != null && !reason.trim().isEmpty() ? reason : "Demo Credit Top-up",
                refCode
        );
        return transactionRepository.save(tx);
    }

    @Transactional
    public WalletTransaction deductCredits(Long userId, BigDecimal amount, String bookingCode, String description) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (user.getWalletBalance().compareTo(amount) < 0) {
            BigDecimal deficit = amount.subtract(user.getWalletBalance());
            throw new IllegalStateException(String.format(
                    "Insufficient virtual wallet balance. You need ₹%,.2f more credits to complete this booking.",
                    deficit
            ));
        }

        BigDecimal newBalance = user.getWalletBalance().subtract(amount);
        user.setWalletBalance(newBalance);
        userRepository.save(user);

        WalletTransaction tx = new WalletTransaction(
                user,
                "DEBIT",
                amount,
                newBalance,
                description != null ? description : "Payment for Booking: " + bookingCode,
                bookingCode
        );
        return transactionRepository.save(tx);
    }

    @Transactional
    public WalletTransaction refundCredits(Long userId, BigDecimal amount, String bookingCode, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        BigDecimal newBalance = user.getWalletBalance().add(amount);
        user.setWalletBalance(newBalance);
        userRepository.save(user);

        WalletTransaction tx = new WalletTransaction(
                user,
                "CREDIT",
                amount,
                newBalance,
                reason != null ? reason : "Refund for Booking: " + bookingCode,
                "REFUND-" + bookingCode
        );
        return transactionRepository.save(tx);
    }

    public List<WalletTransaction> getTransactionHistory(Long userId) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}

