import assert from "node:assert/strict";
import { describe, it } from "node:test";
import { Card } from "../models/card";
import { calculateScoreDetailsForCards } from "./scoringService";

function card(
  id: string,
  personName: string,
  power: number,
  placeName: string = "Paris",
  relationName: string = "lieu de naissance"
): Card {
  return {
    id,
    personId: Number(id.replace(/\D/g, "")) || 1,
    personName,
    placeId: 1,
    placeName,
    relationName,
    latitude: 0,
    longitude: 0,
    zone: false,
    power,
    history: []
  };
}

describe("scoringService", () => {
  it("garde le score brut quand aucune collection ne contient au moins deux cartes", () => {
    const result = calculateScoreDetailsForCards([
      card("c1", "Ada Lovelace", 10, "Londres", "naissance"),
      card("c2", "Marie Curie", 20, "Varsovie", "deces")
    ]);

    assert.equal(result.baseScore, 30);
    assert.equal(result.totalScore, 30);
    assert.deepEqual(result.cardMultipliers, {});
    assert.equal(result.collections.length, 0);
  });

  it("applique les coefficients Fibonacci dans une collection", () => {
    const result = calculateScoreDetailsForCards([
      card("c1", "Alice Alpha", 10, "Lieu A", "Relation A"),
      card("c2", "Aria Atlas", 10, "Lieu B", "Relation B"),
      card("c3", "Ava Archer", 10, "Lieu C", "Relation C"),
      card("c4", "Amelia Arden", 10, "Lieu D", "Relation D")
    ]);

    const firstLetterCollection = result.collections.find(
      collection => collection.type === "first-letter" && collection.key === "A"
    );

    assert.ok(firstLetterCollection);
    assert.deepEqual(
      firstLetterCollection.cards.map(collectionCard => collectionCard.collectionCoefficient),
      [1, 1, 2, 3]
    );
    assert.equal(firstLetterCollection.score, 10 * (1 + 1 + 2 + 3));
  });

  it("additionne les coefficients quand une carte appartient a plusieurs collections", () => {
    const result = calculateScoreDetailsForCards([
      card("c1", "Victor Hugo", 10, "Besancon", "lieu de naissance"),
      card("c2", "Valentin Hauy", 20, "Besancon", "lieu de naissance")
    ]);

    assert.equal(result.cardMultipliers.c1, 4);
    assert.equal(result.cardMultipliers.c2, 4);
    assert.equal(result.totalScore, 10 * 4 + 20 * 4);
    assert.equal(result.baseScore, 30);
  });
});
