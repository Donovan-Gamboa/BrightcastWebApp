package com.brightcast.model;

import java.util.Collections;
import java.util.Stack;

public class Deck {
    private final Stack<CardType> cards = new Stack<>();

    public Deck() {
        reset();
    }

    public void reset() {
        cards.clear();
        for (CardType cardType : CardType.values()) {
            for (int i = 0; i < cardType.getDeckCount(); i++) {
                cards.push(cardType);
            }
        }
        shuffle();
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

    public CardType draw() {
        if (cards.empty()) {
            throw new IllegalStateException("No cards in Deck");
        }
        return cards.pop();
    }

    public int size(){
        return cards.size();
    }

    public boolean isEmpty(){
        return cards.isEmpty();
    }

    public void add(CardType cardType){
        cards.push(cardType);
    }

    public void addAll(Stack<CardType> pile){
        cards.addAll(pile);
    }
}
