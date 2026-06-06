import { Player } from "../models/player";
import { Card } from "../models/card";
import { v4 as uuidv4 } from "uuid";
import fs from "fs";
import path from "path";
import { getCardById, getCardsNearby } from "./cardsService";
import { calculateInventoryScore } from "./scoringService";

const DATA_DIR = path.join(process.cwd(), "data");
const PLAYERS_FILE = path.join(DATA_DIR, "players.json");

function normalizePlayerName(name: string): string {
  return name.trim().toLocaleLowerCase();
}

function loadPlayers(): Map<string, Player> {
  if (!fs.existsSync(PLAYERS_FILE)) {
    return new Map();
  }

  try {
    const raw = fs.readFileSync(PLAYERS_FILE, "utf-8");
    const storedPlayers = JSON.parse(raw) as Player[];
    const mergedPlayers = new Map<string, Player>();

    for (const player of storedPlayers) {
      const key = normalizePlayerName(player.name);
      const existingPlayer = Array.from(mergedPlayers.values()).find(
        (storedPlayer) => normalizePlayerName(storedPlayer.name) === key
      );

      if (!existingPlayer) {
        mergedPlayers.set(player.id, {
          ...player,
          inventory: Array.from(new Set(player.inventory))
        });
        continue;
      }

      existingPlayer.inventory = Array.from(new Set([
        ...existingPlayer.inventory,
        ...player.inventory
      ]));
      existingPlayer.score = calculateInventoryScore(existingPlayer.inventory);
      if (new Date(player.lastSeen).getTime() > new Date(existingPlayer.lastSeen).getTime()) {
        existingPlayer.latitude = player.latitude;
        existingPlayer.longitude = player.longitude;
        existingPlayer.lastSeen = player.lastSeen;
      }
    }

    return mergedPlayers;
  } catch (error) {
    console.warn("Impossible de charger les joueurs sauvegardes", error);
    return new Map();
  }
}

let players: Map<string, Player> = loadPlayers();

function savePlayers(): void {
  fs.mkdirSync(DATA_DIR, { recursive: true });
  fs.writeFileSync(
    PLAYERS_FILE,
    JSON.stringify(Array.from(players.values()), null, 2),
    "utf-8"
  );
}

export function registerPlayer(name: string): Player {
  const existingPlayer = Array.from(players.values()).find(
    (player) => normalizePlayerName(player.name) === normalizePlayerName(name)
  );
  if (existingPlayer) {
    existingPlayer.lastSeen = new Date().toISOString();
    existingPlayer.score = calculateInventoryScore(existingPlayer.inventory);
    savePlayers();
    return existingPlayer;
  }

  const id = uuidv4();
  const player: Player = {
    id,
    name,
    latitude: 0,
    longitude: 0,
    lastSeen: new Date().toISOString(),
    inventory: [],
    score: 0
  };
  players.set(id, player);
  savePlayers();
  return player;
}

export function getPlayer(id: string): Player | undefined {
  return players.get(id);
}

export function updatePlayerLocation(id: string, lat: number, lon: number ): Player | undefined {
  const player = players.get(id);
  if (!player) return undefined;

  const now = Date.now();
  const oldTime = new Date(player.lastSeen).getTime();
  const minutes = (now - oldTime) / (1000 * 60);
  const distance = haversine( player.latitude, player.longitude, lat, lon );

  const hasPreviousLocation = player.latitude !== 0 || player.longitude !== 0;

  // anti-triche: skip the very first real GPS fix after registration.
  if (hasPreviousLocation && minutes < 30 && distance > 500) {
    throw new Error("Déplacement suspect détecté");
  }

  player.latitude = lat;
  player.longitude = lon;
  player.lastSeen = new Date().toISOString();
  savePlayers();

  return player;
}

export function addCardToInventory(playerId: string, cardId: string ): Player | undefined {
  const player = players.get(playerId);
  if (!player) return undefined;
  if (!player.inventory.includes(cardId)) {
    const card = getCardById(cardId);
    if (!card) return undefined;
    player.inventory.push(cardId);
    player.score = calculateInventoryScore(player.inventory);
    savePlayers();
  }
  return player;
}

export function getPlayerInventory(playerId: string): string[] {
  const player = players.get(playerId);
  return player?.inventory ?? [];
}

export function getPlayerInventoryCards(playerId: string): Card[] {
  const player = players.get(playerId);
  if (!player) return [];

  if (player.latitude !== 0 || player.longitude !== 0) {
    getCardsNearby(player.latitude, player.longitude, 1);
  }

  return player.inventory
    .map((cardId) => getCardById(cardId))
    .filter((card): card is Card => card !== undefined);
}

function haversine(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371;
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a = Math.sin(dLat / 2) ** 2 + Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *  Math.sin(dLon / 2) ** 2;

  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

export function getLeaderboard(): Player[] {
  for (const player of players.values()) {
    player.score = calculateInventoryScore(player.inventory);
  }
  savePlayers();

  return Array.from(players.values())
    .sort((a, b) => b.score - a.score);
}
