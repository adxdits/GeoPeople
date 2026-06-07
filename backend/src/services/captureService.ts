import { Capture } from "../models/capture";
import { v4 as uuidv4 } from "uuid";
import { addCardToInventory, getPlayerInventory } from "./playerService";
import { getCardById } from "./cardsService";

const captures: Capture[] = [];
const failedCaptureLocks = new Map<string, { attempts: number; lockedUntil: number }>();
const LOCK_DURATIONS_MS = [
  30_000,
  2 * 60_000,
  5 * 60_000,
  15 * 60_000,
  30 * 60_000
];

function lockKey(playerId: string, cardId: string): string {
  return `${playerId}:${cardId}`;
}

function formatDuration(ms: number): string {
  const totalSeconds = Math.max(1, Math.ceil(ms / 1000));
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return minutes > 0 ? `${minutes}min ${seconds}s` : `${seconds}s`;
}

function registerFailedAttempt(playerId: string, cardId: string): { attempts: number; remainingMs: number } {
  const key = lockKey(playerId, cardId);
  const previousAttempts = failedCaptureLocks.get(key)?.attempts ?? 0;
  const attempts = previousAttempts + 1;
  const duration = LOCK_DURATIONS_MS[Math.min(attempts - 1, LOCK_DURATIONS_MS.length - 1)];
  failedCaptureLocks.set(key, { attempts, lockedUntil: Date.now() + duration });
  return { attempts, remainingMs: duration };
}

function getRemainingLockMs(playerId: string, cardId: string): number {
  const key = lockKey(playerId, cardId);
  const lock = failedCaptureLocks.get(key);
  if (!lock) return 0;

  const remainingMs = lock.lockedUntil - Date.now();
  if (remainingMs <= 0) {
    failedCaptureLocks.delete(key);
    return 0;
  }
  return remainingMs;
}

function haversine(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371000;
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a = Math.sin(dLat / 2) ** 2 +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
    Math.sin(dLon / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

export function captureCard(
  playerId: string,
  cardId: string,
  playerLat: number,
  playerLon: number,
  miniGameSuccess: boolean
): { success: boolean; message: string; capture?: Capture } {

  const card = getCardById(cardId);
  if (!card) {
    return { success: false, message: "Carte introuvable" };
  }

  // deja capturee par n'importe quel joueur
  if (card.capturedBy) {
    return {
      success: false,
      message: "Carte deja capturee par un autre joueur"
    };
  }

  const remainingLockMs = getRemainingLockMs(playerId, cardId);
  if (remainingLockMs > 0) {
    return {
      success: false,
      message: `Carte verrouillee encore ${formatDuration(remainingLockMs)}`
    };
  }

  // mini jeu
  if (!miniGameSuccess) {
    const lock = registerFailedAttempt(playerId, cardId);
    return {
      success: false,
      message: `Mini-jeu perdu : carte verrouillee pendant ${formatDuration(lock.remainingMs)}. Tentative ${lock.attempts}.`
    };
  }

  // distance max 50m
  const distance = haversine(playerLat,playerLon,card.latitude,card.longitude);
  if (distance > 50) {
    return {
      success: false,
      message: `Trop loin (${Math.round(distance)}m, max 50m)`
    };
  }

  // deja dans l'inventaire de ce joueur
  if (getPlayerInventory(playerId).includes(cardId)) {
    return {
      success: false,
      message: "Carte deja presente dans votre inventaire"
    };
  }

  const captureDate = new Date().toISOString();

  const capture: Capture = {
    id: uuidv4(),
    playerId,
    cardId,
    latitude: playerLat,
    longitude: playerLon,
    capturedAt: captureDate
  };

  captures.push(capture);

  // mise à jour carte
  card.capturedBy = playerId;
  card.capturedAt = captureDate;

  // historique
  card.history.push({
    playerId,
    action: "capture",
    date: captureDate
  });

  // ajout inventaire
  addCardToInventory(playerId, cardId);
  failedCaptureLocks.delete(lockKey(playerId, cardId));

  return {
    success: true,
    message: "Carte capturée !",
    capture
  };
}

export function getPlayerCaptures(playerId: string): Capture[] {
  return captures.filter(c => c.playerId === playerId);
}
