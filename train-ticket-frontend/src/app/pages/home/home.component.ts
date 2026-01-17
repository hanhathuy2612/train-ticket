import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SearchFormComponent } from '../../components/search-form/search-form.component';
import { ScheduleListComponent } from '../../components/schedule-list/schedule-list.component';
import { BookingFormComponent } from '../../components/booking-form/booking-form.component';
import { ScheduleService } from '../../services/schedule.service';
import { SearchTrainRequest } from '../../models/search-train-request.interface';
import { ScheduleResponse } from '../../models/schedule-response.interface';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule,
    SearchFormComponent,
    ScheduleListComponent,
    BookingFormComponent
  ],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent {
  private readonly scheduleService = inject(ScheduleService);

  schedules = signal<ScheduleResponse[]>([]);
  isLoading = signal(false);
  selectedSchedule = signal<ScheduleResponse | null>(null);
  isBookingModalOpen = signal(false);
  searchParams = signal<{ seatClass: string; passengers: number } | null>(null);
  errorMessage = signal<string | null>(null);
  bookingSuccess = signal(false);

  onSearch(request: SearchTrainRequest): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.bookingSuccess.set(false);
    this.searchParams.set({
      seatClass: request.seatClass || 'ECONOMY',
      passengers: request.passengers || 1
    });

    this.scheduleService.searchSchedules(request).subscribe({
      next: (response) => {
        this.isLoading.set(false);
        if (response.success && response.data) {
          this.schedules.set(response.data);
          if (response.data.length === 0) {
            this.errorMessage.set('Không tìm thấy chuyến tàu nào phù hợp với yêu cầu của bạn.');
          }
        } else {
          this.errorMessage.set(response.message || 'Không tìm thấy chuyến tàu nào.');
          this.schedules.set([]);
        }
      },
      error: (error) => {
        this.isLoading.set(false);
        this.errorMessage.set(error.error?.message || 'Có lỗi xảy ra khi tìm kiếm chuyến tàu.');
        this.schedules.set([]);
      }
    });
  }

  onSelectSchedule(schedule: ScheduleResponse): void {
    this.selectedSchedule.set(schedule);
    this.isBookingModalOpen.set(true);
  }

  onCloseBooking(): void {
    this.isBookingModalOpen.set(false);
    this.selectedSchedule.set(null);
  }

  onBookingSuccess(): void {
    this.bookingSuccess.set(true);
    this.isBookingModalOpen.set(false);
    this.selectedSchedule.set(null);
    // Optionally refresh the schedule list or show success message
    setTimeout(() => {
      this.bookingSuccess.set(false);
    }, 5000);
  }

  getSelectedSchedule(): ScheduleResponse | null {
    const schedule = this.selectedSchedule();
    return schedule;
  }

  getSeatClass(): string {
    return this.searchParams()?.seatClass || 'ECONOMY';
  }

  getNumberOfPassengers(): number {
    return this.searchParams()?.passengers || 1;
  }
}
