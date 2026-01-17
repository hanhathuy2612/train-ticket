import dayjs from 'dayjs';

export interface SearchTrainRequest {
  origin: string;
  destination: string;
  departureDate: dayjs.Dayjs; // Format: yyyy-MM-dd
  passengers?: number;
  seatClass?: 'ECONOMY' | 'BUSINESS' | 'FIRST';
}
