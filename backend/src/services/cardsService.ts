import fs from "node:fs";
import path from "node:path";
import readline from "node:readline";
import { Card } from "../models/card";
import { LocalizedText, PersonPlaceEntry } from "../models/personPlace";

const PROFESSOR_DATA_FILE = process.env.PEOPLE_PLACES_FILE ??
  path.resolve(__dirname, "../../data/people-places.jsonl");
const ZONE_PLACEMENT_RADIUS_METERS = 20_000;
const PRECISE_PLACEMENT_RADIUS_METERS = 500;
const NEARBY_RESULT_LIMIT = 250;
const CACHE_TTL_MS = 5 * 60_000;

const DEMO_PEOPLE = [
  { personId: 2001, personName: "Victor Hugo", relationName: "collection test", power: 40 },
  { personId: 2002, personName: "Valentin Hauy", relationName: "collection test", power: 45 },
  { personId: 2003, personName: "Vera Rubin", relationName: "collection test", power: 50 },
  { personId: 2004, personName: "Voltaire", relationName: "collection test", power: 55 },
  { personId: 2005, personName: "Alan Turing", relationName: "enigme de capture", power: 35 },
  { personId: 2006, personName: "Ada Lovelace", relationName: "enigme de capture", power: 35 },
  { personId: 2007, personName: "Simone Veil", relationName: "memoire historique", power: 60 },
  { personId: 2008, personName: "Sophie Germain", relationName: "memoire historique", power: 65 }
];

const DEMO_OFFSETS_METERS = [
  { north: 8, east: 0 },
  { north: -12, east: 8 },
  { north: 18, east: -10 },
  { north: -65, east: -10 },
  { north: 85, east: 20 },
  { north: -110, east: 35 },
  { north: 145, east: -45 },
  { north: -180, east: 20 }
];

const initialCards: Card[] = [
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
  }
];

const cardsById = new Map<string, Card>();
const nearbyCache = new Map<string, { expiresAt: number; cards: Card[] }>();

initialCards.forEach(card => cardsById.set(card.id, card));

function haversine(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371000;
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a = Math.sin(dLat / 2) ** 2 +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
    Math.sin(dLon / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function localizedText(text: LocalizedText | undefined, fallback: string): string {
  return text?.fr ?? text?.en ?? fallback;
}

function hashString(value: string): number {
  let hash = 2166136261;
  for (let i = 0; i < value.length; i++) {
    hash ^= value.charCodeAt(i);
    hash = Math.imul(hash, 16777619);
  }
  return hash >>> 0;
}

function deterministicUnit(seed: string): number {
  return hashString(seed) / 0xffffffff;
}

function offsetCoordinates(lat: number, lon: number, northMeters: number, eastMeters: number) {
  const metersPerDegreeLat = 111_320;
  const metersPerDegreeLon = 111_320 * Math.cos(lat * Math.PI / 180);

  return {
    latitude: lat + northMeters / metersPerDegreeLat,
    longitude: lon + eastMeters / metersPerDegreeLon
  };
}

function placedCoordinates(lat: number, lon: number, cardId: string, zone: boolean) {
  const maxRadius = zone ? ZONE_PLACEMENT_RADIUS_METERS : PRECISE_PLACEMENT_RADIUS_METERS;
  const angle = deterministicUnit(`${cardId}:angle`) * Math.PI * 2;
  const distance = Math.sqrt(deterministicUnit(`${cardId}:distance`)) * maxRadius;

  return offsetCoordinates(
    lat,
    lon,
    Math.cos(angle) * distance,
    Math.sin(angle) * distance
  );
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

function rememberCard(card: Card): Card {
  const existing = cardsById.get(card.id);
  if (existing) {
    Object.assign(existing, {
      ...card,
      capturedBy: existing.capturedBy,
      capturedAt: existing.capturedAt,
      history: existing.history
    });
    return existing;
  }

  cardsById.set(card.id, card);
  return card;
}

function cardFromProfessorEntry(entry: PersonPlaceEntry, placeIndex: number): Card | null {
  const place = entry.places[placeIndex];
  const [placeLat, placeLon] = place.location;
  if (!Number.isFinite(placeLat) || !Number.isFinite(placeLon)) return null;

  const id = `${entry.id}-${place.id}-${place.relation_id}-${placeIndex}`;
  const position = placedCoordinates(placeLat, placeLon, id, place.zone);

  return {
    id,
    personId: entry.id,
    personName: localizedText(entry.name, `Q${entry.id}`),
    placeId: place.id,
    placeName: localizedText(place.name, `Lieu ${place.id}`),
    relationName: localizedText(place.relation_name, `Relation ${place.relation_id}`),
    latitude: position.latitude,
    longitude: position.longitude,
    zone: place.zone,
    power: 1,
    history: []
  };
}

function applyIntrinsicPower(cards: Card[]): Card[] {
  for (const card of cards) {
    let nearest = Number.POSITIVE_INFINITY;
    for (const other of cards) {
      if (other.id === card.id) continue;
      nearest = Math.min(nearest, haversine(card.latitude, card.longitude, other.latitude, other.longitude));
    }
    const fallback = card.zone ? 80 : 40;
    card.power = Number.isFinite(nearest)
      ? Math.round(Math.max(10, Math.min(250, nearest / 100)))
      : fallback;
  }
  return cards;
}

function cacheKey(lat: number, lon: number, radiusKm: number): string {
  return `${lat.toFixed(3)}:${lon.toFixed(3)}:${radiusKm.toFixed(1)}`;
}

async function scanProfessorCardsNearby(lat: number, lon: number, radiusKm: number): Promise<Card[]> {
  if (!fs.existsSync(PROFESSOR_DATA_FILE)) {
    console.warn(`[cards] Professor data file not found: ${PROFESSOR_DATA_FILE}`);
    return [];
  }

  const maxDistanceMeters = radiusKm * 1000;
  const roughDistanceMeters = maxDistanceMeters + ZONE_PLACEMENT_RADIUS_METERS;
  const stream = fs.createReadStream(PROFESSOR_DATA_FILE, { encoding: "utf8" });
  const reader = readline.createInterface({ input: stream, crlfDelay: Infinity });
  const found: { card: Card; distance: number }[] = [];

  for await (const line of reader) {
    if (!line.trim()) continue;

    let entry: PersonPlaceEntry;
    try {
      entry = JSON.parse(line) as PersonPlaceEntry;
    } catch {
      continue;
    }

    for (let i = 0; i < entry.places.length; i++) {
      const place = entry.places[i];
      const [placeLat, placeLon] = place.location;
      if (haversine(lat, lon, placeLat, placeLon) > roughDistanceMeters) continue;

      const card = cardFromProfessorEntry(entry, i);
      if (!card) continue;

      const distance = haversine(lat, lon, card.latitude, card.longitude);
      if (distance <= maxDistanceMeters) {
        found.push({ card: rememberCard(card), distance });
      }
    }
  }

  return applyIntrinsicPower(
    found
      .sort((a, b) => a.distance - b.distance)
      .slice(0, NEARBY_RESULT_LIMIT)
      .map(({ card }) => card)
  );
}

function ensureDemoCardsAround(lat: number, lon: number): Card[] {
  return buildDemoCardsAround(lat, lon).map(rememberCard);
}

export function getAllCards(): Card[] {
  return Array.from(cardsById.values());
}

export function getCardById(id: string): Card | undefined {
  return cardsById.get(id);
}

export async function getCardsNearby(lat: number, lon: number, radiusKm: number = 20): Promise<Card[]> {
  const key = cacheKey(lat, lon, radiusKm);
  const cached = nearbyCache.get(key);
  if (cached && cached.expiresAt > Date.now()) {
    return cached.cards;
  }

  const professorCards = await scanProfessorCardsNearby(lat, lon, radiusKm);
  const demoCards = ensureDemoCardsAround(lat, lon).filter(card =>
    haversine(lat, lon, card.latitude, card.longitude) <= Math.min(radiusKm * 1000, 500)
  );
  const result = [...professorCards, ...demoCards]
    .sort((a, b) => haversine(lat, lon, a.latitude, a.longitude) - haversine(lat, lon, b.latitude, b.longitude));

  nearbyCache.set(key, {
    expiresAt: Date.now() + CACHE_TTL_MS,
    cards: result
  });

  return result;
}
