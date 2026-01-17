export interface BookTicketRequest {
  trainId: number;
  numberOfSeats: number;
  departureDate: string; // Format: yyyy-MM-dd HH:mm
  totalPrice: number;
  seatNumbers?: string[];
  passengers?: PassengerInfo[];
}

export interface PassengerInfo {
  name: string;
  idNumber?: string;
  phoneNumber?: string;
}
