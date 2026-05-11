export type LocalizedText = {
  en?: string;
  fr?: string;
};

export type PersonPlace = {
  id: number;
  relation_id: number;
  relation_name: LocalizedText;
  name: LocalizedText;
  location: [number, number];
  zone: boolean;
};

export type PersonPlaceEntry = {
  id: number;
  name: LocalizedText;
  places: PersonPlace[];
};
