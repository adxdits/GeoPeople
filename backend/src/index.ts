import express from "express";
import cors from "cors";
import dotenv from "dotenv";
import cardsRoutes from "./routes/cardsRoutes";

dotenv.config();

const app = express();

app.use(cors());
app.use(express.json());

app.use("/api/cards", cardsRoutes);

const PORT = process.env.PORT || 3000;

app.get("/api/health", (req, res) => {
  res.json({
    status: "ok",
    message: "GeoPeople backend is running"
  });
});

app.listen(PORT, () => {
  console.log(`Server running on http://localhost:${PORT}`);
});
