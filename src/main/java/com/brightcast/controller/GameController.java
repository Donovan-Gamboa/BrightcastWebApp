package com.brightcast.controller;

import com.brightcast.model.GameState;
import com.brightcast.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "*")
public class GameController {
    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping("/create")
    public ResponseEntity<GameState> createGame(@RequestParam String playerName) {
        GameState gameState = gameService.createGame(playerName);
        return ResponseEntity.ok(gameState);
    }

    @PostMapping("/join")
    public ResponseEntity<GameState> joinGame(@RequestParam String gameId, @RequestParam String playerName) {
        try {
            GameState gameState = gameService.joinGame(gameId, playerName);
            return ResponseEntity.ok(gameState);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();        }
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<GameState> getGame(@PathVariable String gameId){
        GameState game = gameService.getGame(gameId);
        if (game == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(game);
    }
}