import { Router, Request, Response } from "express";
import {
  acceptTrade,
  createTradeProposal,
  getTradesForPlayer,
  rejectTrade
} from "../services/tradeService";

const router = Router();

// POST /api/trades
router.post("/", (req: Request, res: Response) => {
  const { fromPlayerId, toPlayerId, fromCardId, toCardId } = req.body;

  if (
    typeof fromPlayerId !== "string" ||
    typeof toPlayerId !== "string" ||
    typeof fromCardId !== "string" ||
    typeof toCardId !== "string"
  ) {
    res.status(400).json({
      success: false,
      message: "fromPlayerId, toPlayerId, fromCardId et toCardId sont requis"
    });
    return;
  }

  const result = createTradeProposal(fromPlayerId, toPlayerId, fromCardId, toCardId);
  if (!result.success) {
    res.status(400).json(result);
    return;
  }

  res.status(201).json(result);
});

// GET /api/trades/player/:playerId
router.get("/player/:playerId", (req: Request, res: Response) => {
  res.json(getTradesForPlayer(req.params.playerId as string));
});

// PUT /api/trades/:id/accept
router.put("/:id/accept", (req: Request, res: Response) => {
  const { playerId } = req.body;
  if (typeof playerId !== "string") {
    res.status(400).json({ success: false, message: "playerId est requis" });
    return;
  }

  const result = acceptTrade(req.params.id as string, playerId);
  if (!result.success) {
    res.status(400).json(result);
    return;
  }

  res.json(result);
});

// PUT /api/trades/:id/reject
router.put("/:id/reject", (req: Request, res: Response) => {
  const { playerId } = req.body;
  if (typeof playerId !== "string") {
    res.status(400).json({ success: false, message: "playerId est requis" });
    return;
  }

  const result = rejectTrade(req.params.id as string, playerId);
  if (!result.success) {
    res.status(400).json(result);
    return;
  }

  res.json(result);
});

export default router;
