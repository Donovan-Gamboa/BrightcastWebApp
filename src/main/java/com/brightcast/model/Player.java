package com.brightcast.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Player {
    private final String name;
    private final List<CardType> hand = new ArrayList<>();
    private final List<CardInstance> board = new ArrayList<>();
    private final Stack<CardType> discardPile = new Stack<>();
    private final Deck deck;

    public Player(String name) {
        this.name = name;
        this.deck = new Deck();
    }

    public void drawCard() {
        if (deck.isEmpty()) {
            reshuffleDiscardIntoDeck();
        }

        if (!deck.isEmpty()) {
            hand.add(deck.draw());
        }
    }

    private void reshuffleDiscardIntoDeck() {
        while (!discardPile.isEmpty()) {
            deck.add(discardPile.pop());
        }
        deck.shuffle();
    }

    public void playToBoard (CardType card) {
        if(hand.remove(card)) {
            board.add(new CardInstance(card));
        } else {
            throw new IllegalArgumentException();
        }
    }

    public void discardFromHand(CardType card) {
        if(hand.remove(card)) {
            discardPile.push(card);
        }
    }

    public void discardFromBoard(CardInstance card) {
        if(board.remove(card)) {
            discardPile.push(card.getOriginalCard());
        }
    }

    public void playAlchemistToBoard(CardType copiedType) {
        if(hand.remove(CardType.ALCHEMIST)) {
            CardInstance instance = new CardInstance(CardType.ALCHEMIST);
            instance.morph(copiedType);            board.add(instance);
        }
    }

    public void addCardToHand(CardType card) {
        this.hand.add(card);
    }

    public int getHandSize() {
        return hand.size();
    }

    public List<CardType> getHand() {
        return new ArrayList<>(hand);
    }

    public List<CardInstance> getBoard() {
        return new  ArrayList<>(board);
    }

    public Stack<CardType> getDiscardPile() {
        return discardPile;
    }

    public String getName() {
        return name;
    }

    public Deck getDeck() {
        return deck;
    }
}
