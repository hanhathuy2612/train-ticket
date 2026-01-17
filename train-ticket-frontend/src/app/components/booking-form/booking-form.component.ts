import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ScheduleResponse } from '../../models/schedule-response.interface';
import { BookTicketRequest, PassengerInfo } from '../../models/book-ticket-request.interface';
import { TicketService } from '../../services/ticket.service';

@Component({
  selector: 'app-booking-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './booking-form.component.html',
  styleUrl: './booking-form.component.scss'
})
export class BookingFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly ticketService = inject(TicketService);

  schedule = input.required<ScheduleResponse>();
  isOpen = input<boolean>(false);
  seatClass = input<string>('ECONOMY');
  numberOfPassengers = input<number>(1);

  closed = output<void>();
  bookingSuccess = output<void>();

  isLoading = signal(false);
  errorMessage = signal<string | null>(null);

  bookingForm: FormGroup = this.fb.group({
    numberOfSeats: [1, [Validators.required, Validators.min(1)]],
    passengers: this.fb.array([])
  });

  totalPrice = computed(() => {
    const scheduleValue = this.schedule();
    const numberOfSeats = this.bookingForm.get('numberOfSeats')?.value || 1;
    const seatClassValue = this.seatClass();
    
    if (!scheduleValue?.train?.prices) {
      return 0;
    }

    let pricePerSeat = 0;
    switch (seatClassValue) {
      case 'ECONOMY':
        pricePerSeat = scheduleValue.train.prices.economy;
        break;
      case 'BUSINESS':
        pricePerSeat = scheduleValue.train.prices.business;
        break;
      case 'FIRST':
        pricePerSeat = scheduleValue.train.prices.firstClass;
        break;
    }

    return pricePerSeat * numberOfSeats;
  });

  constructor() {
    effect(() => {
      const passengers = this.numberOfPassengers();
      const passengersArray = this.bookingForm.get('passengers') as FormArray;
      
      // Clear existing passengers
      while (passengersArray.length > 0) {
        passengersArray.removeAt(0);
      }

      // Add new passenger forms
      for (let i = 0; i < passengers; i++) {
        passengersArray.push(this.createPassengerForm());
      }

      // Update numberOfSeats to match passengers
      this.bookingForm.patchValue({ numberOfSeats: passengers });
    });
  }

  private createPassengerForm(): FormGroup {
    return this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      idNumber: [''],
      phoneNumber: ['', [Validators.pattern(/^[0-9]{10,11}$/)]]
    });
  }

  get passengersArray(): FormArray {
    return this.bookingForm.get('passengers') as FormArray;
  }

  getPassengerForm(index: number): FormGroup {
    return this.passengersArray.at(index) as FormGroup;
  }

  close(): void {
    this.closed.emit();
  }

  onSubmit(): void {
    if (this.bookingForm.valid) {
      this.isLoading.set(true);
      this.errorMessage.set(null);

      const scheduleValue = this.schedule();
      const formValue = this.bookingForm.value;
      
      // Format departure date: yyyy-MM-dd HH:mm
      const departureDate = new Date(scheduleValue.departureDate);
      const departureTime = scheduleValue.train.departureTime.split(':');
      departureDate.setHours(parseInt(departureTime[0]), parseInt(departureTime[1]));
      
      const formattedDate = `${departureDate.getFullYear()}-${String(departureDate.getMonth() + 1).padStart(2, '0')}-${String(departureDate.getDate()).padStart(2, '0')} ${String(departureDate.getHours()).padStart(2, '0')}:${String(departureDate.getMinutes()).padStart(2, '0')}`;

      const passengers: PassengerInfo[] = formValue.passengers.map((p: any) => ({
        name: p.name,
        idNumber: p.idNumber || undefined,
        phoneNumber: p.phoneNumber || undefined
      }));

      const request: BookTicketRequest = {
        trainId: scheduleValue.train.id,
        numberOfSeats: formValue.numberOfSeats,
        departureDate: formattedDate,
        totalPrice: this.totalPrice(),
        passengers: passengers.length > 0 ? passengers : undefined
      };

      this.ticketService.bookTicket(request).subscribe({
        next: (response) => {
          this.isLoading.set(false);
          if (response.success) {
            this.bookingSuccess.emit();
            this.bookingForm.reset();
          } else {
            this.errorMessage.set(response.message || 'Đặt vé thất bại');
          }
        },
        error: (error) => {
          this.isLoading.set(false);
          this.errorMessage.set(error.error?.message || 'Có lỗi xảy ra khi đặt vé');
        }
      });
    } else {
      this.bookingForm.markAllAsTouched();
    }
  }

  formatTime(time: string): string {
    if (!time) return '';
    const date = new Date(`2000-01-01T${time}`);
    return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  }

  formatDate(date: string): string {
    if (!date) return '';
    const d = new Date(date);
    return d.toLocaleDateString('vi-VN', { 
      weekday: 'long', 
      year: 'numeric', 
      month: 'long', 
      day: 'numeric' 
    });
  }
}
