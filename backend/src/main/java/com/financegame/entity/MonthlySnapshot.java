package com.financegame.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "monthly_snapshots")
public class MonthlySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "turn", nullable = false)
    private int turn;

    @Column(name = "cash", nullable = false, precision = 15, scale = 2)
    private BigDecimal cash;

    @Column(name = "net_worth", nullable = false, precision = 15, scale = 2)
    private BigDecimal netWorth;

    @Column(name = "total_income", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalIncome = BigDecimal.ZERO;

    @Column(name = "total_expenses", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalExpenses = BigDecimal.ZERO;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDateTime snapshotDate;

    @PrePersist
    protected void onCreate() { snapshotDate = LocalDateTime.now(); }

    public MonthlySnapshot() {}

    public MonthlySnapshot(Long playerId, int turn, BigDecimal cash, BigDecimal netWorth,
                           BigDecimal totalIncome, BigDecimal totalExpenses) {
        this.playerId = playerId;
        this.turn = turn;
        this.cash = cash;
        this.netWorth = netWorth;
        this.totalIncome = totalIncome;
        this.totalExpenses = totalExpenses;
    }

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }
    public int getTurn() { return turn; }
    public void setTurn(int turn) { this.turn = turn; }
    public BigDecimal getCash() { return cash; }
    public void setCash(BigDecimal cash) { this.cash = cash; }
    public BigDecimal getNetWorth() { return netWorth; }
    public void setNetWorth(BigDecimal netWorth) { this.netWorth = netWorth; }
    public BigDecimal getTotalIncome() { return totalIncome; }
    public void setTotalIncome(BigDecimal v) { this.totalIncome = v; }
    public BigDecimal getTotalExpenses() { return totalExpenses; }
    public void setTotalExpenses(BigDecimal v) { this.totalExpenses = v; }
    public LocalDateTime getSnapshotDate() { return snapshotDate; }
}
