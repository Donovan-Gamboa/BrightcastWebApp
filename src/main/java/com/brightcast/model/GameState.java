package com.brightcast.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GameState {
    private final String gameId;
    private final Player player1;
    private Player player2;
    private int currentPlayerIndex;
    private String status;
    private String winnerName;
    private CardType pendingCard;

    private Integer pendingTargetIndex;
    private String turnPhase;

    private List<String> logs = new ArrayList<>();

    public GameState(Player player1, Player player2) {
        this.gameId = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        this.player1 = player1;
        this.player2 = player2;
        this.currentPlayerIndex = 0;
        this.status = "WAITING_FOR_PLAYER";
        this.turnPhase = "DRAW";
    }

    public void addLog(String message) {
        logs.add(0, message);
        if (logs.size() > 50) logs.remove(logs.size() - 1);
    }

    public List<String> getLogs() { return logs; }

    public void setPlayer2(Player p2) { this.player2 = p2; this.status = "PLAYING"; }
    public void setStatus(String status) { this.status = status; }
    public Player getCurrentPlayer() { return currentPlayerIndex == 0 ? player1 : player2; }
    public Player getOpponent() { return currentPlayerIndex == 0 ? player2 : player1; }
    public void switchTurn(){
        currentPlayerIndex = (currentPlayerIndex == 0 ) ? 1 : 0;
        this.turnPhase = "DRAW";
    }
    public void setWinner(String winnerName) { this.winnerName = winnerName; this.status = "FINISHED"; }
    public String getGameId() { return gameId; }
    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }
    public int getCurrentPlayerIndex() { return currentPlayerIndex; }
    public String getStatus() { return status; }
    public String getWinnerName() { return winnerName; }
    public CardType getPendingCard() { return pendingCard; }
    public void setPendingCard(CardType pendingCard) { this.pendingCard = pendingCard; }
    public Integer getPendingTargetIndex() { return pendingTargetIndex; }
    public void setPendingTargetIndex(Integer pendingTargetIndex) { this.pendingTargetIndex = pendingTargetIndex; }
    public String getTurnPhase() { return turnPhase; }
    public void setTurnPhase(String turnPhase) { this.turnPhase = turnPhase; }
}