"use client";

import { Suspense, useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import { format } from "date-fns";
import { ptBR } from "date-fns/locale";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { publicApi, Appointment } from "@/lib/api";
import { formatCurrency } from "@/lib/utils";
import { CheckCircle, Calendar, Clock, Loader2 } from "lucide-react";

function ConfirmacaoContent() {
  const searchParams = useSearchParams();
  const token = searchParams.get("token");

  const [appointment, setAppointment] = useState<Appointment | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function loadAppointment() {
      if (!token) {
        setError("Token nao encontrado");
        setLoading(false);
        return;
      }

      try {
        const data = await publicApi.getAppointmentByToken(token);
        setAppointment(data);
      } catch (err: any) {
        setError(err.message || "Agendamento nao encontrado");
      } finally {
        setLoading(false);
      }
    }

    loadAppointment();
  }, [token]);

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  if (error || !appointment) {
    return (
      <div className="min-h-screen bg-gray-50 py-8">
        <div className="container mx-auto px-4 max-w-md">
          <Card>
            <CardHeader>
              <CardTitle className="text-red-600">Erro</CardTitle>
              <CardDescription>{error || "Agendamento nao encontrado"}</CardDescription>
            </CardHeader>
            <CardContent>
              <Link href="/">
                <Button className="w-full">Voltar ao inicio</Button>
              </Link>
            </CardContent>
          </Card>
        </div>
      </div>
    );
  }

  const startDate = new Date(appointment.startDateTime);
  const isCancelled = appointment.status === "CANCELLED" || appointment.status === "CANCELLED_LATE";

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="container mx-auto px-4 max-w-md">
        <Card>
          <CardHeader className="text-center">
            {isCancelled ? (
              <>
                <div className="mx-auto w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mb-4">
                  <span className="text-red-600 text-2xl">X</span>
                </div>
                <CardTitle>Agendamento Cancelado</CardTitle>
              </>
            ) : (
              <>
                <div className="mx-auto w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mb-4">
                  <CheckCircle className="h-10 w-10 text-green-600" />
                </div>
                <CardTitle>Agendamento Confirmado</CardTitle>
                <CardDescription>
                  Guarde esta pagina para cancelar ou reagendar
                </CardDescription>
              </>
            )}
          </CardHeader>

          <CardContent className="space-y-6">
            <div className="bg-gray-50 p-4 rounded-lg space-y-3">
              <div className="flex items-center gap-3">
                <Calendar className="h-5 w-5 text-primary" />
                <div>
                  <p className="font-medium">
                    {format(startDate, "EEEE, d 'de' MMMM 'de' yyyy", { locale: ptBR })}
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-3">
                <Clock className="h-5 w-5 text-primary" />
                <div>
                  <p className="font-medium">{format(startDate, "HH:mm")}</p>
                  <p className="text-sm text-gray-600">
                    {appointment.sessionType.durationMinutes} minutos
                  </p>
                </div>
              </div>

              <div className="pt-2 border-t">
                <p className="text-sm text-gray-600">{appointment.sessionType.name}</p>
                <p className="font-semibold text-primary">
                  {formatCurrency(appointment.sessionType.price)}
                </p>
              </div>

              <div className="pt-2 border-t flex items-center justify-between">
                <span className="text-sm text-gray-600">Status</span>
                <Badge
                  variant={
                    appointment.status === "CONFIRMED" || appointment.status === "SCHEDULED"
                      ? "success"
                      : appointment.status === "ATTENDED"
                      ? "default"
                      : "destructive"
                  }
                >
                  {appointment.status === "CONFIRMED" && "Confirmado"}
                  {appointment.status === "SCHEDULED" && "Agendado"}
                  {appointment.status === "CANCELLED" && "Cancelado"}
                  {appointment.status === "CANCELLED_LATE" && "Cancelado Tardio"}
                  {appointment.status === "ATTENDED" && "Realizado"}
                  {appointment.status === "NO_SHOW" && "Falta"}
                </Badge>
              </div>
            </div>

            {!isCancelled && (
              <div className="space-y-3">
                <Link href={`/reagendar?token=${token}`}>
                  <Button variant="outline" className="w-full">
                    Reagendar
                  </Button>
                </Link>
                <Link href={`/cancelar?token=${token}`}>
                  <Button variant="destructive" className="w-full">
                    Cancelar
                  </Button>
                </Link>
              </div>
            )}

            <div className="text-center">
              <Link href="/" className="text-sm text-gray-600 hover:text-primary">
                Voltar ao inicio
              </Link>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

export default function ConfirmacaoPage() {
  return (
    <Suspense fallback={
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    }>
      <ConfirmacaoContent />
    </Suspense>
  );
}
