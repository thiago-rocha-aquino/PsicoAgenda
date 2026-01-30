"use client";

import { Suspense, useEffect, useState } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import Link from "next/link";
import { format, addDays, startOfDay } from "date-fns";
import { ptBR } from "date-fns/locale";
import { Calendar } from "@/components/ui/calendar";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { publicApi, Appointment, TimeSlot } from "@/lib/api";
import { useToast } from "@/hooks/use-toast";
import { ArrowLeft, ArrowRight, Loader2 } from "lucide-react";

function ReagendarContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const { toast } = useToast();
  const token = searchParams.get("token");

  const [appointment, setAppointment] = useState<Appointment | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [step, setStep] = useState<"date" | "time">("date");
  const [selectedDate, setSelectedDate] = useState<Date | undefined>();
  const [selectedSlot, setSelectedSlot] = useState<TimeSlot | null>(null);
  const [slots, setSlots] = useState<TimeSlot[]>([]);
  const [loadingSlots, setLoadingSlots] = useState(false);

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

  useEffect(() => {
    async function loadSlots() {
      if (!selectedDate || !appointment) return;

      setLoadingSlots(true);
      try {
        const dateStr = format(selectedDate, "yyyy-MM-dd");
        const response = await publicApi.getAvailableSlots(dateStr, appointment.sessionType.id);
        setSlots(response.slots.filter((s) => s.available));
      } catch (err) {
        toast({
          title: "Erro",
          description: "Nao foi possivel carregar os horarios",
          variant: "destructive",
        });
      } finally {
        setLoadingSlots(false);
      }
    }

    loadSlots();
  }, [selectedDate, appointment, toast]);

  const handleReschedule = async () => {
    if (!token || !selectedSlot) return;

    setSubmitting(true);
    try {
      const result = await publicApi.rescheduleAppointment(token, selectedSlot.dateTime);
      toast({
        title: "Reagendado",
        description: "Seu agendamento foi alterado com sucesso",
      });
      router.push(`/confirmacao?token=${result.cancellationToken}`);
    } catch (err: any) {
      toast({
        title: "Erro",
        description: err.message || "Nao foi possivel reagendar",
        variant: "destructive",
      });
    } finally {
      setSubmitting(false);
    }
  };

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

  const isCancellable = appointment.status === "CONFIRMED" || appointment.status === "SCHEDULED";

  if (!isCancellable) {
    return (
      <div className="min-h-screen bg-gray-50 py-8">
        <div className="container mx-auto px-4 max-w-md">
          <Card>
            <CardHeader>
              <CardTitle>Reagendamento nao disponivel</CardTitle>
              <CardDescription>
                Este agendamento ja foi cancelado ou realizado
              </CardDescription>
            </CardHeader>
            <CardContent>
              <Link href={`/confirmacao?token=${token}`}>
                <Button className="w-full">Ver detalhes</Button>
              </Link>
            </CardContent>
          </Card>
        </div>
      </div>
    );
  }

  const minDate = startOfDay(addDays(new Date(), 1));
  const maxDate = addDays(new Date(), 90);

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="container mx-auto px-4 max-w-md">
        <Link href={`/confirmacao?token=${token}`} className="inline-flex items-center text-gray-600 hover:text-primary mb-6">
          <ArrowLeft className="h-4 w-4 mr-2" />
          Voltar
        </Link>

        <Card>
          <CardHeader>
            <CardTitle>Reagendar</CardTitle>
            <CardDescription>
              {step === "date" && "Selecione uma nova data"}
              {step === "time" && "Escolha um novo horario"}
            </CardDescription>
          </CardHeader>

          <CardContent className="space-y-6">
            <div className="bg-gray-50 p-4 rounded-lg text-sm">
              <p className="text-gray-600">Agendamento atual:</p>
              <p className="font-medium">
                {format(new Date(appointment.startDateTime), "EEEE, d 'de' MMMM 'as' HH:mm", { locale: ptBR })}
              </p>
            </div>

            {step === "date" && (
              <div className="space-y-4">
                <Calendar
                  mode="single"
                  selected={selectedDate}
                  onSelect={setSelectedDate}
                  disabled={(date) => date < minDate || date > maxDate}
                  locale={ptBR}
                  className="rounded-md border mx-auto"
                />

                <Button
                  onClick={() => setStep("time")}
                  disabled={!selectedDate}
                  className="w-full"
                >
                  Continuar
                  <ArrowRight className="ml-2 h-4 w-4" />
                </Button>
              </div>
            )}

            {step === "time" && (
              <div className="space-y-4">
                <p className="text-sm text-gray-600">
                  Nova data: {selectedDate && format(selectedDate, "d 'de' MMMM", { locale: ptBR })}
                </p>

                {loadingSlots ? (
                  <div className="flex justify-center py-8">
                    <Loader2 className="h-8 w-8 animate-spin text-primary" />
                  </div>
                ) : slots.length === 0 ? (
                  <p className="text-center text-gray-500 py-8">
                    Nenhum horario disponivel nesta data
                  </p>
                ) : (
                  <div className="grid grid-cols-3 gap-2">
                    {slots.map((slot) => (
                      <Button
                        key={slot.time}
                        variant={selectedSlot?.time === slot.time ? "default" : "outline"}
                        onClick={() => setSelectedSlot(slot)}
                        className="text-sm"
                      >
                        {slot.time.slice(0, 5)}
                      </Button>
                    ))}
                  </div>
                )}

                <div className="flex gap-4">
                  <Button variant="outline" onClick={() => setStep("date")} className="flex-1">
                    <ArrowLeft className="mr-2 h-4 w-4" />
                    Voltar
                  </Button>
                  <Button
                    onClick={handleReschedule}
                    disabled={!selectedSlot || submitting}
                    className="flex-1"
                  >
                    {submitting ? (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                      "Confirmar"
                    )}
                  </Button>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

export default function ReagendarPage() {
  return (
    <Suspense fallback={
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    }>
      <ReagendarContent />
    </Suspense>
  );
}
