import { Router, Request, Response } from "express";
import {
  registerPlayer,
  getPlayer,
  updatePlayerLocation,
  getPlayerInventory,
  getLeaderboard
} from "../services/playerService";

import { getPlayerCollections } from "../services/collectionService";

const router = Router();

// POST /api/players/register
router.post("/register", (req: Request, res: Response) => {
  const { name } = req.body;
  if (!name || typeof name !== "string") {
    res.status(400).json({ error: "Le champ 'name' est requis" });
    return;
  }
  const player = registerPlayer(name.trim());
  res.status(201).json(player);
});

// GET /api/players/leaderboard
router.get("/leaderboard", (req: Request, res: Response) => {

  const leaderboard = getLeaderboard();

  res.json(leaderboard);
});

// GET /api/players/:id
router.get("/:id", (req: Request, res: Response) => {
  const player = getPlayer(req.params.id as string);
  if (!player) {
    res.status(404).json({ error: "Joueur introuvable" });
    return;
  }
  res.json(player);
});

// PUT /api/players/:id/location
router.put("/:id/location", (req: Request, res: Response) => {
  const { latitude, longitude } = req.body;
  if (typeof latitude !== "number" || typeof longitude !== "number") {
    res.status(400).json({ error: "latitude et longitude requis (numbers)" });
    return;
  }
  try {
    const player = updatePlayerLocation(req.params.id as string, latitude, longitude);
    if (!player) {
      res.status(404).json({ error: "Joueur introuvable" });
      return;
    }
    res.json(player);
  } catch (error) {
    res.status(400).json({
      error: error instanceof Error ? error.message : "Erreur"
    });
  }
});

// GET /api/players/:id/inventory
router.get("/:id/inventory", (req: Request, res: Response) => {
  const player = getPlayer(req.params.id as string);
  if (!player) {
    res.status(404).json({ error: "Joueur introuvable" });
    return;
  }
  const inventory = getPlayerInventory(req.params.id as string);
  res.json({ playerId: req.params.id, inventory });
});

// GET /api/players/:id/score
router.get("/:id/score", (req: Request, res: Response) => {
  const player = getPlayer(req.params.id as string);
  if (!player) {
    res.status(404).json({ error: "Joueur introuvable" });
    return;
  }
  res.json({playerId: player.id,name: player.name, score: player.score });
});

// GET /api/players/:id/collections
router.get("/:id/collections", (req: Request, res: Response) => {
  const player = getPlayer(req.params.id as string);
  if (!player) {
    res.status(404).json({ error: "Joueur introuvable" });
    return;
  }
  const collections = getPlayerCollections(req.params.id as string);

  res.json({ playerId: player.id, collections });
});

export default router;
