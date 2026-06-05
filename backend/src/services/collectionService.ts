import { getPlayerInventory } from "./playerService";
import { calculateScoreDetails } from "./scoringService";

export function getPlayerCollections(playerId: string) {
  const inventory = getPlayerInventory(playerId);
  return calculateScoreDetails(inventory);
}
