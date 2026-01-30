"use client";

import { useEffect, useState } from "react";
import { format, startOfWeek, endOfWeek, addWeeks, startOfDay, endOfDay } from "date-fns";
import { ptBR } from "date-fns/locale";
import { Calendar } from "@/components/ui/calendar";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { useAuth } from "@/hooks/use-auth";
import { adminApi, Appointment, AppointmentStatus } from "@/lib/api";
import { useToast } from "@/hooks/use-toast";
import { formatTime } from "@/lib/utils";
import { ChevronLeft, ChevronRight, Loader2 } from "lucide-react";

const statusColors: Record<string, "default" | "secondary" | "destructive" | "success" | "warning"> = {
  CONFIRMED: "success",
  SCHEDULED: "secondary",
  CANCELLED: "destructive",
  CANCELLED_LATE: "destructive",
  ATTENDED: "default",
  NO_SHOW: "warning",
};

const statusLabels: Record<string, string> = {
  CONFIRMED: "Confirmado",
  SCHEDULED: "Agendado",
  CANCELLED: "Cancelado",
  CANCELLED_LATE: "Canc. Tardio",
  ATTENDED: "Realizado",
  NO_SHOW: "Falta",
};

export default function AgendamentosPage() {
  const { token } = useAuth();
  const { toast } = useToast();

  const [selectedDate, setSelectedDate] = useState<Date>(new Date());
  const [weekStart, setWeekStart] = useState(startOfWeek(new Date(), { weekStartsOn: 1 }));

  // Atualiza a semana quando a data selecionada mudar
  const handleDateSelect = (date: Date | undefined) => {
    if (date) {
      setSelectedDate(date);
      const newWeekStart = startOfWeek(date, { weekStartsOn: 1 });
      if (newWeekStart.getTime() !== weekStart.getTime()) {
        setWeekStart(newWeekStart);
      }
    }
  };
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedAppointment, setSelectedAppointment] = useState<Appointment | null>(null);

  useEffect(() => {
    loadAppointments();
  }, [weekStart, token]);

  async function loadAppointments() {
    if (!token) return;

    setLoading(true);
    try {
      const end = endOfWeek(weekStart, { weekStartsOn: 1 });
      const data = await adminApi.getAppointments(
        token,
        weekStart.toISOString(),
        end.toISOString(),
        true
      );
      setAppointments(data);
    } catch (error) {
      console.error("Erro ao carregar agendamentos:", error);
    } finally {
      setLoading(false);
    }
  }

  const handleStatusChange = async (appointmentId: string, newStatus: AppointmentStatus) => {
    if (!token) return;

    try {
      await adminApi.updateAppointmentStatus(token, appointmentId, newStatus);
      toast({ title: "Status atualizado" });
      loadAppointments();

      if (selectedAppointment?.id === appointmentId) {
        const updated = await adminApi.getAppointment(token, appointmentId);
        setSelectedAppointment(updated);
      }
    } catch (error: any) {
      toast({
        title: "Erro",
        description: error.message,
        variant: "destructive",
      });
    }
  };

  const dayAppointments = appointments.filter((apt) => {
    const aptDate = new Date(apt.startDateTime);
    return (
      aptDate >= startOfDay(selectedDate) &&
      aptDate <= endOfDay(selectedDate)
    );
  });

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Agenda</h1>
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="icon"
            onClick={() => setWeekStart(addWeeks(weekStart, -1))}
          >
            <ChevronLeft className="h-4 w-4" />
          </Button>
          <span className="text-sm font-medium min-w-[200px] text-center">
            {format(weekStart, "d MMM", { locale: ptBR })} -{" "}
            {format(endOfWeek(weekStart, { weekStartsOn: 1 }), "d MMM yyyy", { locale: ptBR })}
          </span>
          <Button
            variant="outline"
            size="icon"
            onClick={() => setWeekStart(addWeeks(weekStart, 1))}
          >
            <ChevronRight className="h-4 w-4" />
          </Button>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Calendar */}
        <Card>
          <CardContent className="pt-6">
            <Calendar
              mode="single"
              selected={selectedDate}
              onSelect={handleDateSelect}
              locale={ptBR}
              className="rounded-md border"
            />
          </CardContent>
        </Card>

        {/* Day appointments */}
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>
              {format(selectedDate, "EEEE, d 'de' MMMM", { locale: ptBR })}
            </CardTitle>
          </CardHeader>
          <CardContent>
            {loading ? (
              <div className="flex justify-center py-8">
                <Loader2 className="h-8 w-8 animate-spin text-primary" />
              </div>
            ) : dayAppointments.length === 0 ? (
              <p className="text-muted-foreground text-center py-8">
                Nenhum agendamento neste dia
              </p>
            ) : (
              <div className="space-y-3">
                {dayAppointments
                  .sort((a, b) => new Date(a.startDateTime).getTime() - new Date(b.startDateTime).getTime())
                  .map((apt) => (
                    <div
                      key={apt.id}
                      className={`p-4 rounded-lg border cursor-pointer transition-colors ${
                        selectedAppointment?.id === apt.id
                          ? "border-primary bg-primary/5"
                          : "hover:bg-gray-50"
                      }`}
                      onClick={() => setSelectedAppointment(apt)}
                    >
                      <div className="flex items-center justify-between">
                        <div>
                          <p className="font-medium">{apt.patient.name}</p>
                          <p className="text-sm text-muted-foreground">
                            {formatTime(apt.startDateTime)} - {formatTime(apt.endDateTime)}
                          </p>
                          <p className="text-sm text-muted-foreground">
                            {apt.sessionType.name}
                          </p>
                        </div>
                        <Badge variant={statusColors[apt.status]}>
                          {statusLabels[apt.status]}
                        </Badge>
                      </div>
                    </div>
                  ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Selected appointment details */}
      {selectedAppointment && (
        <Card>
          <CardHeader>
            <CardTitle>Detalhes do Agendamento</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid gap-4 md:grid-cols-2">
              <div>
                <p className="text-sm text-muted-foreground">Paciente</p>
                <p className="font-medium">{selectedAppointment.patient.name}</p>
                <p className="text-sm">{selectedAppointment.patient.phone}</p>
                {selectedAppointment.patient.email && (
                  <p className="text-sm">{selectedAppointment.patient.email}</p>
                )}
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Sessao</p>
                <p className="font-medium">{selectedAppointment.sessionType.name}</p>
                <p className="text-sm">
                  {format(new Date(selectedAppointment.startDateTime), "dd/MM/yyyy HH:mm")}
                </p>
              </div>
            </div>

            <div>
              <p className="text-sm text-muted-foreground mb-2">Alterar Status</p>
              <Select
                value={selectedAppointment.status}
                onValueChange={(value) =>
                  handleStatusChange(selectedAppointment.id, value as AppointmentStatus)
                }
              >
                <SelectTrigger className="w-48">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="CONFIRMED">Confirmado</SelectItem>
                  <SelectItem value="ATTENDED">Realizado</SelectItem>
                  <SelectItem value="NO_SHOW">Falta</SelectItem>
                  <SelectItem value="CANCELLED">Cancelado</SelectItem>
                  <SelectItem value="CANCELLED_LATE">Cancelado Tardio</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {selectedAppointment.payment && (
              <div>
                <p className="text-sm text-muted-foreground">Pagamento</p>
                <Badge variant={
                  selectedAppointment.payment.status === "PAID" ? "success" :
                  selectedAppointment.payment.status === "WAIVED" ? "secondary" : "warning"
                }>
                  {selectedAppointment.payment.status === "PAID" ? "Pago" :
                   selectedAppointment.payment.status === "WAIVED" ? "Isento" : "Pendente"}
                </Badge>
              </div>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
