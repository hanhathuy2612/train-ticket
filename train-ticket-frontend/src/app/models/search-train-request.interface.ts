export interface SearchTrainRequest {
  origin: string;
  destination: string;
  departureDate: string; // Format: yyyy-MM-dd
  passengers?: number;
  seatClass?: 'ECONOMY' | 'BUSINESS' | 'FIRST';
}
