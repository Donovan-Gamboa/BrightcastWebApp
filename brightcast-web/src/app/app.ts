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
  viewingEnemyHand = false;
  selectedTargets: number[] = [];
  showRules = false;

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
          alert(`Hand Limit Reached! You have ${this.me!.hand.length} cards. Discard down to 8.`);
        } else {
          alert("Sage Effect: Select a card to discard!");
        }
      }
      else if (state?.status === 'WAITING_FOR_INTERRUPT' && !this.isMyTurn) {
      }
      else if (state?.currentPlayer.name !== this.playerName) {
        this.cancelTargeting();
      }

      this.cdr.detectChanges();
    });
  }

  getStackedBoard(player: Player): CardStack[] {
    const stacks: Map<string, CardStack> = new Map();

    player.board.forEach((cardInstance, index) => {
      const type = cardInstance.currentCard;

      if (!stacks.has(type)) {
        stacks.set(type, { type, cards: [] });
      }

      stacks.get(type)!.cards.push({
        instance: cardInstance,
        originalIndex: index      });
    });

    return Array.from(stacks.values());
  }

  getTopDiscard(player: Player): string | null {
    if (!player.discardPile || player.discardPile.length === 0) return null;
    return player.discardPile[player.discardPile.length - 1];
  }

  toggleRules(){
    this.showRules = !this.showRules;
  }

  isLockedOut(index: number): boolean {
    if (this.targetingState === 'OWN_HAND') {
      return false;
    }
    return this.targetingState !== 'NONE' && this.selectedHandIndex !== index;
  }

  onHandClick(index: number) {
    if (!this.gameState || !this.isMyTurn) return;
    if (this.targetingState !== 'NONE' && this.targetingState !== 'OWN_HAND') {
      if (this.selectedHandIndex === index) {
        this.cancelTargeting();
      } else {
        alert("Finish your current action or cancel by clicking the selected card!");
      }
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

      if(this.gameState.status !== 'PLAYING') {
        console.warn("Game paused, cannot play cards");
        return;
      }

      if (index === this.selectedHandIndex) {
        alert("You cannot discard the card you are playing!");
        return;
      }
      this.finalizeMove(this.selectedHandIndex!, { targetIndex: index });
      return;
    }

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
          alert("You need a Spellcaster on the board to copy!");
          return;
        }
        this.startTargeting(index, 'OWN_BOARD');
        alert("Select a Spellcaster on your board to copy.");
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
      this.finalizeMove(this.selectedHandIndex!, { targetIndex: targetIndex });
      this.viewingGraveyard = false;
    }
  }

  onOpponentBoardClick(originalIndex: number) {
    if(this.targetingState !== 'ENEMY_BOARD') return;

    if (this.me?.hand[this.selectedHandIndex!] === CardType.SORCERER) {
      this.finalizeMove(this.selectedHandIndex!, { targetIndex: originalIndex, targetIndices: [originalIndex] });
      return;
    }

    if(this.me?.hand[this.selectedHandIndex!] === CardType.DRAGON) {
      const existingIdx = this.selectedTargets.indexOf(originalIndex);
      if (existingIdx > -1) {
        this.selectedTargets.splice(existingIdx, 1);
      } else {
        if(this.selectedTargets.length < 3) this.selectedTargets.push(originalIndex);
        else alert("Max 3 targets!");
      }
    }
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

  togglePlayer() {
    this.playerName = this.playerName === 'Donovan' ? 'Guest' : 'Donovan';
    this.cancelTargeting();
    this.cdr.detectChanges();
  }

  toggleGraveyard() { this.viewingGraveyard = !this.viewingGraveyard; }

  startTargeting(index: number, mode: TargetingState) {
    this.targetingState = mode;
    this.selectedHandIndex = index;
  }

  cancelTargeting() {
    this.targetingState = 'NONE';
    this.selectedHandIndex = null;
    this.viewingGraveyard = false;
    this.viewingEnemyHand = false;
    this.selectedTargets = [];
  }

  finalizeMove(cardIndex: number, extras: any = {}) {
    this.gameService.playCard(
      this.gameState!.gameId,
      this.playerName,
      cardIndex,
      extras
    );
    this.cancelTargeting();
  }

  skipTurn() {
    if (this.isMyTurn) this.gameService.skipTurn(this.gameState!.gameId, this.playerName);
  }

  drawCard() {
    if (this.isMyTurn) this.gameService.drawCard(this.gameState!.gameId, this.playerName);
  }

  get me(): Player | undefined {
    if (!this.gameState) return undefined;
    return this.gameState.player1.name === this.playerName ? this.gameState.player1 : this.gameState.player2;
  }

  get opponent(): Player | undefined {
    if (!this.gameState) return undefined;
    return this.gameState.player1.name === this.playerName ? this.gameState.player2 : this.gameState.player1;
  }

  get isMyTurn(): boolean { return this.gameState?.currentPlayer.name === this.playerName; }

  respondToInterrupt(choice: boolean) {
    if (this.gameState) {
      this.gameService.resolveInterrupt(this.gameState.gameId, choice);
    }
  }

  leaveGame() {
    this.gameState = null;
    this.cancelTargeting();
  }

  createGame(name: string) {
    if(!name) return alert("Enter a name!");
    this.playerName = name;
    this.gameService.createGame(name);
  }

  joinGame(gameId: string, name: string) {
    if(!name || !gameId) return alert("Enter name and ID!");
    this.playerName = name;
    this.gameService.joinGame(gameId.toUpperCase(), name);
  }
}
