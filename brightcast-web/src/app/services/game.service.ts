import { Injectable, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject } from 'rxjs';
import { GameState } from '../models/game.models';
import { GameSocketService } from './game-socket.service';

@Injectable({
  providedIn: 'root'
})
export class GameService {
  private apiUrl = '/api/game';

  public gameState$ = new BehaviorSubject<GameState | null>(null);

  constructor(
    private http: HttpClient,
    private socket: GameSocketService,
    private zone: NgZone
  ) {
    this.socket.activateGameSocket();
  }

  subscribeToGameUpdates(gameId: string): void {
    this.socket.watch(`/topic/game/${gameId}`).subscribe(message => {
      const updatedGame: GameState = JSON.parse(message.body);
      console.log("New Game State Received:", updatedGame);

      this.zone.run(() => {
        this.gameState$.next(updatedGame);
      });
    });
  }

  playCard(gameId: string, playerName: string, cardIndex: number, extraData?: any): void {
    const payload = {
      playerName,
      cardIndex,
      ...extraData    };

    this.socket.publish({
      destination: `/app/game/${gameId}/play`,
      body: JSON.stringify(payload)
    });
  }

  drawCard(gameId: string, playerName: string): void {
    this.socket.publish({
      destination: `/app/game/${gameId}/draw`,
      body: JSON.stringify({ playerName, cardIndex: 0 })
    });
  }

  discardCard(gameId: string, playerName: string, cardIndex: number): void {
    this.socket.publish({
      destination: `/app/game/${gameId}/discard`,
      body: JSON.stringify({ playerName, cardIndex })
    });
  }

  resolveInterrupt(gameId: string, interrupt: boolean): void {
    this.socket.publish({
      destination: `/app/game/${gameId}/interrupt`,
      body: JSON.stringify(interrupt)
    });
  }

  skipTurn(gameId: string, playerName: string): void {
    this.socket.publish({
      destination: `/app/game/${gameId}/skip`,
      body: JSON.stringify({playerName, cardIndex: 0})
    })
  }

  createGame(playerName: string): void {
    this.http.post<GameState>(`${this.apiUrl}/create?playerName=${playerName}`, {})
      .subscribe(game => {
        this.gameState$.next(game);
        this.subscribeToGameUpdates(game.gameId);
      });
  }

  joinGame(gameId: string, playerName: string): void {
    this.http.post<GameState>(`${this.apiUrl}/join?gameId=${gameId}&playerName=${playerName}`, {})
      .subscribe(game => {
        this.gameState$.next(game);
        this.subscribeToGameUpdates(game.gameId);
      }, error => {
        alert("Could not join game! Check ID.");
      });
  }
}
