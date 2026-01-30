"use client";

import { useEffect, useState } from "react";
import { format } from "date-fns";
import { ptBR } from "date-fns/locale";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { useAuth } from "@/hooks/use-auth";
import { adminApi, Dashboard, Appointment } from "@/lib/api";
import { formatCurrency, formatTime } from "@/lib/utils";
import { Calendar, Clock, CreditCard, TrendingUp, Loader2 } from "lucide-react";
import Link from "next/link";

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

export default function AdminDashboardPage() {
  const { token } = useAuth();
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadDashboard() {
      if (!token) return;

      try {
        const data = await adminApi.getDashboard(token);
        setDashboard(data);
      } catch (error) {
        console.error("Erro ao carregar dashboard:", error);
      } finally {
        setLoading(false);
      }
    }

    loadDashboard();
  }, [token]);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Dashboard</h1>

      {/* Stats */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium">Hoje</CardTitle>
            <Calendar className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {dashboard?.statistics.totalAppointmentsToday || 0}
            </div>
            <p className="text-xs text-muted-foreground">agendamentos</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium">Esta semana</CardTitle>
            <Clock className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {dashboard?.statistics.totalAppointmentsThisWeek || 0}
            </div>
            <p className="text-xs text-muted-foreground">agendamentos</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium">Pendentes</CardTitle>
            <CreditCard className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {dashboard?.statistics.pendingPayments || 0}
            </div>
            <p className="text-xs text-muted-foreground">pagamentos</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium">Receita (mes)</CardTitle>
            <TrendingUp className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {formatCurrency(dashboard?.statistics.revenueThisMonth || 0)}
            </div>
            <p className="text-xs text-muted-foreground">pagos</p>
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* Today's Appointments */}
        <Card>
          <CardHeader>
            <CardTitle>Agendamentos de Hoje</CardTitle>
            <CardDescription>
              {format(new Date(), "EEEE, d 'de' MMMM", { locale: ptBR })}
            </CardDescription>
          </CardHeader>
          <CardContent>
            {dashboard?.todayAppointments.length === 0 ? (
              <p className="text-muted-foreground text-center py-4">
                Nenhum agendamento para hoje
              </p>
            ) : (
              <div className="space-y-3">
                {dashboard?.todayAppointments.map((apt) => (
                  <Link
                    key={apt.id}
                    href={`/admin/agendamentos?id=${apt.id}`}
                    className="flex items-center justify-between p-3 rounded-lg bg-gray-50 hover:bg-gray-100 transition-colors"
                  >
                    <div>
                      <p className="font-medium">{apt.patient.name}</p>
                      <p className="text-sm text-muted-foreground">
                        {formatTime(apt.startDateTime)} - {apt.sessionType.name}
                      </p>
                    </div>
                    <Badge variant={statusColors[apt.status]}>
                      {statusLabels[apt.status]}
                    </Badge>
                  </Link>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Upcoming Appointments */}
        <Card>
          <CardHeader>
            <CardTitle>Proximos Agendamentos</CardTitle>
            <CardDescription>Proximos 5 agendamentos</CardDescription>
          </CardHeader>
          <CardContent>
            {dashboard?.upcomingAppointments.length === 0 ? (
              <p className="text-muted-foreground text-center py-4">
                Nenhum agendamento futuro
              </p>
            ) : (
              <div className="space-y-3">
                {dashboard?.upcomingAppointments.map((apt) => (
                  <Link
                    key={apt.id}
                    href={`/admin/agendamentos?id=${apt.id}`}
                    className="flex items-center justify-between p-3 rounded-lg bg-gray-50 hover:bg-gray-100 transition-colors"
                  >
                    <div>
                      <p className="font-medium">{apt.patient.name}</p>
                      <p className="text-sm text-muted-foreground">
                        {format(new Date(apt.startDateTime), "dd/MM HH:mm")} - {apt.sessionType.name}
                      </p>
                    </div>
                    <Badge variant={statusColors[apt.status]}>
                      {statusLabels[apt.status]}
                    </Badge>
                  </Link>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
