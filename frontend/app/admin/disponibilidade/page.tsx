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
import { adminApi, Availability, Block, DayOfWeek } from "@/lib/api";
import { Plus, Pencil, Trash2, Loader2, Calendar, Clock, Ban } from "lucide-react";
import { format } from "date-fns";

const DAYS_OF_WEEK = [
  { value: "MONDAY", label: "Segunda-feira" },
  { value: "TUESDAY", label: "Terça-feira" },
  { value: "WEDNESDAY", label: "Quarta-feira" },
  { value: "THURSDAY", label: "Quinta-feira" },
  { value: "FRIDAY", label: "Sexta-feira" },
  { value: "SATURDAY", label: "Sábado" },
  { value: "SUNDAY", label: "Domingo" },
];

const BLOCK_TYPES = [
  { value: "VACATION", label: "Férias" },
  { value: "HOLIDAY", label: "Feriado" },
  { value: "BREAK", label: "Intervalo" },
];

export default function DisponibilidadePage() {
  const { token } = useAuth();
  const { toast } = useToast();

  const [availabilities, setAvailabilities] = useState<Availability[]>([]);
  const [blocks, setBlocks] = useState<Block[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<"availability" | "blocks">("availability");

  const [showAvailForm, setShowAvailForm] = useState(false);
  const [editingAvailId, setEditingAvailId] = useState<string | null>(null);
  const [availForm, setAvailForm] = useState<{
    dayOfWeek: DayOfWeek;
    startTime: string;
    endTime: string;
  }>({
    dayOfWeek: "MONDAY",
    startTime: "08:00",
    endTime: "18:00",
  });

  const [showBlockForm, setShowBlockForm] = useState(false);
  const [blockForm, setBlockForm] = useState({
    blockType: "VACATION" as "VACATION" | "HOLIDAY" | "BREAK",
    startDateTime: "",
    endDateTime: "",
    reason: "",
  });

  const [saving, setSaving] = useState(false);

  useEffect(() => {
    loadData();
  }, [token]);

  async function loadData() {
    if (!token) return;

    try {
      const [availData, blocksData] = await Promise.all([
        adminApi.getSchedule(token),
        adminApi.getBlocks(token),
      ]);
      setAvailabilities(availData);
      setBlocks(blocksData);
    } catch (error) {
      console.error("Erro ao carregar dados:", error);
    } finally {
      setLoading(false);
    }
  }

  function resetAvailForm() {
    setAvailForm({ dayOfWeek: "MONDAY", startTime: "08:00", endTime: "18:00" });
    setEditingAvailId(null);
    setShowAvailForm(false);
  }

  function startEditAvail(avail: Availability) {
    setAvailForm({
      dayOfWeek: avail.dayOfWeek,
      startTime: avail.startTime,
      endTime: avail.endTime,
    });
    setEditingAvailId(avail.id);
    setShowAvailForm(true);
  }

  async function handleAvailSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!token) return;

    setSaving(true);
    try {
      if (editingAvailId) {
        await adminApi.updateAvailability(token, editingAvailId, availForm);
        toast({
          title: "Disponibilidade atualizada",
          description: "As alterações foram salvas.",
        });
      } else {
        await adminApi.createAvailability(token, availForm);
        toast({
          title: "Disponibilidade criada",
          description: "O horário foi adicionado.",
        });
      }
      resetAvailForm();
      loadData();
    } catch (error) {
      toast({
        title: "Erro",
        description: "Não foi possível salvar.",
        variant: "destructive",
      });
    } finally {
      setSaving(false);
    }
  }

  async function handleDeleteAvail(id: string) {
    if (!token) return;
    if (!confirm("Deseja realmente excluir esta disponibilidade?")) return;

    try {
      await adminApi.deleteAvailability(token, id);
      toast({ title: "Disponibilidade excluída" });
      loadData();
    } catch (error) {
      toast({
        title: "Erro",
        description: "Não foi possível excluir.",
        variant: "destructive",
      });
    }
  }

  function resetBlockForm() {
    setBlockForm({
      blockType: "VACATION",
      startDateTime: "",
      endDateTime: "",
      reason: "",
    });
    setShowBlockForm(false);
  }

  async function handleBlockSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!token) return;

    setSaving(true);
    try {
      await adminApi.createBlock(token, {
        blockType: blockForm.blockType,
        startDateTime: blockForm.startDateTime,
        endDateTime: blockForm.endDateTime,
        reason: blockForm.reason || undefined,
      });
      toast({
        title: "Bloqueio criado",
        description: "O bloqueio foi adicionado.",
      });
      resetBlockForm();
      loadData();
    } catch (error) {
      toast({
        title: "Erro",
        description: "Não foi possível salvar.",
        variant: "destructive",
      });
    } finally {
      setSaving(false);
    }
  }

  async function handleDeleteBlock(id: string) {
    if (!token) return;
    if (!confirm("Deseja realmente excluir este bloqueio?")) return;

    try {
      await adminApi.deleteBlock(token, id);
      toast({ title: "Bloqueio excluído" });
      loadData();
    } catch (error) {
      toast({
        title: "Erro",
        description: "Não foi possível excluir.",
        variant: "destructive",
      });
    }
  }

  function getDayLabel(day: string) {
    return DAYS_OF_WEEK.find((d) => d.value === day)?.label || day;
  }

  function getBlockTypeLabel(type: string) {
    return BLOCK_TYPES.find((t) => t.value === type)?.label || type;
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Disponibilidade e Bloqueios</h1>

      <div className="flex gap-2">
        <Button
          variant={activeTab === "availability" ? "default" : "outline"}
          onClick={() => setActiveTab("availability")}
        >
          <Clock className="h-4 w-4 mr-2" />
          Horários
        </Button>
        <Button
          variant={activeTab === "blocks" ? "default" : "outline"}
          onClick={() => setActiveTab("blocks")}
        >
          <Ban className="h-4 w-4 mr-2" />
          Bloqueios
        </Button>
      </div>

      {activeTab === "availability" && (
        <>
          <div className="flex justify-end">
            {!showAvailForm && (
              <Button onClick={() => setShowAvailForm(true)}>
                <Plus className="h-4 w-4 mr-2" />
                Novo Horário
              </Button>
            )}
          </div>

          {showAvailForm && (
            <Card>
              <CardHeader>
                <CardTitle>
                  {editingAvailId ? "Editar" : "Novo"} Horário de Atendimento
                </CardTitle>
              </CardHeader>
              <CardContent>
                <form onSubmit={handleAvailSubmit} className="space-y-4">
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <div className="space-y-2">
                      <Label>Dia da Semana</Label>
                      <Select
                        value={availForm.dayOfWeek}
                        onValueChange={(v) =>
                          setAvailForm({ ...availForm, dayOfWeek: v as DayOfWeek })
                        }
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
                      <Label htmlFor="startTime">Início</Label>
                      <Input
                        id="startTime"
                        type="time"
                        value={availForm.startTime}
                        onChange={(e) =>
                          setAvailForm({ ...availForm, startTime: e.target.value })
                        }
                        required
                      />
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="endTime">Fim</Label>
                      <Input
                        id="endTime"
                        type="time"
                        value={availForm.endTime}
                        onChange={(e) =>
                          setAvailForm({ ...availForm, endTime: e.target.value })
                        }
                        required
                      />
                    </div>
                  </div>

                  <div className="flex gap-2">
                    <Button type="submit" disabled={saving}>
                      {saving && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
                      {editingAvailId ? "Salvar" : "Criar"}
                    </Button>
                    <Button type="button" variant="outline" onClick={resetAvailForm}>
                      Cancelar
                    </Button>
                  </div>
                </form>
              </CardContent>
            </Card>
          )}

          <Card>
            <CardHeader>
              <CardTitle>Horários de Atendimento</CardTitle>
            </CardHeader>
            <CardContent>
              {loading ? (
                <div className="flex justify-center py-8">
                  <Loader2 className="h-8 w-8 animate-spin text-primary" />
                </div>
              ) : availabilities.length === 0 ? (
                <p className="text-muted-foreground text-center py-8">
                  Nenhum horário cadastrado
                </p>
              ) : (
                <div className="space-y-2">
                  {availabilities
                    .sort((a, b) => {
                      const dayOrder = DAYS_OF_WEEK.map((d) => d.value);
                      return (
                        dayOrder.indexOf(a.dayOfWeek) - dayOrder.indexOf(b.dayOfWeek)
                      );
                    })
                    .map((avail) => (
                      <div
                        key={avail.id}
                        className="flex items-center justify-between p-4 border rounded-lg"
                      >
                        <div className="flex items-center gap-4">
                          <Calendar className="h-5 w-5 text-muted-foreground" />
                          <div>
                            <p className="font-medium">
                              {getDayLabel(avail.dayOfWeek)}
                            </p>
                            <p className="text-sm text-muted-foreground">
                              {avail.startTime} - {avail.endTime}
                            </p>
                          </div>
                        </div>

                        <div className="flex items-center gap-2">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => startEditAvail(avail)}
                          >
                            <Pencil className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleDeleteAvail(avail.id)}
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
        </>
      )}

      {activeTab === "blocks" && (
        <>
          <div className="flex justify-end">
            {!showBlockForm && (
              <Button onClick={() => setShowBlockForm(true)}>
                <Plus className="h-4 w-4 mr-2" />
                Novo Bloqueio
              </Button>
            )}
          </div>

          {showBlockForm && (
            <Card>
              <CardHeader>
                <CardTitle>Novo Bloqueio</CardTitle>
              </CardHeader>
              <CardContent>
                <form onSubmit={handleBlockSubmit} className="space-y-4">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label>Tipo de Bloqueio</Label>
                      <Select
                        value={blockForm.blockType}
                        onValueChange={(v) =>
                          setBlockForm({ ...blockForm, blockType: v as "VACATION" | "HOLIDAY" | "BREAK" })
                        }
                      >
                        <SelectTrigger>
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          {BLOCK_TYPES.map((type) => (
                            <SelectItem key={type.value} value={type.value}>
                              {type.label}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="startDateTime">Início</Label>
                      <Input
                        id="startDateTime"
                        type="datetime-local"
                        value={blockForm.startDateTime}
                        onChange={(e) =>
                          setBlockForm({ ...blockForm, startDateTime: e.target.value })
                        }
                        required
                      />
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="endDateTime">Fim</Label>
                      <Input
                        id="endDateTime"
                        type="datetime-local"
                        value={blockForm.endDateTime}
                        onChange={(e) =>
                          setBlockForm({ ...blockForm, endDateTime: e.target.value })
                        }
                        required
                      />
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="reason">Motivo (opcional)</Label>
                      <Input
                        id="reason"
                        value={blockForm.reason}
                        onChange={(e) =>
                          setBlockForm({ ...blockForm, reason: e.target.value })
                        }
                        placeholder="Ex: Férias de fim de ano..."
                      />
                    </div>
                  </div>

                  <div className="flex gap-2">
                    <Button type="submit" disabled={saving}>
                      {saving && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
                      Criar
                    </Button>
                    <Button type="button" variant="outline" onClick={resetBlockForm}>
                      Cancelar
                    </Button>
                  </div>
                </form>
              </CardContent>
            </Card>
          )}

          <Card>
            <CardHeader>
              <CardTitle>Bloqueios Ativos</CardTitle>
            </CardHeader>
            <CardContent>
              {loading ? (
                <div className="flex justify-center py-8">
                  <Loader2 className="h-8 w-8 animate-spin text-primary" />
                </div>
              ) : blocks.length === 0 ? (
                <p className="text-muted-foreground text-center py-8">
                  Nenhum bloqueio cadastrado
                </p>
              ) : (
                <div className="space-y-2">
                  {blocks.map((block) => (
                    <div
                      key={block.id}
                      className="flex items-center justify-between p-4 border rounded-lg"
                    >
                      <div className="flex items-center gap-4">
                        <Ban className="h-5 w-5 text-red-500" />
                        <div>
                          <p className="font-medium">
                            {getBlockTypeLabel(block.blockType)}
                          </p>
                          <p className="text-sm text-muted-foreground">
                            {format(new Date(block.startDateTime), "dd/MM/yyyy HH:mm")} -{" "}
                            {format(new Date(block.endDateTime), "dd/MM/yyyy HH:mm")}
                          </p>
                          {block.reason && (
                            <p className="text-xs text-muted-foreground">
                              {block.reason}
                            </p>
                          )}
                        </div>
                      </div>

                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handleDeleteBlock(block.id)}
                      >
                        <Trash2 className="h-4 w-4 text-red-500" />
                      </Button>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </>
      )}
    </div>
  );
}
