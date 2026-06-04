export type CardHistory = {
  playerId: string;
  action: "capture" | "exchange" | "battle";
  date: string;
};

export type Card = {
  id: string;
  personId: number;
  personName: string;
  placeId: number;
  placeName: string;
  relationName: string;
  latitude: number;
  longitude: number;
  zone: boolean;
  power: number;
  capturedBy?: string;
  capturedAt?: string;
  history: CardHistory[];
};