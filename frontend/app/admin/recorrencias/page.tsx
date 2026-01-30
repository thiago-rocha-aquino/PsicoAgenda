"use client";

import { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useAuth } from "@/hooks/use-auth";
import { useToast } from "@/hooks/use-toast";
import { adminApi, RecurringSeries, SessionType, Patient } from "@/lib/api";
import {
  Plus,
  Trash2,
  Loader2,
  RefreshCw,
  Calendar,
  AlertCircle,
} from "lucide-react";
import { format, addDays } from "date-fns";
import { ptBR } from "date-fns/locale";

const DAYS_OF_WEEK = [
  { value: "MONDAY", label: "Segunda-feira" },
  { value: "TUESDAY", label: "Terça-feira" },
  { value: "WEDNESDAY", label: "Quarta-feira" },
  { value: "THURSDAY", label: "Quinta-feira" },
  { value: "FRIDAY", label: "Sexta-feira" },
  { value: "SATURDAY", label: "Sábado" },
  { value: "SUNDAY", label: "Domingo" },
];

const FREQUENCIES = [
  { value: "WEEKLY", label: "Semanal" },
  { value: "BIWEEKLY", label: "Quinzenal" },
];

export default function RecorrenciasPage() {
  const { token } = useAuth();
  const { toast } = useToast();

  const [series, setSeries] = useState<RecurringSeries[]>([]);
  const [sessionTypes, setSessionTypes] = useState<SessionType[]>([]);
  const [patients, setPatients] = useState<Patient[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);
  const [conflicts, setConflicts] = useState<string[]>([]);

  const [form, setForm] = useState({
    patientId: "",
    sessionTypeId: "",
    dayOfWeek: "MONDAY",
    time: "09:00",
    frequency: "WEEKLY",
    startDate: format(addDays(new Date(), 1), "yyyy-MM-dd"),
    endDate: format(addDays(new Date(), 90), "yyyy-MM-dd"),
  });

  useEffect(() => {
    loadData();
  }, [token]);

  async function loadData() {
    if (!token) return;

    try {
      const [seriesData, typesData, patientsData] = await Promise.all([
        adminApi.getRecurringSeries(token),
        adminApi.getSessionTypes(token),
        adminApi.getPatients(token),
      ]);
      setSeries(seriesData);
      setSessionTypes(typesData.filter((t) => t.active));
      setPatients(patientsData);
    } catch (error) {
      console.error("Erro ao carregar dados:", error);
    } finally {
      setLoading(false);
    }
  }

  function resetForm() {
    setForm({
      patientId: "",
      sessionTypeId: "",
      dayOfWeek: "MONDAY",
      time: "09:00",
      frequency: "WEEKLY",
      startDate: format(addDays(new Date(), 1), "yyyy-MM-dd"),
      endDate: format(addDays(new Date(), 90), "yyyy-MM-dd"),
    });
    setConflicts([]);
    setShowForm(false);
  }

  async function checkConflicts() {
    if (!token || !form.sessionTypeId) return;

    try {
      const result = await adminApi.checkRecurrenceConflicts(token, {
        sessionTypeId: form.sessionTypeId,
        dayOfWeek: form.dayOfWeek,
        time: form.time,
        frequency: form.frequency as "WEEKLY" | "BIWEEKLY",
        startDate: form.startDate,
        endDate: form.endDate,
      });

      if (!result.available && result.conflictingDates) {
        setConflicts(result.conflictingDates);
      } else {
        setConflicts([]);
      }

      return result.available;
    } catch (error) {
      console.error("Erro ao verificar conflitos:", error);
      return false;
    }
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!token) return;

    const isAvailable = await checkConflicts();
    if (!isAvailable && conflicts.length > 0) {
      toast({
        title: "Conflitos encontrados",
        description: `Existem ${conflicts.length} datas com conflito. Revise antes de continuar.`,
        variant: "destructive",
      });
      return;
    }

    setSaving(true);
    try {
      await adminApi.createRecurringSeries(token, {
        patientId: form.patientId,
        sessionTypeId: form.sessionTypeId,
        dayOfWeek: form.dayOfWeek,
        time: form.time,
        frequency: form.frequency as "WEEKLY" | "BIWEEKLY",
        startDate: form.startDate,
        endDate: form.endDate,
      });

      toast({
        title: "Série criada",
        description: "Os agendamentos recorrentes foram criados.",
      });
      resetForm();
      loadData();
    } catch (error: any) {
      toast({
        title: "Erro",
        description: error.message || "Não foi possível criar a série.",
        variant: "destructive",
      });
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(id: string) {
    if (!token) return;
    if (
      !confirm(
        "Deseja realmente excluir esta série? Os agendamentos futuros não confirmados serão cancelados."
      )
    )
      return;

    try {
      await adminApi.deleteRecurringSeries(token, id);
      toast({
        title: "Série excluída",
        description: "A série e agendamentos futuros foram removidos.",
      });
      loadData();
    } catch (error) {
      toast({
        title: "Erro",
        description: "Não foi possível excluir a série.",
        variant: "destructive",
      });
    }
  }

  function getDayLabel(day: string) {
    return DAYS_OF_WEEK.find((d) => d.value === day)?.label || day;
  }

  function getFrequencyLabel(freq: string) {
    return FREQUENCIES.find((f) => f.value === freq)?.label || freq;
  }

  function getPatientName(id: string) {
    return patients.find((p) => p.id === id)?.name || "Paciente não encontrado";
  }

  function getSessionTypeName(id: string) {
    return sessionTypes.find((t) => t.id === id)?.name || "Tipo não encontrado";
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Séries Recorrentes</h1>
        {!showForm && (
          <Button onClick={() => setShowForm(true)}>
            <Plus className="h-4 w-4 mr-2" />
            Nova Série
          </Button>
        )}
      </div>

      {showForm && (
        <Card>
          <CardHeader>
            <CardTitle>Nova Série Recorrente</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label>Paciente</Label>
                  <Select
                    value={form.patientId}
                    onValueChange={(v) => setForm({ ...form, patientId: v })}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Selecione o paciente" />
                    </SelectTrigger>
                    <SelectContent>
                      {patients.map((p) => (
                        <SelectItem key={p.id} value={p.id}>
                          {p.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-2">
                  <Label>Tipo de Sessão</Label>
                  <Select
                    value={form.sessionTypeId}
                    onValueChange={(v) => setForm({ ...form, sessionTypeId: v })}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Selecione o tipo" />
                    </SelectTrigger>
                    <SelectContent>
                      {sessionTypes.map((t) => (
                        <SelectItem key={t.id} value={t.id}>
                          {t.name} ({t.durationMinutes}min)
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-2">
                  <Label>Dia da Semana</Label>
                  <Select
                    value={form.dayOfWeek}
                    onValueChange={(v) => setForm({ ...form, dayOfWeek: v })}
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {DAYS_OF_WEEK.map((day) => (
                        <SelectItem key={day.value} value={day.value}>
                          {day.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="time">Horário</Label>
                  <Input
                    id="time"
                    type="time"
                    value={form.time}
                    onChange={(e) => setForm({ ...form, time: e.target.value })}
                    required
                  />
                </div>

                <div className="space-y-2">
                  <Label>Frequência</Label>
                  <Select
                    value={form.frequency}
                    onValueChange={(v) => setForm({ ...form, frequency: v })}
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {FREQUENCIES.map((freq) => (
                        <SelectItem key={freq.value} value={freq.value}>
                          {freq.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="startDate">Data Início</Label>
                  <Input
                    id="startDate"
                    type="date"
                    value={form.startDate}
                    onChange={(e) =>
                      setForm({ ...form, startDate: e.target.value })
                    }
                    required
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="endDate">Data Fim</Label>
                  <Input
                    id="endDate"
                    type="date"
                    value={form.endDate}
                    onChange={(e) =>
                      setForm({ ...form, endDate: e.target.value })
                    }
                    required
                  />
                </div>
              </div>

              {conflicts.length > 0 && (
                <div className="p-4 border border-yellow-300 bg-yellow-50 rounded-lg">
                  <div className="flex items-center gap-2 text-yellow-800 mb-2">
                    <AlertCircle className="h-5 w-5" />
                    <span className="font-medium">
                      Conflitos encontrados ({conflicts.length} datas)
                    </span>
                  </div>
                  <div className="text-sm text-yellow-700 max-h-32 overflow-y-auto">
                    {conflicts.map((date, i) => (
                      <span key={i}>
                        {format(new Date(date + "T12:00:00"), "dd/MM/yyyy")}
                        {i < conflicts.length - 1 && ", "}
                      </span>
                    ))}
                  </div>
                </div>
              )}

              <div className="flex gap-2">
                <Button
                  type="button"
                  variant="outline"
                  onClick={checkConflicts}
                  disabled={!form.sessionTypeId}
                >
                  Verificar Conflitos
                </Button>
                <Button type="submit" disabled={saving || !form.patientId}>
                  {saving && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
                  Criar Série
                </Button>
                <Button type="button" variant="outline" onClick={resetForm}>
                  Cancelar
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader>
          <CardTitle>Séries Ativas</CardTitle>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="flex justify-center py-8">
              <Loader2 className="h-8 w-8 animate-spin text-primary" />
            </div>
          ) : series.length === 0 ? (
            <p className="text-muted-foreground text-center py-8">
              Nenhuma série recorrente cadastrada
            </p>
          ) : (
            <div className="space-y-3">
              {series.map((s) => (
                <div
                  key={s.id}
                  className="flex items-center justify-between p-4 border rounded-lg"
                >
                  <div className="flex items-center gap-4">
                    <RefreshCw className="h-5 w-5 text-primary" />
                    <div>
                      <p className="font-medium">{getPatientName(s.patientId)}</p>
                      <p className="text-sm text-muted-foreground">
                        {getSessionTypeName(s.sessionTypeId)} •{" "}
                        {getDayLabel(s.dayOfWeek)} às {s.startTime || s.time}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        {getFrequencyLabel(s.frequency)} •{" "}
                        {format(new Date(s.startDate + "T12:00:00"), "dd/MM/yyyy")} -{" "}
                        {s.endDate ? format(new Date(s.endDate + "T12:00:00"), "dd/MM/yyyy") : "Sem data fim"}
                      </p>
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    {s.active ? (
                      <span className="text-xs bg-green-100 text-green-700 px-2 py-1 rounded">
                        Ativa
                      </span>
                    ) : (
                      <span className="text-xs bg-gray-100 text-gray-700 px-2 py-1 rounded">
                        Inativa
                      </span>
                    )}
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => handleDelete(s.id)}
                    >
                      <Trash2 className="h-4 w-4 text-red-500" />
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
