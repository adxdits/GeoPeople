import { v4 as uuidv4 } from "uuid";
import { getCardById } from "./cardsService";
import { exchangeCards, getPlayer } from "./playerService";

export type TradeStatus = "pending" | "accepted" | "rejected";

export type TradeProposal = {
  id: string;
  fromPlayerId: string;
  toPlayerId: string;
  fromCardId: string;
  toCardId: string;
  status: TradeStatus;
  createdAt: string;
  resolvedAt?: string;
};

const trades: TradeProposal[] = [];
const MAX_TRADE_DISTANCE_METERS = 100;

function haversineMeters(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371000;
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a = Math.sin(dLat / 2) ** 2 +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
    Math.sin(dLon / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function validatePlayersAreClose(
  fromPlayer: NonNullable<ReturnType<typeof getPlayer>>,
  toPlayer: NonNullable<ReturnType<typeof getPlayer>>
): { success: true } | { success: false; message: string } {
  const hasFromLocation = fromPlayer.latitude !== 0 || fromPlayer.longitude !== 0;
  const hasToLocation = toPlayer.latitude !== 0 || toPlayer.longitude !== 0;
  if (!hasFromLocation || !hasToLocation) {
    return {
      success: false,
      message: "Position des deux joueurs requise pour echanger"
    };
  }

  const distance = haversineMeters(
    fromPlayer.latitude,
    fromPlayer.longitude,
    toPlayer.latitude,
    toPlayer.longitude
  );
  if (distance > MAX_TRADE_DISTANCE_METERS) {
    return {
      success: false,
      message: `Joueurs trop eloignes pour echanger (${Math.round(distance)}m, max ${MAX_TRADE_DISTANCE_METERS}m)`
    };
  }

  return { success: true };
}

export function createTradeProposal(
  fromPlayerId: string,
  toPlayerId: string,
  fromCardId: string,
  toCardId: string
): { success: boolean; message: string; trade?: TradeProposal } {
  if (fromPlayerId === toPlayerId) {
    return { success: false, message: "Impossible d'echanger avec soi-meme" };
  }

  const fromPlayer = getPlayer(fromPlayerId);
  const toPlayer = getPlayer(toPlayerId);
  if (!fromPlayer || !toPlayer) {
    return { success: false, message: "Joueur introuvable" };
  }
  const proximity = validatePlayersAreClose(fromPlayer, toPlayer);
  if (!proximity.success) {
    return proximity;
  }

  const fromCard = getCardById(fromCardId);
  const toCard = getCardById(toCardId);
  if (!fromCard || !toCard) {
    return { success: false, message: "Carte introuvable" };
  }

  if (!fromPlayer.inventory.includes(fromCardId)) {
    return { success: false, message: `${fromPlayer.name} ne possede pas cette carte` };
  }
  if (!toPlayer.inventory.includes(toCardId)) {
    return { success: false, message: `${toPlayer.name} ne possede pas cette carte` };
  }

  const existingPending = trades.find((trade) =>
    trade.status === "pending" &&
    trade.fromPlayerId === fromPlayerId &&
    trade.toPlayerId === toPlayerId &&
    trade.fromCardId === fromCardId &&
    trade.toCardId === toCardId
  );
  if (existingPending) {
    return { success: false, message: "Cette proposition est deja en attente" };
  }

  const trade: TradeProposal = {
    id: uuidv4(),
    fromPlayerId,
    toPlayerId,
    fromCardId,
    toCardId,
    status: "pending",
    createdAt: new Date().toISOString()
  };
  trades.push(trade);

  return {
    success: true,
    message: "Proposition d'echange envoyee",
    trade
  };
}

export function getTradesForPlayer(playerId: string): TradeProposal[] {
  return trades.filter((trade) =>
    trade.fromPlayerId === playerId || trade.toPlayerId === playerId
  );
}

export function acceptTrade(
  tradeId: string,
  playerId: string
): { success: boolean; message: string; trade?: TradeProposal } {
  const trade = trades.find((item) => item.id === tradeId);
  if (!trade) {
    return { success: false, message: "Proposition introuvable" };
  }
  if (trade.status !== "pending") {
    return { success: false, message: "Cette proposition n'est plus en attente" };
  }
  if (trade.toPlayerId !== playerId) {
    return { success: false, message: "Seul le destinataire peut accepter l'echange" };
  }

  const fromPlayer = getPlayer(trade.fromPlayerId);
  const toPlayer = getPlayer(trade.toPlayerId);
  if (!fromPlayer || !toPlayer) {
    return { success: false, message: "Joueur introuvable" };
  }
  const proximity = validatePlayersAreClose(fromPlayer, toPlayer);
  if (!proximity.success) {
    return proximity;
  }

  const result = exchangeCards(
    trade.fromPlayerId,
    trade.toPlayerId,
    trade.fromCardId,
    trade.toCardId
  );
  if (!result.success) {
    trade.status = "rejected";
    trade.resolvedAt = new Date().toISOString();
    return { success: false, message: result.message, trade };
  }

  trade.status = "accepted";
  trade.resolvedAt = new Date().toISOString();
  return {
    success: true,
    message: "Echange accepte",
    trade
  };
}

export function rejectTrade(
  tradeId: string,
  playerId: string
): { success: boolean; message: string; trade?: TradeProposal } {
  const trade = trades.find((item) => item.id === tradeId);
  if (!trade) {
    return { success: false, message: "Proposition introuvable" };
  }
  if (trade.status !== "pending") {
    return { success: false, message: "Cette proposition n'est plus en attente" };
  }
  if (trade.toPlayerId !== playerId) {
    return { success: false, message: "Seul le destinataire peut refuser l'echange" };
  }

  trade.status = "rejected";
  trade.resolvedAt = new Date().toISOString();
  return {
    success: true,
    message: "Echange refuse",
    trade
  };
}
