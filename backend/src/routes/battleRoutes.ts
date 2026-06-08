import { Router, Request, Response } from "express";
import {
  acceptBattle,
  createBattleProposal,
  getBattlesForPlayer,
  rejectBattle
} from "../services/battleService";

const router = Router();

// POST /api/battles
router.post("/", (req: Request, res: Response) => {
  const { fromPlayerId, toPlayerId, fromCardId } = req.body;

  if (
    typeof fromPlayerId !== "string" ||
    typeof toPlayerId !== "string" ||
    typeof fromCardId !== "string"
  ) {
    res.status(400).json({
      success: false,
      message: "fromPlayerId, toPlayerId et fromCardId sont requis"
    });
    return;
  }

  const result = createBattleProposal(fromPlayerId, toPlayerId, fromCardId);
  if (!result.success) {
    res.status(400).json(result);
    return;
  }

  res.status(201).json(result);
});

// GET /api/battles/player/:playerId
router.get("/player/:playerId", (req: Request, res: Response) => {
  res.json(getBattlesForPlayer(req.params.playerId as string));
});

// PUT /api/battles/:id/accept
router.put("/:id/accept", (req: Request, res: Response) => {
  const { playerId, cardId } = req.body;
  if (typeof playerId !== "string" || typeof cardId !== "string") {
    res.status(400).json({ success: false, message: "playerId et cardId sont requis" });
    return;
  }

  const result = acceptBattle(req.params.id as string, playerId, cardId);
  if (!result.success) {
    res.status(400).json(result);
    return;
  }

  res.json(result);
});

// PUT /api/battles/:id/reject
router.put("/:id/reject", (req: Request, res: Response) => {
  const { playerId } = req.body;
  if (typeof playerId !== "string") {
    res.status(400).json({ success: false, message: "playerId est requis" });
    return;
  }

  const result = rejectBattle(req.params.id as string, playerId);
  if (!result.success) {
    res.status(400).json(result);
    return;
  }

  res.json(result);
});

export default router;
