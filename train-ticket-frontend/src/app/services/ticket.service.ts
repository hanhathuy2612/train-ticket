import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../models/api-response.interface';
import { BookTicketRequest } from '../models/book-ticket-request.interface';
import { TicketResponse } from '../models/ticket-response.interface';

@Injectable({
  providedIn: 'root'
})
export class TicketService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/tickets`;

  bookTicket(request: BookTicketRequest): Observable<ApiResponse<TicketResponse>> {
    return this.http.post<ApiResponse<TicketResponse>>(
      `${this.apiUrl}/book`,
      request
    );
  }
}
