import { getPlayerInventory } from "./playerService";
import { getCardById } from "./cardsService";

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

export function getPlayerCollections(playerId: string) {
  const inventory = getPlayerInventory(playerId);
  const cards = inventory.map(cardId => getCardById(cardId)).filter(card => card !== undefined);
  const byInitial = new Map<string, typeof cards>();
  for (const card of cards) {
    if (!card) continue;

    const words = card.personName.split(" ");
    const initials = words.map(w => w[0]).join("").toUpperCase();

    if (!byInitial.has(initials)) {
      byInitial.set(initials, []);
    }
    byInitial.get(initials)!.push(card);
  }

  const collections = [];

  for (const [key, collectionCards] of byInitial.entries()) {
    let score = 0;
    collectionCards.forEach((card, index) => {
      const coefficient = fibonacci(index + 1);
      score += card.power * coefficient;
    });

    collections.push({type: "initials", key, cards: collectionCards, score });
  }

  return collections;
}