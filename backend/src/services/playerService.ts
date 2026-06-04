import { Player } from "../models/player";
import { v4 as uuidv4 } from "uuid";
import { getCardById } from "./cardsService";

const players: Map<string, Player> = new Map();

export function registerPlayer(name: string): Player {
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

  // anti-triche
  if (minutes < 30 && distance > 500) {
    throw new Error("Déplacement suspect détecté");
  }

  player.latitude = lat;
  player.longitude = lon;
  player.lastSeen = new Date().toISOString();

  return player;
}

export function addCardToInventory(playerId: string, cardId: string ): Player | undefined {
  const player = players.get(playerId);
  if (!player) return undefined;
  if (!player.inventory.includes(cardId)) {
    const card = getCardById(cardId);
    if (!card) return undefined;
    player.inventory.push(cardId);
    player.score += card.power;
  }
  return player;
}

export function getPlayerInventory(playerId: string): string[] {
  const player = players.get(playerId);
  return player?.inventory ?? [];
}

function haversine(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371;
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a = Math.sin(dLat / 2) ** 2 + Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *  Math.sin(dLon / 2) ** 2;

  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

export function getLeaderboard(): Player[] {
  return Array.from(players.values())
    .sort((a, b) => b.score - a.score);
}