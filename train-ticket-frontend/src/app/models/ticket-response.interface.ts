export interface TicketResponse {
  id: number;
  userId: number;
  trainId: number;
  departureDate: string;
  numberOfSeats: number;
  totalPrice: number;
  status: string;
  statusDescription: string;
  createdAt?: string;
  updatedAt?: string;
  trainInfo?: TrainInfo;
  paymentInfo?: PaymentInfo;
  seatNumbers?: string[];
}

export interface TrainInfo {
  trainNumber: string;
  origin: string;
  destination: string;
  departureTime: string;
  arrivalTime: string;
}

export interface PaymentInfo {
  paymentId: number;
  paymentStatus: string;
  amount: number;
  paidAt?: string;
}
