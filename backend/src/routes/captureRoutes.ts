import { Router, Request, Response } from "express";
import { captureCard, getPlayerCaptures } from "../services/captureService";

const router = Router();

// POST /api/captures
router.post("/", (req: Request, res: Response) => {
  const { playerId, cardId, latitude, longitude, miniGameSuccess } = req.body;

  if (!playerId || !cardId || typeof latitude !== "number" ||typeof longitude !== "number" || typeof miniGameSuccess !== "boolean") {
    res.status(400).json({
      error: "playerId, cardId, latitude, longitude et miniGameSuccess sont requis"
    });
    return;
  }

  const result = captureCard(
    playerId,
    cardId,
    latitude,
    longitude,
    miniGameSuccess
  );

  if (!result.success) {
    res.status(400).json(result);
    return;
  }

  res.status(201).json(result);
});

// GET /api/captures/:playerId
router.get("/:playerId", (req: Request, res: Response) => {
  const captures = getPlayerCaptures(req.params.playerId as string);
  res.json(captures);
});

export default router;
