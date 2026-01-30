"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { format, addDays, startOfDay } from "date-fns";
import { ptBR } from "date-fns/locale";
import { Calendar } from "@/components/ui/calendar";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { publicApi, SessionType, TimeSlot, ConsentVersion } from "@/lib/api";
import { useToast } from "@/hooks/use-toast";
import { formatCurrency } from "@/lib/utils";
import { ArrowLeft, ArrowRight, Loader2 } from "lucide-react";
import Link from "next/link";

type Step = "type" | "date" | "time" | "info" | "confirm";

// Regex para validar email
const EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

// Função para formatar telefone
function formatPhone(value: string): string {
  const numbers = value.replace(/\D/g, "").slice(0, 11);
  if (numbers.length <= 2) return numbers;
  if (numbers.length <= 7) return `(${numbers.slice(0, 2)}) ${numbers.slice(2)}`;
  return `(${numbers.slice(0, 2)}) ${numbers.slice(2, 7)}-${numbers.slice(7)}`;
}

// Função para validar telefone (mínimo 10 dígitos)
function isValidPhone(phone: string): boolean {
  const numbers = phone.replace(/\D/g, "");
  return numbers.length >= 10 && numbers.length <= 11;
}

// Função para validar email
function isValidEmail(email: string): boolean {
  if (!email) return true; // Email é opcional, então vazio é válido
  return EMAIL_REGEX.test(email);
}

export default function AgendarPage() {
  const router = useRouter();
  const { toast } = useToast();

  const [step, setStep] = useState<Step>("type");
  const [loading, setLoading] = useState(false);
  const [sessionTypes, setSessionTypes] = useState<SessionType[]>([]);
  const [consent, setConsent] = useState<ConsentVersion | null>(null);
  const [slots, setSlots] = useState<TimeSlot[]>([]);
  const [errors, setErrors] = useState<{ phone?: string; email?: string }>({});

  // Form data
  const [selectedType, setSelectedType] = useState<SessionType | null>(null);
  const [selectedDate, setSelectedDate] = useState<Date | undefined>();
  const [selectedSlot, setSelectedSlot] = useState<TimeSlot | null>(null);
  const [formData, setFormData] = useState({
    name: "",
    phone: "",
    email: "",
    consentAccepted: false,
  });

  // Validação ao tentar continuar
  const validateInfo = (): boolean => {
    const newErrors: { phone?: string; email?: string } = {};

    if (!isValidPhone(formData.phone)) {
      newErrors.phone = "Telefone deve ter 10 ou 11 dígitos";
    }

    if (formData.email && !isValidEmail(formData.email)) {
      newErrors.email = "Email inválido";
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleContinueToConfirm = () => {
    if (validateInfo()) {
      setStep("confirm");
    }
  };

  // Load session types and consent
  useEffect(() => {
    async function loadData() {
      try {
        const [types, consentData] = await Promise.all([
          publicApi.getSessionTypes(),
          publicApi.getConsent(),
        ]);
        setSessionTypes(types);
        setConsent(consentData);
      } catch (error) {
        toast({
          title: "Erro",
          description: "Nao foi possivel carregar os dados",
          variant: "destructive",
        });
      }
    }
    loadData();
  }, [toast]);

  // Load slots when date changes
  useEffect(() => {
    async function loadSlots() {
      if (!selectedDate || !selectedType) return;

      setLoading(true);
      try {
        const dateStr = format(selectedDate, "yyyy-MM-dd");
        const response = await publicApi.getAvailableSlots(dateStr, selectedType.id);
        setSlots(response.slots.filter((s) => s.available));
      } catch (error) {
        toast({
          title: "Erro",
          description: "Nao foi possivel carregar os horarios",
          variant: "destructive",
        });
      } finally {
        setLoading(false);
      }
    }
    loadSlots();
  }, [selectedDate, selectedType, toast]);

  const handleSubmit = async () => {
    if (!selectedType || !selectedSlot || !formData.consentAccepted) return;

    setLoading(true);
    try {
      const result = await publicApi.createBooking({
        sessionTypeId: selectedType.id,
        startDateTime: selectedSlot.dateTime,
        patientName: formData.name,
        patientPhone: formData.phone,
        patientEmail: formData.email || undefined,
        consentAccepted: formData.consentAccepted,
        consentVersion: consent?.version,
      });

      router.push(`/confirmacao?token=${result.cancellationToken}`);
    } catch (error: any) {
      toast({
        title: "Erro ao agendar",
        description: error.message || "Tente novamente",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const minDate = startOfDay(addDays(new Date(), 1));
  const maxDate = addDays(new Date(), 90);

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="container mx-auto px-4 max-w-2xl">
        <Link href="/" className="inline-flex items-center text-gray-600 hover:text-primary mb-6">
          <ArrowLeft className="h-4 w-4 mr-2" />
          Voltar
        </Link>

        <Card>
          <CardHeader>
            <CardTitle>Agendar Compromisso</CardTitle>
            <CardDescription>
              {step === "type" && "Escolha o tipo de atendimento"}
              {step === "date" && "Selecione uma data"}
              {step === "time" && "Escolha um horario"}
              {step === "info" && "Preencha seus dados"}
              {step === "confirm" && "Confirme seu agendamento"}
            </CardDescription>
          </CardHeader>

          <CardContent className="space-y-6">
            {/* Step 1: Session Type */}
            {step === "type" && (
              <div className="space-y-4">
                {sessionTypes.map((type) => (
                  <div
                    key={type.id}
                    className={`p-4 border rounded-lg cursor-pointer transition-colors ${
                      selectedType?.id === type.id
                        ? "border-primary bg-primary/5"
                        : "hover:border-gray-400"
                    }`}
                    onClick={() => setSelectedType(type)}
                  >
                    <div className="flex justify-between items-start">
                      <div>
                        <h3 className="font-medium">{type.name}</h3>
                        <p className="text-sm text-gray-600">{type.durationMinutes} minutos</p>
                        {type.description && (
                          <p className="text-sm text-gray-500 mt-1">{type.description}</p>
                        )}
                      </div>
                      <span className="font-semibold text-primary">
                        {formatCurrency(type.price)}
                      </span>
                    </div>
                  </div>
                ))}

                <Button
                  onClick={() => setStep("date")}
                  disabled={!selectedType}
                  className="w-full"
                >
                  Continuar
                  <ArrowRight className="ml-2 h-4 w-4" />
                </Button>
              </div>
            )}

            {/* Step 2: Date */}
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

                <div className="flex gap-4">
                  <Button variant="outline" onClick={() => setStep("type")} className="flex-1">
                    <ArrowLeft className="mr-2 h-4 w-4" />
                    Voltar
                  </Button>
                  <Button
                    onClick={() => setStep("time")}
                    disabled={!selectedDate}
                    className="flex-1"
                  >
                    Continuar
                    <ArrowRight className="ml-2 h-4 w-4" />
                  </Button>
                </div>
              </div>
            )}

            {/* Step 3: Time */}
            {step === "time" && (
              <div className="space-y-4">
                {loading ? (
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
                    onClick={() => setStep("info")}
                    disabled={!selectedSlot}
                    className="flex-1"
                  >
                    Continuar
                    <ArrowRight className="ml-2 h-4 w-4" />
                  </Button>
                </div>
              </div>
            )}

            {/* Step 4: Info */}
            {step === "info" && (
              <div className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="name">Nome completo *</Label>
                  <Input
                    id="name"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    placeholder="Seu nome"
                    maxLength={100}
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="phone">Telefone *</Label>
                  <Input
                    id="phone"
                    value={formData.phone}
                    onChange={(e) => {
                      const formatted = formatPhone(e.target.value);
                      setFormData({ ...formData, phone: formatted });
                      if (errors.phone) setErrors({ ...errors, phone: undefined });
                    }}
                    placeholder="(11) 99999-9999"
                    maxLength={15}
                    className={errors.phone ? "border-red-500" : ""}
                  />
                  {errors.phone && (
                    <p className="text-sm text-red-500">{errors.phone}</p>
                  )}
                </div>

                <div className="space-y-2">
                  <Label htmlFor="email">Email (opcional)</Label>
                  <Input
                    id="email"
                    type="email"
                    value={formData.email}
                    onChange={(e) => {
                      setFormData({ ...formData, email: e.target.value });
                      if (errors.email) setErrors({ ...errors, email: undefined });
                    }}
                    placeholder="seu@email.com"
                    maxLength={100}
                    className={errors.email ? "border-red-500" : ""}
                  />
                  {errors.email && (
                    <p className="text-sm text-red-500">{errors.email}</p>
                  )}
                </div>

                <div className="flex gap-4">
                  <Button variant="outline" onClick={() => setStep("time")} className="flex-1">
                    <ArrowLeft className="mr-2 h-4 w-4" />
                    Voltar
                  </Button>
                  <Button
                    onClick={handleContinueToConfirm}
                    disabled={!formData.name || !formData.phone}
                    className="flex-1"
                  >
                    Continuar
                    <ArrowRight className="ml-2 h-4 w-4" />
                  </Button>
                </div>
              </div>
            )}

            {/* Step 5: Confirm */}
            {step === "confirm" && selectedType && selectedDate && selectedSlot && (
              <div className="space-y-6">
                <div className="bg-gray-50 p-4 rounded-lg space-y-2">
                  <h3 className="font-semibold">Resumo do agendamento</h3>
                  <p><span className="text-gray-600">Tipo:</span> {selectedType.name}</p>
                  <p>
                    <span className="text-gray-600">Data:</span>{" "}
                    {format(selectedDate, "EEEE, d 'de' MMMM", { locale: ptBR })}
                  </p>
                  <p><span className="text-gray-600">Horario:</span> {selectedSlot.time.slice(0, 5)}</p>
                  <p><span className="text-gray-600">Nome:</span> {formData.name}</p>
                  <p><span className="text-gray-600">Telefone:</span> {formData.phone}</p>
                  {formData.email && (
                    <p><span className="text-gray-600">Email:</span> {formData.email}</p>
                  )}
                  <p className="font-semibold text-primary">
                    Valor: {formatCurrency(selectedType.price)}
                  </p>
                </div>

                <div className="space-y-2">
                  <div className="flex items-start gap-2">
                    <input
                      type="checkbox"
                      id="consent"
                      checked={formData.consentAccepted}
                      onChange={(e) =>
                        setFormData({ ...formData, consentAccepted: e.target.checked })
                      }
                      className="mt-1"
                    />
                    <label htmlFor="consent" className="text-sm text-gray-600">
                      Li e concordo com os{" "}
                      <a
                        href="/politicas"
                        className="text-primary hover:underline"
                        target="_blank"
                        rel="noopener noreferrer"
                      >
                        termos de uso e politica de privacidade
                      </a>
                    </label>
                  </div>
                </div>

                <div className="flex gap-4">
                  <Button variant="outline" onClick={() => setStep("info")} className="flex-1">
                    <ArrowLeft className="mr-2 h-4 w-4" />
                    Voltar
                  </Button>
                  <Button
                    onClick={handleSubmit}
                    disabled={!formData.consentAccepted || loading}
                    className="flex-1"
                  >
                    {loading ? (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                      "Confirmar Agendamento"
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
