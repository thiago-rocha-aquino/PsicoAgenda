"use client";

import { Suspense, useEffect, useState } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import Link from "next/link";
import { format } from "date-fns";
import { ptBR } from "date-fns/locale";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { publicApi, Appointment } from "@/lib/api";
import { useToast } from "@/hooks/use-toast";
import { AlertTriangle, ArrowLeft, Loader2 } from "lucide-react";

function CancelarContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const { toast } = useToast();
  const token = searchParams.get("token");

  const [appointment, setAppointment] = useState<Appointment | null>(null);
  const [loading, setLoading] = useState(true);
  const [cancelling, setCancelling] = useState(false);
  const [reason, setReason] = useState("");
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

  const handleCancel = async () => {
    if (!token) return;

    setCancelling(true);
    try {
      await publicApi.cancelAppointment(token, reason);
      toast({
        title: "Cancelado",
        description: "Seu agendamento foi cancelado com sucesso",
      });
      router.push(`/confirmacao?token=${token}`);
    } catch (err: any) {
      toast({
        title: "Erro",
        description: err.message || "Nao foi possivel cancelar",
        variant: "destructive",
      });
    } finally {
      setCancelling(false);
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

  const startDate = new Date(appointment.startDateTime);
  const isCancellable = appointment.status === "CONFIRMED" || appointment.status === "SCHEDULED";

  if (!isCancellable) {
    return (
      <div className="min-h-screen bg-gray-50 py-8">
        <div className="container mx-auto px-4 max-w-md">
          <Card>
            <CardHeader>
              <CardTitle>Cancelamento nao disponivel</CardTitle>
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

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="container mx-auto px-4 max-w-md">
        <Link href={`/confirmacao?token=${token}`} className="inline-flex items-center text-gray-600 hover:text-primary mb-6">
          <ArrowLeft className="h-4 w-4 mr-2" />
          Voltar
        </Link>

        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <AlertTriangle className="h-6 w-6 text-yellow-500" />
              <CardTitle>Cancelar Agendamento</CardTitle>
            </div>
            <CardDescription>
              Tem certeza que deseja cancelar este agendamento?
            </CardDescription>
          </CardHeader>

          <CardContent className="space-y-6">
            <div className="bg-gray-50 p-4 rounded-lg space-y-2">
              <p className="font-medium">
                {format(startDate, "EEEE, d 'de' MMMM", { locale: ptBR })}
              </p>
              <p className="text-gray-600">
                {format(startDate, "HH:mm")} - {appointment.sessionType.name}
              </p>
            </div>

            <div className="bg-yellow-50 border border-yellow-200 p-4 rounded-lg">
              <p className="text-sm text-yellow-800">
                <strong>Atencao:</strong> Cancelamentos com menos de 24 horas de antecedencia
                podem estar sujeitos a cobranca conforme nossa politica.
              </p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="reason">Motivo do cancelamento (opcional)</Label>
              <Input
                id="reason"
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                placeholder="Informe o motivo..."
              />
            </div>

            <div className="flex gap-4">
              <Link href={`/confirmacao?token=${token}`} className="flex-1">
                <Button variant="outline" className="w-full">
                  Manter agendamento
                </Button>
              </Link>
              <Button
                variant="destructive"
                onClick={handleCancel}
                disabled={cancelling}
                className="flex-1"
              >
                {cancelling ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  "Confirmar cancelamento"
                )}
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

export default function CancelarPage() {
  return (
    <Suspense fallback={
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    }>
      <CancelarContent />
    </Suspense>
  );
}
