import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../models/api-response.interface';
import { ScheduleResponse } from '../models/schedule-response.interface';
import { SearchTrainRequest } from '../models/search-train-request.interface';

@Injectable({
  providedIn: 'root'
})
export class ScheduleService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/inventory/schedules`;

  searchSchedules(request: SearchTrainRequest): Observable<ApiResponse<ScheduleResponse[]>> {
    return this.http.post<ApiResponse<ScheduleResponse[]>>(
      `${this.apiUrl}/search`,
      request
    );
  }

  getAvailability(request: SearchTrainRequest): Observable<ApiResponse<ScheduleResponse[]>> {
    return this.http.post<ApiResponse<ScheduleResponse[]>>(
      `${this.apiUrl}/availability`,
      request
    );
  }
}
