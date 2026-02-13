package com.brightcast.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    @Test
    void playerShouldDrawAndPlay(){
        Player player = new Player("Donovan");

        for (int i = 0; i < 4; i++ ) {
            player.drawCard();
        }

        assertEquals(4, player.getHandSize());

        CardType cardToPlay = player.getHand().get(0);
        player.playToBoard(cardToPlay);

        assertEquals(3,  player.getHandSize());
        assertEquals(1, player.getBoard().size());
        assertEquals(cardToPlay, player.getBoard().get(0));
    }
}