const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8085";

interface FetchOptions extends RequestInit {
  token?: string;
}

async function fetchApi<T>(
  endpoint: string,
  options: FetchOptions = {}
): Promise<T> {
  const { token, ...fetchOptions } = options;

  const headers: HeadersInit = {
    "Content-Type": "application/json",
    ...options.headers,
  };

  if (token) {
    (headers as Record<string, string>)["Authorization"] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_URL}${endpoint}`, {
    ...fetchOptions,
    headers,
  });

  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: "Erro desconhecido" }));
    throw new Error(error.message || `HTTP ${response.status}`);
  }

  if (response.status === 204) {
    return {} as T;
  }

  return response.json();
}

// Public API
export const publicApi = {
  getSessionTypes: () =>
    fetchApi<SessionType[]>("/api/public/session-types"),

  getAvailableSlots: (date: string, sessionTypeId: string) =>
    fetchApi<AvailableSlotResponse>(
      `/api/public/slots?date=${date}&sessionTypeId=${sessionTypeId}`
    ),

  getAvailableSlotsRange: (startDate: string, endDate: string, sessionTypeId: string) =>
    fetchApi<AvailableSlotResponse[]>(
      `/api/public/slots/range?startDate=${startDate}&endDate=${endDate}&sessionTypeId=${sessionTypeId}`
    ),

  getConsent: () =>
    fetchApi<ConsentVersion>("/api/public/consent"),

  createBooking: (data: BookingRequest) =>
    fetchApi<BookingConfirmation>("/api/public/book", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  getAppointmentByToken: (token: string) =>
    fetchApi<Appointment>(`/api/public/appointment?token=${token}`),

  cancelAppointment: (token: string, reason?: string) =>
    fetchApi<void>("/api/public/cancel", {
      method: "POST",
      body: JSON.stringify({ cancellationToken: token, reason }),
    }),

  rescheduleAppointment: (token: string, newStartDateTime: string) =>
    fetchApi<BookingConfirmation>("/api/public/reschedule", {
      method: "POST",
      body: JSON.stringify({ cancellationToken: token, newStartDateTime }),
    }),
};

// Auth API
export const authApi = {
  login: (email: string, password: string) =>
    fetchApi<AuthResponse>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    }),

  refresh: (refreshToken: string) =>
    fetchApi<AuthResponse>("/api/auth/refresh", {
      method: "POST",
      body: JSON.stringify({ refreshToken }),
    }),
};

// Admin API
export const adminApi = {
  // Dashboard
  getDashboard: (token: string) =>
    fetchApi<Dashboard>("/api/admin/dashboard", { token }),

  // Appointments
  getAppointments: (token: string, start: string, end: string, includeAll = false) =>
    fetchApi<Appointment[]>(
      `/api/admin/appointments?start=${start}&end=${end}&includeAll=${includeAll}`,
      { token }
    ),

  getAppointment: (token: string, id: string) =>
    fetchApi<Appointment>(`/api/admin/appointments/${id}`, { token }),

  createAppointment: (token: string, data: CreateAppointmentRequest) =>
    fetchApi<Appointment>("/api/admin/appointments", {
      method: "POST",
      body: JSON.stringify(data),
      token,
    }),

  updateAppointmentStatus: (token: string, id: string, status: string, reason?: string) =>
    fetchApi<Appointment>(`/api/admin/appointments/${id}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status, reason }),
      token,
    }),

  // Session Types
  getSessionTypes: (token: string) =>
    fetchApi<SessionType[]>("/api/admin/session-types", { token }),

  createSessionType: (token: string, data: SessionTypeRequest) =>
    fetchApi<SessionType>("/api/admin/session-types", {
      method: "POST",
      body: JSON.stringify(data),
      token,
    }),

  updateSessionType: (token: string, id: string, data: SessionTypeRequest) =>
    fetchApi<SessionType>(`/api/admin/session-types/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
      token,
    }),

  deleteSessionType: (token: string, id: string) =>
    fetchApi<void>(`/api/admin/session-types/${id}`, {
      method: "DELETE",
      token,
    }),

  // Availability
  getSchedule: (token: string) =>
    fetchApi<Availability[]>("/api/admin/availability/schedule", { token }),

  createAvailability: (token: string, data: AvailabilityRequest) =>
    fetchApi<Availability>("/api/admin/availability/schedule", {
      method: "POST",
      body: JSON.stringify(data),
      token,
    }),

  updateAvailability: (token: string, id: string, data: AvailabilityRequest) =>
    fetchApi<Availability>(`/api/admin/availability/schedule/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
      token,
    }),

  deleteAvailability: (token: string, id: string) =>
    fetchApi<void>(`/api/admin/availability/schedule/${id}`, {
      method: "DELETE",
      token,
    }),

  // Blocks
  getBlocks: (token: string) =>
    fetchApi<Block[]>("/api/admin/availability/blocks", { token }),

  createBlock: (token: string, data: BlockRequest) =>
    fetchApi<Block>("/api/admin/availability/blocks", {
      method: "POST",
      body: JSON.stringify(data),
      token,
    }),

  deleteBlock: (token: string, id: string) =>
    fetchApi<void>(`/api/admin/availability/blocks/${id}`, {
      method: "DELETE",
      token,
    }),

  // Recurrence
  getRecurringSeries: (token: string) =>
    fetchApi<RecurringSeries[]>("/api/admin/recurrence", { token }),

  getRecurringSeriesById: (token: string, id: string) =>
    fetchApi<RecurringSeries>(`/api/admin/recurrence/${id}`, { token }),

  checkConflicts: (token: string, data: RecurringSeriesRequest) =>
    fetchApi<ConflictCheckResponse>("/api/admin/recurrence/check-conflicts", {
      method: "POST",
      body: JSON.stringify(data),
      token,
    }),

  checkRecurrenceConflicts: (token: string, data: { sessionTypeId: string; dayOfWeek: string; time: string; frequency: "WEEKLY" | "BIWEEKLY"; startDate: string; endDate: string }) =>
    fetchApi<{ available: boolean; conflictingDates?: string[] }>("/api/admin/recurrence/check-conflicts", {
      method: "POST",
      body: JSON.stringify({ ...data, startTime: data.time }),
      token,
    }),

  createRecurringSeries: (token: string, data: RecurringSeriesRequest | { patientId: string; sessionTypeId: string; dayOfWeek: string; time: string; frequency: "WEEKLY" | "BIWEEKLY"; startDate: string; endDate: string }) =>
    fetchApi<RecurringSeries>("/api/admin/recurrence", {
      method: "POST",
      body: JSON.stringify('time' in data ? { ...data, startTime: data.time } : data),
      token,
    }),

  deleteRecurringSeries: (token: string, id: string) =>
    fetchApi<void>(`/api/admin/recurrence/${id}`, {
      method: "DELETE",
      token,
    }),

  cancelRecurringSeries: (token: string, id: string, reason?: string) =>
    fetchApi<void>(`/api/admin/recurrence/${id}${reason ? `?reason=${reason}` : ""}`, {
      method: "DELETE",
      token,
    }),

  // Payments
  getPendingPayments: (token: string) =>
    fetchApi<Payment[]>("/api/admin/payments/pending", { token }),

  markAsPaid: (token: string, appointmentId: string, receiptNumber?: string) =>
    fetchApi<Payment>(
      `/api/admin/payments/appointment/${appointmentId}/mark-paid${receiptNumber ? `?receiptNumber=${receiptNumber}` : ""}`,
      { method: "POST", token }
    ),

  waivePayment: (token: string, appointmentId: string, reason?: string) =>
    fetchApi<Payment>(
      `/api/admin/payments/appointment/${appointmentId}/waive${reason ? `?reason=${reason}` : ""}`,
      { method: "POST", token }
    ),

  // Patients
  getPatients: (token: string) =>
    fetchApi<Patient[]>("/api/admin/patients", { token }),

  searchPatients: (token: string, name: string) =>
    fetchApi<Patient[]>(`/api/admin/patients/search?name=${encodeURIComponent(name)}`, { token }),
};

// Types
export interface SessionType {
  id: string;
  name: string;
  durationMinutes: number;
  price: number;
  description?: string;
  active: boolean;
  displayOrder: number;
}

export interface AvailableSlotResponse {
  date: string;
  slots: TimeSlot[];
}

export interface TimeSlot {
  time: string;
  dateTime: string;
  available: boolean;
}

export interface ConsentVersion {
  id: string;
  version: string;
  content: string;
  effectiveFrom: string;
  active: boolean;
}

export interface BookingRequest {
  sessionTypeId: string;
  startDateTime: string;
  patientName: string;
  patientPhone: string;
  patientEmail?: string;
  consentAccepted: boolean;
  consentVersion?: string;
}

export interface BookingConfirmation {
  appointmentId: string;
  patientName: string;
  sessionTypeName: string;
  startDateTime: string;
  endDateTime: string;
  cancellationToken: string;
  cancellationUrl: string;
  rescheduleUrl: string;
  message: string;
}

export interface Patient {
  id: string;
  name: string;
  phone: string;
  email?: string;
  active: boolean;
  anonymized: boolean;
  createdAt: string;
}

export interface Appointment {
  id: string;
  patient: Patient;
  sessionType: SessionType;
  recurringSeriesId?: string;
  startDateTime: string;
  endDateTime: string;
  status: AppointmentStatus;
  sessionLink?: string;
  cancellationToken?: string;
  cancelledAt?: string;
  cancelledBy?: string;
  cancellationReason?: string;
  payment?: PaymentInfo;
  createdAt: string;
}

export type AppointmentStatus =
  | "SCHEDULED"
  | "CONFIRMED"
  | "CANCELLED"
  | "CANCELLED_LATE"
  | "ATTENDED"
  | "NO_SHOW";

export interface PaymentInfo {
  id: string;
  status: PaymentStatus;
  amount: number;
  paidAt?: string;
  receiptNumber?: string;
}

export type PaymentStatus = "UNPAID" | "PAID" | "WAIVED";

export interface Payment {
  id: string;
  appointmentId: string;
  status: PaymentStatus;
  amount: number;
  paidAt?: string;
  receiptNumber?: string;
  notes?: string;
  appointment?: Appointment;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: {
    id: string;
    email: string;
    name: string;
  };
}

export interface Dashboard {
  todayAppointments: Appointment[];
  upcomingAppointments: Appointment[];
  statistics: {
    totalAppointmentsToday: number;
    totalAppointmentsThisWeek: number;
    pendingPayments: number;
    revenueThisMonth: number;
  };
}

export interface Availability {
  id: string;
  dayOfWeek: DayOfWeek;
  startTime: string;
  endTime: string;
  active: boolean;
}

export type DayOfWeek =
  | "MONDAY"
  | "TUESDAY"
  | "WEDNESDAY"
  | "THURSDAY"
  | "FRIDAY"
  | "SATURDAY"
  | "SUNDAY";

export interface Block {
  id: string;
  startDateTime: string;
  endDateTime: string;
  blockType: BlockType;
  reason?: string;
}

export type BlockType = "VACATION" | "HOLIDAY" | "BREAK";

export interface RecurringSeries {
  id: string;
  patient?: Patient;
  patientId: string;
  sessionType?: SessionType;
  sessionTypeId: string;
  dayOfWeek: DayOfWeek;
  startTime: string;
  time: string;
  frequency: RecurrenceFrequency;
  startDate: string;
  endDate: string;
  active: boolean;
  appointments?: Appointment[];
}

export type RecurrenceFrequency = "WEEKLY" | "BIWEEKLY";

export interface ConflictCheckResponse {
  hasConflicts: boolean;
  conflicts: { dateTime: string; reason: string }[];
  totalOccurrences: number;
  conflictCount: number;
}

export interface SessionTypeRequest {
  name: string;
  durationMinutes: number;
  price: number;
  description?: string;
  active?: boolean;
  displayOrder?: number;
}

export interface AvailabilityRequest {
  dayOfWeek: DayOfWeek;
  startTime: string;
  endTime: string;
  active?: boolean;
}

export interface BlockRequest {
  startDateTime: string;
  endDateTime: string;
  blockType: BlockType;
  reason?: string;
}

export interface CreateAppointmentRequest {
  patientId?: string;
  patientName?: string;
  patientPhone?: string;
  patientEmail?: string;
  sessionTypeId: string;
  startDateTime: string;
  status?: AppointmentStatus;
  sessionLink?: string;
}

export interface RecurringSeriesRequest {
  patientId?: string;
  patientName?: string;
  patientPhone?: string;
  patientEmail?: string;
  sessionTypeId: string;
  dayOfWeek: DayOfWeek;
  startTime: string;
  frequency: RecurrenceFrequency;
  startDate: string;
  endDate?: string;
}
