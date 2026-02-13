package com.brightcast.model;

public class CardInstance {
    private CardType currentCard;    private final CardType originalCard;
    public CardInstance(CardType card) {
        this.currentCard = card;
        this.originalCard = card;
    }

    public void morph(CardType newType) {
        this.currentCard = newType;
    }

    public CardType getCurrentCard() {
        return currentCard;
    }

    public CardType getOriginalCard() {
        return originalCard;
    }

    public String toString() {
        return currentCard.toString();
    }
}