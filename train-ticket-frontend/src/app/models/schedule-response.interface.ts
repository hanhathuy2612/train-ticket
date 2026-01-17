export interface ScheduleResponse {
  id: number;
  train: TrainInfo;
  departureDate: string;
  availability: AvailabilityInfo;
  status: string;
  notes?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface TrainInfo {
  id: number;
  trainNumber: string;
  trainName: string;
  trainType: string;
  origin: string;
  destination: string;
  departureTime: string;
  arrivalTime: string;
  prices: PriceInfo;
}

export interface AvailabilityInfo {
  totalAvailable: number;
  economyAvailable: number;
  businessAvailable: number;
  firstClassAvailable: number;
  reserved: number;
}

export interface PriceInfo {
  economy: number;
  business: number;
  firstClass: number;
}
