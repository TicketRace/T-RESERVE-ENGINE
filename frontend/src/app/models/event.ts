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
  venue: string;
  imageUrl: string;
  category: string;
  ageRating: string;
  tagline: string;
  featured: boolean;
  sessions: EventSession[];
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
