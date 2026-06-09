export interface EventSession {
  id: number;
  eventId: number;
  startsAt: string;
  price: number;
}

export interface EventItem {
  id: number;
  title: string;
  description: string;
  venue: {
    id: number;
    name: string;
    address: string;
  };
  imageUrl: string | null;
  category: string;
  ageRestriction: string;
  startTime: string;
  basePrice: number;
}

export interface AdminEventSummary {
  id: number;
  title: string;
  venue: string;
  nextSession: string;
}

export interface VenueSeatTemplate {
  id: number;
  row: number;
  column: number;
  label: string;
  disabled: boolean;
}
