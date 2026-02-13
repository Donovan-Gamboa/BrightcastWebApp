package com.brightcast.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DeckTest {

    @Test
    void deckShouldHave34Cards () {
        Deck deck =  new Deck();
        assertEquals(34, deck.size(), "Deck size not correct");
    }

    @Test
    void deckShouldShuffle(){
        Deck deck1 =  new Deck();
        Deck deck2 =  new Deck();
        assertNotEquals(deck1.draw(), deck2.draw(), "Decks should be randomized");
    }
}