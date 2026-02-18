package com.brightcast.service;

import com.brightcast.controller.GameSocketController;
import com.brightcast.model.CardInstance;
import com.brightcast.model.CardType;
import com.brightcast.model.GameState;
import com.brightcast.model.Player;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class GameService {

    private final Map<String, GameState> activeGames = new ConcurrentHashMap<>();
    private final Map<String, GameSocketController.MoveRequest> pendingMoves = new ConcurrentHashMap<>();

    private final SimpMessagingTemplate messagingTemplate;

    public GameService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public GameState createGame(String p1Name) {
        Player p1 = new Player(p1Name);
        GameState game = new GameState(p1, null);
        activeGames.put(game.getGameId(), game);
        return game;
    }

    public GameState joinGame(String gameId, String p2Name) {
        GameState game = activeGames.get(gameId);
        if (game == null) throw new IllegalArgumentException("Game not found");
        if (game.getPlayer2() != null) throw new IllegalArgumentException("Game full");

        game.setPlayer2(new Player(p2Name));
        game.addLog(p2Name + " joined the game!");

        randomizeStart(game);

        messagingTemplate.convertAndSend("/topic/game/" + gameId, game);

        return game;
    }

    private void randomizeStart(GameState game) {
        Player p1 = game.getPlayer1();
        Player p2 = game.getPlayer2();

        if (Math.random() < 0.5) {
            game.switchTurn();
            game.addLog(p2.getName() + " goes first!");
        } else {
            game.addLog(p1.getName() + " goes first!");
        }

        for (int i = 0; i < 4; i++) {
            p1.drawCard();
            p2.drawCard();
        }
        if (!p1.getDeck().isEmpty()) p1.getDiscardPile().push(p1.getDeck().draw());
        if (!p2.getDeck().isEmpty()) p2.getDiscardPile().push(p2.getDeck().draw());
    }

    public GameState getGame(String gameId) { return activeGames.get(gameId); }

    public GameState drawCard(String gameId, String playerName) {
        GameState game = activeGames.get(gameId);
        if (game == null) return null;

        if (!game.getStatus().equals("PLAYING") && !game.getStatus().equals("WAITING_FOR_PLAYER")) {
            throw new IllegalStateException("Finish your current action first!");
        }

        Player currentPlayer = game.getCurrentPlayer();
        if (!currentPlayer.getName().equals(playerName)) throw new IllegalArgumentException("Not your turn!");
        if (!"DRAW".equals(game.getTurnPhase())) throw new IllegalStateException("Already drawn!");

        currentPlayer.drawCard();
        game.addLog(playerName + " drew a card.");
        game.setTurnPhase("MAIN");
        return game;
    }

    public GameState skipTurn(String gameId, String playerName) {
        GameState game = activeGames.get(gameId);
        Player currentPlayer = game.getCurrentPlayer();

        if (!game.getStatus().equals("PLAYING")) {
            throw new IllegalStateException("You cannot skip turn right now! Complete your action (Discard/Interrupt).");
        }

        if (!currentPlayer.getName().equals(playerName)) throw new IllegalArgumentException("Not your turn!");
        if (!"MAIN".equals(game.getTurnPhase())) throw new IllegalStateException("Draw first!");

        currentPlayer.drawCard();
        game.addLog(playerName + " Skipped & Drew.");
        endTurnOrForceDiscard(game);
        return game;
    }

    public GameState playCard(String gameId, GameSocketController.MoveRequest request) {
        GameState game = activeGames.get(gameId);

        if (!game.getStatus().equals("PLAYING")) throw new IllegalStateException("Game is paused or waiting for action.");
        if ("DRAW".equals(game.getTurnPhase())) throw new IllegalStateException("Draw first!");

        Player currentPlayer = game.getCurrentPlayer();
        Player opponent = game.getOpponent();

        if (!currentPlayer.getName().equals(request.getPlayerName())) throw new IllegalArgumentException("Not your turn");

        CardType card = currentPlayer.getHand().get(request.getCardIndex());

        if(card != CardType.ALCHEMIST) {
            game.addLog(currentPlayer.getName() + " played " + card + ".");
        }

        if (card == CardType.ALCHEMIST) {
            if (request.getTargetIndex() == null) throw new IllegalArgumentException("Target required");
            CardInstance cardToCopy = currentPlayer.getBoard().get(request.getTargetIndex());
            CardType copiedType = cardToCopy.getCurrentCard();

            game.addLog(currentPlayer.getName() + " played Alchemist (copying " + copiedType + ").");
            currentPlayer.playAlchemistToBoard(copiedType);

            if (canInterrupt(opponent, CardType.ALCHEMIST)) {
                return triggerInterrupt(game, request, CardType.ALCHEMIST);
            }
            return executeCardEffect(game, copiedType, request);
        }

        currentPlayer.playToBoard(card);

        if (canInterrupt(opponent, card)) {
            return triggerInterrupt(game, request, card);
        }

        return executeCardEffect(game, card, request);
    }

    private void endTurnOrForceDiscard(GameState game) {
        if (game.getCurrentPlayer().getHandSize() > 8) {
            game.setStatus("WAITING_FOR_DISCARD");
            game.addLog(game.getCurrentPlayer().getName() + " must discard down to 8.");
        } else {
            game.setStatus("PLAYING");
            game.switchTurn();
        }
    }

    public GameState discardCard(String gameId, String playerName, int cardIndex) {
        GameState game = activeGames.get(gameId);
        Player currentPlayer = game.getCurrentPlayer();

        if (!game.getStatus().equals("WAITING_FOR_DISCARD")) throw new IllegalStateException("Not discarding mode");

        if (cardIndex >= 0 && cardIndex < currentPlayer.getHandSize()) {
            CardType c = currentPlayer.getHand().get(cardIndex);
            currentPlayer.discardFromHand(c);
            game.addLog(playerName + " discarded " + c + ".");
        }

        if (checkWinCondition(currentPlayer.getBoard())) {
            game.setWinner(currentPlayer.getName());
            game.addLog("GAME OVER! " + currentPlayer.getName() + " wins!");
        } else {
            endTurnOrForceDiscard(game);
        }
        return game;
    }

    private GameState executeCardEffect(GameState game, CardType effectiveCard, GameSocketController.MoveRequest request) {
        Player currentPlayer = game.getCurrentPlayer();
        Player opponent = game.getOpponent();
        boolean actionSuccessful = true;

        switch (effectiveCard) {
            case WIZARD:
                if (!currentPlayer.getDeck().isEmpty()) {
                    currentPlayer.drawCard();
                    game.addLog(currentPlayer.getName() + " drew 1 card (Wizard).");
                }
                break;
            case SAGE:
                currentPlayer.drawCard(); currentPlayer.drawCard();
                game.addLog(currentPlayer.getName() + " drew 2 cards (Sage).");
                game.setStatus("WAITING_FOR_DISCARD");
                return game;
            case SORCERER:
                if (request.getTargetIndex() != null && request.getTargetIndex() < opponent.getBoard().size()) {
                    CardInstance target = opponent.getBoard().get(request.getTargetIndex());
                    opponent.discardFromBoard(target);
                    game.addLog(currentPlayer.getName() + " destroyed " + target.getCurrentCard() + "!");
                }
                break;
            case DRAGON:
                if (!currentPlayer.getBoard().isEmpty()) {
                    CardInstance dragonInstance = currentPlayer.getBoard().get(currentPlayer.getBoard().size() - 1);
                    currentPlayer.discardFromBoard(dragonInstance);
                }
                if (request.getTargetIndices() != null && !request.getTargetIndices().isEmpty()) {
                    int count = 0;
                    List<Integer> sortedIndices = request.getTargetIndices().stream()
                            .sorted(java.util.Comparator.reverseOrder()).toList();

                    for(Integer idx : sortedIndices) {
                        if (idx < opponent.getBoard().size()) {
                            opponent.discardFromBoard(opponent.getBoard().get(idx));
                            count++;
                        }
                    }
                    game.addLog(currentPlayer.getName() + " Dragon burned " + count + " cards!");
                }
                break;
            case DRUID:
                if (request.getTargetIndex() != null && request.getTargetIndex() < opponent.getHandSize()) {
                    opponent.discardFromHand(opponent.getHand().get(request.getTargetIndex()));
                    game.addLog(currentPlayer.getName() + " forced opponent to discard a card.");
                }
                break;
            case WARLOCK:
                if (request.getTargetIndex() != null && request.getTargetIndex() < currentPlayer.getDiscardPile().size()) {
                    CardType target = currentPlayer.getDiscardPile().get(request.getTargetIndex());
                    if (target.getCategory() == CardType.Category.SPELLCASTER) {
                        currentPlayer.getDiscardPile().remove(request.getTargetIndex().intValue());
                        currentPlayer.addCardToHand(target);
                        game.addLog(currentPlayer.getName() + " returned " + target + " from graveyard.");
                    } else {
                        actionSuccessful = false;
                        game.addLog("Invalid Warlock target! Must be Spellcaster.");
                    }
                } else {
                    actionSuccessful = false;
                }
                break;
            case ALCHEMIST: break;
        }

        if (checkWinCondition(currentPlayer.getBoard())) {
            game.setWinner(currentPlayer.getName());
            game.addLog("🏆 " + currentPlayer.getName() + " WINS THE GAME! 🏆");
        } else {
            endTurnOrForceDiscard(game);
        }
        return game;
    }

    private boolean canInterrupt(Player opponent, CardType playedCard) {
        List<CardType> hand = opponent.getHand();
        if (!hand.contains(CardType.WIZARD)) return false;
        if (playedCard == CardType.ALCHEMIST) return hand.contains(CardType.ALCHEMIST);
        int requiredWizards = (playedCard == CardType.WIZARD) ? 2 : 1;
        long wizardCount = hand.stream().filter(c -> c == CardType.WIZARD).count();
        if (wizardCount < requiredWizards) return false;
        return hand.contains(playedCard) || hand.contains(CardType.ALCHEMIST);
    }

    private GameState triggerInterrupt(GameState game, GameSocketController.MoveRequest request, CardType card) {
        pendingMoves.put(game.getGameId(), request);
        game.setPendingCard(card);
        game.setPendingTargetIndex(request.getTargetIndex());
        game.setStatus("WAITING_FOR_INTERRUPT");
        game.addLog("Waiting for " + game.getOpponent().getName() + " to interrupt...");
        return game;
    }

    public GameState resolveInterrupt(String gameId, boolean interrupt) {
        GameState game = activeGames.get(gameId);
        GameSocketController.MoveRequest request = pendingMoves.remove(gameId);

        if (!interrupt) {
            game.setStatus("PLAYING");
            game.setPendingCard(null);
            game.setPendingTargetIndex(null);
            game.addLog(game.getOpponent().getName() + " did not interrupt.");
            CardInstance instance = game.getCurrentPlayer().getBoard().get(game.getCurrentPlayer().getBoard().size() - 1);
            return executeCardEffect(game, instance.getCurrentCard(), request);
        } else {
            Player opponent = game.getOpponent();
            Player currentPlayer = game.getCurrentPlayer();
            CardType playedCard = game.getPendingCard();

            opponent.discardFromHand(CardType.WIZARD);
            if (playedCard == CardType.ALCHEMIST) opponent.discardFromHand(CardType.ALCHEMIST);
            else {
                if (opponent.getHand().contains(playedCard)) opponent.discardFromHand(playedCard);
                else opponent.discardFromHand(CardType.ALCHEMIST);
            }

            if (!currentPlayer.getBoard().isEmpty()) {
                CardInstance invalidCard = currentPlayer.getBoard().get(currentPlayer.getBoard().size() - 1);
                currentPlayer.discardFromBoard(invalidCard);
            }

            game.setStatus("PLAYING");
            game.setPendingCard(null);
            game.setPendingTargetIndex(null);
            game.addLog("⚡ " + opponent.getName() + " INTERRUPTED " + playedCard + "!");
            game.switchTurn();
            return game;
        }
    }

    private boolean checkWinCondition(List<CardInstance> board) {
        var spellcasters = board.stream().map(CardInstance::getCurrentCard)
                .filter(c -> c.getCategory() == CardType.Category.SPELLCASTER).toList();
        if (spellcasters.size() < 5) return false;
        if (spellcasters.stream().map(Enum::name).distinct().count() >= 5) return true;
        var counts = spellcasters.stream().collect(Collectors.groupingBy(c -> c, Collectors.counting()));
        return counts.values().stream().anyMatch(count -> count >= 5);
    }
}