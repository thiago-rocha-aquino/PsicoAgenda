"use client";

import { useEffect, useState } from "react";
import { format } from "date-fns";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { useAuth } from "@/hooks/use-auth";
import { adminApi, Payment } from "@/lib/api";
import { useToast } from "@/hooks/use-toast";
import { formatCurrency } from "@/lib/utils";
import { Check, X, Loader2 } from "lucide-react";

export default function PagamentosPage() {
  const { token } = useAuth();
  const { toast } = useToast();

  const [payments, setPayments] = useState<Payment[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadPayments();
  }, [token]);

  async function loadPayments() {
    if (!token) return;

    try {
      const data = await adminApi.getPendingPayments(token);
      setPayments(data);
    } catch (error) {
      console.error("Erro ao carregar pagamentos:", error);
    } finally {
      setLoading(false);
    }
  }

  const handleMarkPaid = async (appointmentId: string) => {
    if (!token) return;

    try {
      await adminApi.markAsPaid(token, appointmentId);
      toast({ title: "Pagamento registrado" });
      loadPayments();
    } catch (error: any) {
      toast({
        title: "Erro",
        description: error.message,
        variant: "destructive",
      });
    }
  };

  const handleWaive = async (appointmentId: string) => {
    if (!token) return;

    try {
      await adminApi.waivePayment(token, appointmentId, "Isencao");
      toast({ title: "Pagamento isento" });
      loadPayments();
    } catch (error: any) {
      toast({
        title: "Erro",
        description: error.message,
        variant: "destructive",
      });
    }
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Pagamentos Pendentes</h1>

      <Card>
        <CardHeader>
          <CardTitle>Pendentes</CardTitle>
          <CardDescription>
            {payments.length} pagamento(s) aguardando
          </CardDescription>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="flex justify-center py-8">
              <Loader2 className="h-8 w-8 animate-spin text-primary" />
            </div>
          ) : payments.length === 0 ? (
            <p className="text-muted-foreground text-center py-8">
              Nenhum pagamento pendente
            </p>
          ) : (
            <div className="space-y-4">
              {payments.map((payment) => (
                <div
                  key={payment.id}
                  className="flex items-center justify-between p-4 border rounded-lg"
                >
                  <div>
                    <p className="font-medium">
                      {payment.appointment?.patient.name}
                    </p>
                    <p className="text-sm text-muted-foreground">
                      {payment.appointment &&
                        format(new Date(payment.appointment.startDateTime), "dd/MM/yyyy HH:mm")}{" "}
                      - {payment.appointment?.sessionType.name}
                    </p>
                    <p className="text-lg font-semibold text-primary mt-1">
                      {formatCurrency(payment.amount)}
                    </p>
                  </div>
                  <div className="flex gap-2">
                    <Button
                      size="sm"
                      onClick={() => handleMarkPaid(payment.appointmentId)}
                    >
                      <Check className="h-4 w-4 mr-1" />
                      Pago
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => handleWaive(payment.appointmentId)}
                    >
                      <X className="h-4 w-4 mr-1" />
                      Isentar
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
