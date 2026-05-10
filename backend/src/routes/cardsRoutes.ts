import { Router } from "express";
import { getAllCards } from "../services/cardsService";

const router = Router();

router.get("/", (req, res) => {
  const cards = getAllCards();
  res.json(cards);
});

export default router;
