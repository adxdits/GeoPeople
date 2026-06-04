import { Router, Request, Response } from "express";
import { getAllCards, getCardById, getCardsNearby } from "../services/cardsService";

const router = Router();

// GET /api/cards
router.get("/", (req: Request, res: Response) => {
  const cards = getAllCards();
  res.json(cards);
});

// GET /api/cards/nearby?lat=X&lon=Y&radius=Z
router.get("/nearby", (req: Request, res: Response) => {
  const lat = parseFloat(req.query.lat as string);
  const lon = parseFloat(req.query.lon as string);
  const radius = parseFloat(req.query.radius as string) || 20;

  if (isNaN(lat) || isNaN(lon)) {
    res.status(400).json({ error: "Paramètres lat et lon requis" });
    return;
  }
  const cards = getCardsNearby(lat, lon, radius);
  res.json(cards);
});

router.get("/:id/history", (req, res) => {
  const card = getCardById(req.params.id);

  if (!card) {
    return res.status(404).json({ error: "Card not found" });
  }

  res.json(card.history);
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
