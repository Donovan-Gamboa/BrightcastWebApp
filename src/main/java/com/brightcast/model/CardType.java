package com.brightcast.model;

public enum CardType {
    DRUID("Druid", Category.SPELLCASTER, 6, "Look at your opponent's hand They discard 1 card of your choice."),
    SAGE("Sage", Category.SPELLCASTER, 6, "Draw 2 cards, then discard 1 card from your hand."),
    WARLOCK("Warlock", Category.SPELLCASTER, 6, "Return one Spellcaster from your discard pile to your hand. (Not including Monsters or Wildcards)"),
    SORCERER("Sorcerer", Category.SPELLCASTER, 6, "Choose 1 of your opponent's cards in play. They place it into their discard pile"),
    WIZARD("Wizard", Category.SPELLCASTER, 6, "Draw 1 card OR When your opponent plays a card you wish to stop, you may discard this card plus a matching copy of their card from your hand. Their card has no effect and is discarded."),
    ALCHEMIST("Alchemist", Category.WILDCARD, 2, "Choose a Spellcaster you have in play and copy it action. The Alchemist remains in play as an exact copy of that Spellcaster. (Wildcards can count as any matching card when stopping your opponent's card.)"),
    DRAGON("Dragon", Category.MONSTER, 2, "Choose up to 3 of your opponent's cards in play. They place them into their discard pile.");

    private final String displayName;
    private final Category category;
    private final int deckCount;
    private final String description;

    CardType(String displayName, Category category, int deckCount, String description){
        this.displayName = displayName;
        this.category = category;
        this.deckCount = deckCount;
        this.description = description;
    }

    public int getDeckCount() {
        return deckCount;
    }

    public Category getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public enum Category {
        SPELLCASTER,
        MONSTER,
        WILDCARD
    }
}
