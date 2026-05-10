import { Card } from "../models/card";

const cards: Card[] = [
  {
    id: "42-350-19",
    personId: 42,
    personName: "Douglas Adams",
    placeId: 350,
    placeName: "Cambridge",
    relationName: "lieu de naissance",
    latitude: 52.208057,
    longitude: 0.1225,
    zone: true,
    power: 50
  },
  {
    id: "535-123-19",
    personId: 535,
    personName: "Victor Hugo",
    placeId: 123,
    placeName: "Besancon",
    relationName: "lieu de naissance",
    latitude: 47.2348,
    longitude: 6.02918,
    zone: false,
    power: 100
  }
];

export function getAllCards(): Card[] {
  return cards;
}
