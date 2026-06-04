import { Capture } from "../models/capture";
import { v4 as uuidv4 } from "uuid";
import { addCardToInventory } from "./playerService";
import { getCardById } from "./cardsService";

const captures: Capture[] = [];

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

  // mini jeu
  if (!miniGameSuccess) {
    return { success: false, message: "Mini-jeu non réussi" };
  }

  // distance max 50m
  const distance = haversine(playerLat,playerLon,card.latitude,card.longitude);
  if (distance > 50) {
    return {
      success: false,
      message: `Trop loin (${Math.round(distance)}m, max 50m)`
    };
  }

  // carte déjà prise par quelqu’un
  if (card.capturedBy) {
    return {
      success: false,
      message: "Carte déjà capturée par un joueur"
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

  return {
    success: true,
    message: "Carte capturée !",
    capture
  };
}

export function getPlayerCaptures(playerId: string): Capture[] {
  return captures.filter(c => c.playerId === playerId);
}
