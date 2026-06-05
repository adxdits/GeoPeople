import { Card } from "../models/card";
import { getCardById } from "./cardsService";

export type CollectionCard = Card & {
  collectionCoefficient: number;
  collectionScore: number;
};

export type CardCollection = {
  type: "initials" | "place" | "relation" | "first-letter";
  label: string;
  key: string;
  cards: CollectionCard[];
  score: number;
};

export type ScoreDetails = {
  totalScore: number;
  baseScore: number;
  collections: CardCollection[];
  cardMultipliers: Record<string, number>;
};

function fibonacci(n: number): number {
  if (n <= 2) return 1;

  let a = 1;
  let b = 1;

  for (let i = 3; i <= n; i++) {
    const next = a + b;
    a = b;
    b = next;
  }

  return b;
}

function normalizeKey(value: string): string {
  return value
    .trim()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toUpperCase();
}

function personInitials(personName: string): string {
  return personName
    .split(/\s+/)
    .filter(Boolean)
    .map(part => part[0])
    .join("")
    .toUpperCase();
}

function firstLetter(personName: string): string {
  return normalizeKey(personName)[0] ?? "?";
}

function addToGroup(groups: Map<string, Card[]>, key: string, card: Card): void {
  if (!groups.has(key)) {
    groups.set(key, []);
  }
  groups.get(key)!.push(card);
}

function buildCollection(
  type: CardCollection["type"],
  label: string,
  key: string,
  cards: Card[]
): CardCollection {
  const sortedCards = [...cards].sort((a, b) => a.personName.localeCompare(b.personName));
  let score = 0;

  const collectionCards = sortedCards.map((card, index) => {
    const collectionCoefficient = fibonacci(index + 1);
    const collectionScore = card.power * collectionCoefficient;
    score += collectionScore;

    return {
      ...card,
      collectionCoefficient,
      collectionScore
    };
  });

  return {
    type,
    label,
    key,
    cards: collectionCards,
    score
  };
}

export function buildCollections(cards: Card[]): CardCollection[] {
  const groups: Array<{
    type: CardCollection["type"];
    labelPrefix: string;
    values: Map<string, Card[]>;
  }> = [
    { type: "initials", labelPrefix: "Initiales", values: new Map() },
    { type: "place", labelPrefix: "Lieu", values: new Map() },
    { type: "relation", labelPrefix: "Relation", values: new Map() },
    { type: "first-letter", labelPrefix: "Premiere lettre", values: new Map() }
  ];

  for (const card of cards) {
    addToGroup(groups[0].values, personInitials(card.personName), card);
    addToGroup(groups[1].values, normalizeKey(card.placeName), card);
    addToGroup(groups[2].values, normalizeKey(card.relationName), card);
    addToGroup(groups[3].values, firstLetter(card.personName), card);
  }

  return groups.flatMap(group =>
    Array.from(group.values.entries())
      .filter(([, collectionCards]) => collectionCards.length > 1)
      .map(([key, collectionCards]) =>
        buildCollection(group.type, `${group.labelPrefix}: ${key}`, key, collectionCards)
      )
  );
}

export function calculateScoreDetails(cardIds: string[]): ScoreDetails {
  const cards = cardIds
    .map(cardId => getCardById(cardId))
    .filter((card): card is Card => card !== undefined);

  return calculateScoreDetailsForCards(cards);
}

export function calculateScoreDetailsForCards(cards: Card[]): ScoreDetails {
  const collections = buildCollections(cards);
  const cardMultipliers: Record<string, number> = {};

  for (const collection of collections) {
    for (const card of collection.cards) {
      cardMultipliers[card.id] = (cardMultipliers[card.id] ?? 0) + card.collectionCoefficient;
    }
  }

  let totalScore = 0;
  const baseScore = cards.reduce((sum, card) => sum + card.power, 0);

  for (const card of cards) {
    totalScore += card.power * Math.max(1, cardMultipliers[card.id] ?? 0);
  }

  return {
    totalScore,
    baseScore,
    collections,
    cardMultipliers
  };
}

export function calculateInventoryScore(cardIds: string[]): number {
  return calculateScoreDetails(cardIds).totalScore;
}
