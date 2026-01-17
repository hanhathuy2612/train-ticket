import { Component, EventEmitter, inject, output, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { SearchTrainRequest } from '../../models/search-train-request.interface';
import dayjs from 'dayjs';

@Component({
  selector: 'app-search-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './search-form.component.html',
  styleUrl: './search-form.component.scss'
})
export class SearchFormComponent {
  private readonly fb = inject(FormBuilder);
  
  searchSubmitted = output<SearchTrainRequest>();
  isLoading = signal(false);

  searchForm: FormGroup = this.fb.group({
    origin: ['', [Validators.required, Validators.minLength(2)]],
    destination: ['', [Validators.required, Validators.minLength(2)]],
    departureDate: ['', [Validators.required]],
    passengers: [1, [Validators.required, Validators.min(1), Validators.max(10)]],
    seatClass: ['ECONOMY']
  });

  get today(): string {
    return new Date().toISOString().split('T')[0];
  }

  onSubmit(): void {
    if (this.searchForm.valid) {
      const formValue = this.searchForm.value;
      const request: SearchTrainRequest = {
        origin: formValue.origin.trim(),
        destination: formValue.destination,
        departureDate: dayjs(formValue.departureDate),
        passengers: formValue.passengers || 1,
        seatClass: formValue.seatClass || 'ECONOMY'
      };
      this.searchSubmitted.emit(request);
    } else {
      this.searchForm.markAllAsTouched();
    }
  }

  get origin() {
    return this.searchForm.get('origin');
  }

  get destination() {
    return this.searchForm.get('destination');
  }

  get departureDate() {
    return this.searchForm.get('departureDate');
  }

  get passengers() {
    return this.searchForm.get('passengers');
  }
}
