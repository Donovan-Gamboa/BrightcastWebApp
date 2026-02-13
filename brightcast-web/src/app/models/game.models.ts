export enum CardType {
  DRUID = 'DRUID',
  SAGE = 'SAGE',
  WARLOCK = 'WARLOCK',
  SORCERER = 'SORCERER',
  WIZARD = 'WIZARD',
  ALCHEMIST = 'ALCHEMIST',
  DRAGON = 'DRAGON'
}

export interface CardInstance {
  currentCard: CardType;
  originalCard: CardType;
}

export interface Player {
  name: string;
  hand: CardType[];
  board: CardInstance[];
  discardPile: CardType[];
  handSize: number;
}

export interface GameState {
  gameId: string;
  player1: Player;
  player2: Player;
  currentPlayerIndex: number;
  status: 'PLAYING' | 'FINISHED' | 'WAITING_FOR_INTERRUPT' | 'WAITING_FOR_DISCARD' | 'WAITING_FOR_PLAYER';
  winnerName?: string;
  currentPlayer: Player;
  opponent: Player;
  turnPhase: 'DRAW'|'MAIN';
  pendingCard?: CardType;
  pendingTargetIndex?: number;
  logs: string[];
}
