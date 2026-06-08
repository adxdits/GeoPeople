import { v4 as uuidv4 } from "uuid";
import { getCardById } from "./cardsService";
import { battleCards, getPlayer } from "./playerService";

export type BattleStatus = "pending" | "accepted" | "rejected";

export type BattleProposal = {
  id: string;
  fromPlayerId: string;
  toPlayerId: string;
  fromCardId: string;
  toCardId?: string;
  status: BattleStatus;
  createdAt: string;
  resolvedAt?: string;
  winnerId?: string;
  loserId?: string;
  winnerCardId?: string;
  loserCardId?: string;
  playerACardName?: string;
  playerBCardName?: string;
  playerACardPower?: number;
  playerBCardPower?: number;
  winnerCardName?: string;
  loserCardName?: string;
  winnerCardPower?: number;
  loserCardPower?: number;
  message?: string;
};

const battles: BattleProposal[] = [];

export function createBattleProposal(
  fromPlayerId: string,
  toPlayerId: string,
  fromCardId: string
): { success: boolean; message: string; battle?: BattleProposal } {
  if (fromPlayerId === toPlayerId) {
    return { success: false, message: "Impossible de combattre contre soi-meme" };
  }

  const fromPlayer = getPlayer(fromPlayerId);
  const toPlayer = getPlayer(toPlayerId);
  if (!fromPlayer || !toPlayer) {
    return { success: false, message: "Joueur introuvable" };
  }

  const fromCard = getCardById(fromCardId);
  if (!fromCard) {
    return { success: false, message: "Carte introuvable" };
  }
  if (!fromPlayer.inventory.includes(fromCardId)) {
    return { success: false, message: `${fromPlayer.name} ne possede pas cette carte` };
  }

  const battle: BattleProposal = {
    id: uuidv4(),
    fromPlayerId,
    toPlayerId,
    fromCardId,
    status: "pending",
    createdAt: new Date().toISOString()
  };
  battles.push(battle);

  return {
    success: true,
    message: "Defi de bataille envoye",
    battle
  };
}

export function getBattlesForPlayer(playerId: string): BattleProposal[] {
  return battles.filter((battle) =>
    battle.fromPlayerId === playerId || battle.toPlayerId === playerId
  );
}

export function acceptBattle(
  battleId: string,
  playerId: string,
  cardId: string
): { success: boolean; message: string; battle?: BattleProposal } {
  const battle = battles.find((item) => item.id === battleId);
  if (!battle) {
    return { success: false, message: "Defi introuvable" };
  }
  if (battle.status !== "pending") {
    return { success: false, message: "Ce defi n'est plus en attente" };
  }
  if (battle.toPlayerId !== playerId) {
    return { success: false, message: "Seul le destinataire peut accepter le defi" };
  }

  const result = battleCards(
    battle.fromPlayerId,
    battle.toPlayerId,
    battle.fromCardId,
    cardId
  );
  if (!result.success) {
    battle.status = "rejected";
    battle.resolvedAt = new Date().toISOString();
    battle.message = result.message;
    return { success: false, message: result.message, battle };
  }

  battle.status = "accepted";
  battle.toCardId = cardId;
  battle.resolvedAt = new Date().toISOString();
  battle.winnerId = result.winnerId;
  battle.loserId = result.loserId;
  battle.winnerCardId = result.winnerCardId;
  battle.loserCardId = result.loserCardId;
  battle.playerACardName = result.playerACardName;
  battle.playerBCardName = result.playerBCardName;
  battle.playerACardPower = result.playerACardPower;
  battle.playerBCardPower = result.playerBCardPower;
  battle.winnerCardName = result.winnerCardName;
  battle.loserCardName = result.loserCardName;
  battle.winnerCardPower = result.winnerCardPower;
  battle.loserCardPower = result.loserCardPower;
  battle.message = result.message;

  return {
    success: true,
    message: result.message,
    battle
  };
}

export function rejectBattle(
  battleId: string,
  playerId: string
): { success: boolean; message: string; battle?: BattleProposal } {
  const battle = battles.find((item) => item.id === battleId);
  if (!battle) {
    return { success: false, message: "Defi introuvable" };
  }
  if (battle.status !== "pending") {
    return { success: false, message: "Ce defi n'est plus en attente" };
  }
  if (battle.toPlayerId !== playerId) {
    return { success: false, message: "Seul le destinataire peut refuser le defi" };
  }

  battle.status = "rejected";
  battle.resolvedAt = new Date().toISOString();
  battle.message = "Defi refuse";
  return {
    success: true,
    message: "Defi refuse",
    battle
  };
}
