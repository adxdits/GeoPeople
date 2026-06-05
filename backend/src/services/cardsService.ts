import { Card } from "../models/card";

const DEMO_PEOPLE = [
  { personId: 2001, personName: "Alan Turing", relationName: "enigme de capture", power: 45 },
  { personId: 2002, personName: "Simone Veil", relationName: "memoire historique", power: 60 },
  { personId: 2003, personName: "Leonard de Vinci", relationName: "atelier secret", power: 80 },
  { personId: 2004, personName: "Frida Kahlo", relationName: "trace artistique", power: 55 },
  { personId: 2005, personName: "Nelson Mandela", relationName: "lieu symbolique", power: 75 },
  { personId: 2006, personName: "Hypatie", relationName: "savoir ancien", power: 65 },
  { personId: 2007, personName: "Nikola Tesla", relationName: "signal electrique", power: 70 },
  { personId: 2008, personName: "Wangari Maathai", relationName: "racine verte", power: 50 }
];

const DEMO_OFFSETS_METERS = [
  { north: 80, east: 20 },
  { north: -90, east: 70 },
  { north: 140, east: -110 },
  { north: -160, east: -60 },
  { north: 230, east: 130 },
  { north: -260, east: 180 },
  { north: 360, east: -180 },
  { north: -420, east: -220 }
];

const cards: Card[] = [
  {
    id: "demo-emu-1",
    personId: 25,
    personName: "Ada Lovelace",
    placeId: 1,
    placeName: "Android Emulator",
    relationName: "carte de test",
    latitude: 37.4219983,
    longitude: -122.084,
    zone: false,
    power: 25,
    history: []
  },
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
    power: 50,
    history: []
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
    power: 100,
    history: []
  },
  {
    id: "7-456-19",
    personId: 7,
    personName: "Marie Curie",
    placeId: 456,
    placeName: "Paris",
    relationName: "lieu de décès",
    latitude: 48.8566,
    longitude: 2.3522,
    zone: true,
    power: 90,
    history: []
  },
  {
    id: "12-789-19",
    personId: 12,
    personName: "Napoléon Bonaparte",
    placeId: 789,
    placeName: "Ajaccio",
    relationName: "lieu de naissance",
    latitude: 41.9192,
    longitude: 8.7386,
    zone: false,
    power: 95,
    history: []
  },
  {
    id: "99-234-19",
    personId: 99,
    personName: "Albert Camus",
    placeId: 234,
    placeName: "Mondovi",
    relationName: "lieu de naissance",
    latitude: 36.4627,
    longitude: 7.4331,
    zone: false,
    power: 70,
    history: []
  }
];

function haversine(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371000;
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a = Math.sin(dLat / 2) ** 2 +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
    Math.sin(dLon / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function offsetCoordinates(lat: number, lon: number, northMeters: number, eastMeters: number) {
  const metersPerDegreeLat = 111_320;
  const metersPerDegreeLon = 111_320 * Math.cos(lat * Math.PI / 180);

  return {
    latitude: lat + northMeters / metersPerDegreeLat,
    longitude: lon + eastMeters / metersPerDegreeLon
  };
}

function demoCellId(lat: number, lon: number): string {
  return `${lat.toFixed(3)}-${lon.toFixed(3)}`.replace(/[^0-9-]/g, "");
}

function buildDemoCardsAround(lat: number, lon: number): Card[] {
  const cellId = demoCellId(lat, lon);
  return DEMO_PEOPLE.map((person, index) => {
    const offset = DEMO_OFFSETS_METERS[index];
    const position = offsetCoordinates(lat, lon, offset.north, offset.east);

    return {
      id: `demo-${cellId}-${index + 1}`,
      personId: person.personId,
      personName: person.personName,
      placeId: 9000 + index,
      placeName: "Autour de vous",
      relationName: person.relationName,
      latitude: position.latitude,
      longitude: position.longitude,
      zone: false,
      power: person.power,
      history: []
    };
  });
}

function ensureDemoCardsAround(lat: number, lon: number): void {
  buildDemoCardsAround(lat, lon).forEach(demoCard => {
    const existingIndex = cards.findIndex(card => card.id === demoCard.id);
    if (existingIndex >= 0) {
      cards[existingIndex] = {
        ...cards[existingIndex],
        ...demoCard,
        capturedBy: cards[existingIndex].capturedBy,
        capturedAt: cards[existingIndex].capturedAt,
        history: cards[existingIndex].history
      };
    } else {
      cards.push(demoCard);
    }
  });
}

export function getAllCards(): Card[] {
  return cards;
}

export function getCardById(id: string): Card | undefined {
  return cards.find(c => c.id === id);
}

export function getCardsNearby(lat: number, lon: number, radiusKm: number = 20): Card[] {
  ensureDemoCardsAround(lat, lon);

  return cards
    .map(card => ({
      card,
      distance: haversine(lat, lon, card.latitude, card.longitude)
    }))
    .filter(({ distance }) => distance <= radiusKm * 1000)
    .sort((a, b) => a.distance - b.distance)
    .map(({ card }) => card);
}
