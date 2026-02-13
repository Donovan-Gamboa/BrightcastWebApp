package com.brightcast.controller;

import com.brightcast.model.GameState;
import com.brightcast.service.GameService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class GameSocketController {
    private final GameService gameService;
    public GameSocketController(GameService gameService) {
        this.gameService = gameService;
    }

    @MessageMapping("/game/{gameId}/play")
    @SendTo("/topic/game/{gameId}")
    public GameState playCard(@DestinationVariable String gameId, MoveRequest move) {
        return gameService.playCard(gameId, move);
    }

    @MessageMapping("/game/{gameId}/draw")
    @SendTo("/topic/game/{gameId}")
    public GameState drawCard(@DestinationVariable String gameId, MoveRequest move) {
        return gameService.drawCard(gameId, move.getPlayerName());
    }

    @MessageMapping("/game/{gameId}/interrupt")
    @SendTo("/topic/game/{gameId}")
    public GameState resolveInterrupt(@DestinationVariable String gameId, boolean interrupt) {
        return gameService.resolveInterrupt(gameId, interrupt);
    }

    @MessageMapping("/game/{gameId}/discard")
    @SendTo("/topic/game/{gameId}")
    public GameState discardCard(@DestinationVariable String gameId, MoveRequest move) {
        return gameService.discardCard(gameId, move.getPlayerName(), move.getCardIndex());
    }

    @MessageMapping("/game/{gameId}/skip")
    @SendTo("/topic/game/{gameId}")
    public GameState skipTurn(@DestinationVariable String gameId, MoveRequest move) {
        return gameService.skipTurn(gameId, move.getPlayerName());
    }

    public static class MoveRequest {
        private String playerName;
        private int cardIndex;

        private Integer targetIndex;
        private java.util.List<Integer> targetIndices;

        public String getPlayerName() { return playerName; }
        public void setPlayerName(String playerName) { this.playerName = playerName; }
        public int getCardIndex() { return cardIndex; }
        public void setCardIndex(int cardIndex) { this.cardIndex = cardIndex; }

        public Integer getTargetIndex() { return targetIndex; }
        public void setTargetIndex(Integer targetIndex) { this.targetIndex = targetIndex; }
        public java.util.List<Integer> getTargetIndices() { return targetIndices; }
        public void setTargetIndices(java.util.List<Integer> targetIndices) { this.targetIndices = targetIndices; }
    }
}