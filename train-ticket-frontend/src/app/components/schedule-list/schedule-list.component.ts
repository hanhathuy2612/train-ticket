import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ScheduleResponse } from '../../models/schedule-response.interface';

@Component({
  selector: 'app-schedule-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './schedule-list.component.html',
  styleUrl: './schedule-list.component.scss'
})
export class ScheduleListComponent {
  schedules = input.required<ScheduleResponse[]>();
  isLoading = input<boolean>(false);
  selectedSchedule = output<ScheduleResponse>();

  onSelectSchedule(schedule: ScheduleResponse): void {
    this.selectedSchedule.emit(schedule);
  }

  getPrice(schedule: ScheduleResponse, seatClass: string): number {
    if (!schedule.train?.prices) {
      return 0;
    }
    switch (seatClass) {
      case 'ECONOMY':
        return schedule.train.prices.economy;
      case 'BUSINESS':
        return schedule.train.prices.business;
      case 'FIRST':
        return schedule.train.prices.firstClass;
      default:
        return schedule.train.prices.economy;
    }
  }

  getAvailableSeats(schedule: ScheduleResponse, seatClass: string): number {
    if (!schedule.availability) {
      return 0;
    }
    switch (seatClass) {
      case 'ECONOMY':
        return schedule.availability.economyAvailable;
      case 'BUSINESS':
        return schedule.availability.businessAvailable;
      case 'FIRST':
        return schedule.availability.firstClassAvailable;
      default:
        return schedule.availability.totalAvailable;
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
