import {ChangeDetectorRef, Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {GameService} from './services/game.service';
import {CardInstance, CardType, GameState, Player} from './models/game.models';

type TargetingState = 'NONE' | 'ENEMY_BOARD' | 'OWN_HAND' | 'OWN_GRAVEYARD' | 'ENEMY_HAND' | 'OWN_BOARD';

interface CardStack {
  type: string;
  cards: { instance: CardInstance; originalIndex: number }[];
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './app.html',
  styleUrls: ['./app.css']
})
export class App implements OnInit {
  gameState: GameState | null = null;
  playerName = 'Donovan';

  targetingState: TargetingState = 'NONE';
  selectedHandIndex: number | null = null;
  viewingGraveyard = false;
  viewingOpponentGraveyard = false;
  viewingEnemyHand = false;
  selectedTargets: number[] = [];
  showRules = false;

  // NEW: Notification System
  notificationMessage: string | null = null;

  constructor(
    private gameService: GameService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.gameService.gameState$.subscribe(state => {
      this.gameState = state;

      if (state?.status === 'WAITING_FOR_DISCARD' && this.isMyTurn) {
        this.targetingState = 'OWN_HAND';
        if (this.me!.hand.length > 8) {
          this.showNotification(`Hand Limit Reached! Discard down to 8.`);
        }
      }
      else if (state?.currentPlayer.name !== this.playerName) {
        this.cancelTargeting();
      }
      this.cdr.detectChanges();
    });
  }

  // NEW: Replaces alert() with a game-styled popup
  showNotification(msg: string) {
    this.notificationMessage = msg;
    // Auto-hide after 3 seconds
    setTimeout(() => {
      if (this.notificationMessage === msg) {
        this.notificationMessage = null;
      }
    }, 3000);
  }

  getStackedBoard(player: Player): CardStack[] {
    const stacks: Map<string, CardStack> = new Map();
    player.board.forEach((cardInstance, index) => {
      const type = cardInstance.currentCard;
      if (!stacks.has(type)) {
        stacks.set(type, { type, cards: [] });
      }
      stacks.get(type)!.cards.push({ instance: cardInstance, originalIndex: index });
    });
    return Array.from(stacks.values());
  }

  getTopDiscard(player: Player): string | null {
    if (!player.discardPile || player.discardPile.length === 0) return null;
    return player.discardPile[player.discardPile.length - 1];
  }

  toggleRules(){ this.showRules = !this.showRules; }

  isLockedOut(index: number): boolean {
    if (this.targetingState === 'OWN_HAND') return false;
    return this.targetingState !== 'NONE' && this.selectedHandIndex !== index;
  }

  onHandClick(index: number) {
    if (!this.gameState || !this.isMyTurn) return;

    if (this.gameState.turnPhase === 'DRAW' && this.targetingState === 'NONE') {
      this.showNotification("You must Draw a card first!");
      return;
    }

    if (this.targetingState !== 'NONE' && this.targetingState !== 'OWN_HAND') {
      if (this.selectedHandIndex === index) this.cancelTargeting();
      else this.showNotification("Finish your current action or cancel by clicking the selected card!");
      return;
    }

    const card = this.me!.hand[index];
    this.selectedTargets = [];

    if (this.targetingState === 'OWN_HAND') {
      if (this.gameState.status === 'WAITING_FOR_DISCARD') {
        this.gameService.discardCard(this.gameState.gameId, this.playerName, index);
        this.cancelTargeting();
        return;
      }
      return;
    }

    this.selectedHandIndex = index;

    switch (card) {
      case CardType.SAGE:
        this.finalizeMove(index);
        break;
      case CardType.SORCERER:
        this.startTargeting(index, 'ENEMY_BOARD');
        break;
      case CardType.DRAGON:
        this.startTargeting(index, 'ENEMY_BOARD');
        break;
      case CardType.WARLOCK:
        this.startTargeting(index, 'OWN_GRAVEYARD');
        this.viewingGraveyard = true;
        break;
      case CardType.DRUID:
        this.startTargeting(index, 'ENEMY_HAND');
        this.viewingEnemyHand = true;
        break;
      case CardType.ALCHEMIST:
        if (this.me!.board.length === 0) {
          this.showNotification("You need a Spellcaster on the board to copy!");
          return;
        }
        this.startTargeting(index, 'OWN_BOARD');
        break;
      default:
        this.finalizeMove(index);
        break;
    }
  }

  onOwnBoardClick(originalIndex: number) {
    if (this.targetingState === 'OWN_BOARD') {
      this.finalizeMove(this.selectedHandIndex!, { targetIndex: originalIndex });
    }
  }

  onGraveyardCardClick(targetIndex: number) {
    if (this.targetingState === 'OWN_GRAVEYARD') {
      const targetCard = this.me!.discardPile[targetIndex];
      if (targetCard === CardType.DRAGON || targetCard === CardType.ALCHEMIST) {
        this.showNotification("Warlock can only revive Spellcasters (not Monsters or Wildcards)!");
        return;
      }
      this.finalizeMove(this.selectedHandIndex!, { targetIndex: targetIndex });
      this.viewingGraveyard = false;
    }
  }

  onOpponentBoardClick(originalIndex: number) {
    if(this.targetingState !== 'ENEMY_BOARD') return;

    if (this.me?.hand[this.selectedHandIndex!] === CardType.SORCERER) {
      this.finalizeMove(this.selectedHandIndex!, { targetIndex: originalIndex });
      return;
    }

    if(this.me?.hand[this.selectedHandIndex!] === CardType.DRAGON) {
      if (this.selectedTargets.length < 3) {
        this.selectedTargets.push(originalIndex);
      } else {
        this.showNotification("Max 3 Targets!");
      }
    }
  }

  resetTargets() {
    this.selectedTargets = [];
  }

  onEnemyHandCardClick(targetIndex: number) {
    if (this.targetingState === 'ENEMY_HAND') {
      this.finalizeMove(this.selectedHandIndex!, { targetIndex: targetIndex });
      this.viewingEnemyHand = false;
    }
  }

  confirmDragonAttack() {
    if (this.selectedTargets.length === 0) return;
    this.finalizeMove(this.selectedHandIndex!, { targetIndices: this.selectedTargets });
    this.selectedTargets = [];
  }

  toggleGraveyard() {
    if (this.viewingGraveyard && this.targetingState === 'OWN_GRAVEYARD') {
      this.cancelTargeting();
    } else {
      this.viewingGraveyard = !this.viewingGraveyard;
    }
  }
  startTargeting(index: number, mode: TargetingState) { this.targetingState = mode; this.selectedHandIndex = index; }

  cancelTargeting() {
    this.targetingState = 'NONE';
    this.selectedHandIndex = null;
    this.viewingGraveyard = false;
    this.viewingOpponentGraveyard = false;
    this.viewingEnemyHand = false;
    this.selectedTargets = [];
  }

  finalizeMove(cardIndex: number, extras: any = {}) {
    this.gameService.playCard(this.gameState!.gameId, this.playerName, cardIndex, extras);
    this.cancelTargeting();
  }

  getSelectionCount(originalIndex: number): number {
    return this.selectedTargets.filter(i => i === originalIndex).length;
  }

  skipTurn() { if (this.isMyTurn) this.gameService.skipTurn(this.gameState!.gameId, this.playerName); }
  drawCard() { if (this.isMyTurn) this.gameService.drawCard(this.gameState!.gameId, this.playerName); }

  get me(): Player | undefined {
    if (!this.gameState) return undefined;
    return this.gameState.player1.name === this.playerName ? this.gameState.player1 : this.gameState.player2;
  }

  get opponent(): Player | undefined {
    if (!this.gameState) return undefined;
    return this.gameState.player1.name === this.playerName ? this.gameState.player2 : this.gameState.player1;
  }
  get isMyTurn(): boolean { return this.gameState?.currentPlayer.name === this.playerName; }
  respondToInterrupt(choice: boolean) { if (this.gameState) this.gameService.resolveInterrupt(this.gameState.gameId, choice); }
  leaveGame() { this.gameState = null; this.cancelTargeting(); }
  createGame(name: string) { if(name) { this.playerName = name; this.gameService.createGame(name); } }
  joinGame(gameId: string, name: string) { if(name && gameId) { this.playerName = name; this.gameService.joinGame(gameId.toUpperCase(), name); } }
}
