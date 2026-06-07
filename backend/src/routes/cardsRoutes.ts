import { Router, Request, Response } from "express";
import { getAllCards, getCardById, getCardsNearby } from "../services/cardsService";
import { getPlayer } from "../services/playerService";

const router = Router();

// GET /api/cards
router.get("/", (req: Request, res: Response) => {
  const cards = getAllCards();
  res.json(cards);
});

// GET /api/cards/nearby?lat=X&lon=Y&radius=Z
router.get("/nearby", async (req: Request, res: Response) => {
  const lat = parseFloat(req.query.lat as string);
  const lon = parseFloat(req.query.lon as string);
  const radius = parseFloat(req.query.radius as string) || 20;
  console.log(`[cards/nearby] lat=${lat} lon=${lon} radiusKm=${radius}`);

  if (isNaN(lat) || isNaN(lon)) {
    res.status(400).json({ error: "Paramètres lat et lon requis" });
    return;
  }
  try {
    const cards = await getCardsNearby(lat, lon, radius);
    console.log(`[cards/nearby] returning ${cards.length} cards`);
    res.json(cards);
  } catch (error) {
    console.error("[cards/nearby] failed", error);
    res.status(500).json({ error: "Impossible de charger les cartes" });
  }
});

router.get("/:id/history", (req, res) => {
  const card = getCardById(req.params.id);

  if (!card) {
    return res.status(404).json({ error: "Card not found" });
  }

  res.json(
    card.history.map((entry) => {
      const player = getPlayer(entry.playerId);
      return {
        ...entry,
        playerName: player?.name ?? entry.playerId
      };
    })
  );
});

// GET /api/cards/:id
router.get("/:id", (req: Request, res: Response) => {
  const card = getCardById(req.params.id as string);
  if (!card) {
    res.status(404).json({ error: "Carte introuvable" });
    return;
  }
  res.json(card);
});

export default router;
