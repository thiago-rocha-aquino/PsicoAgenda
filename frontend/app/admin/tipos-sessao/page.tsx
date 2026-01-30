"use client";

import { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useAuth } from "@/hooks/use-auth";
import { useToast } from "@/hooks/use-toast";
import { adminApi, SessionType } from "@/lib/api";
import { Plus, Pencil, Trash2, Loader2, X, Check } from "lucide-react";

export default function TiposSessaoPage() {
  const { token } = useAuth();
  const { toast } = useToast();

  const [sessionTypes, setSessionTypes] = useState<SessionType[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const [form, setForm] = useState({
    name: "",
    durationMinutes: 50,
    price: 0,
    active: true,
  });

  useEffect(() => {
    loadSessionTypes();
  }, [token]);

  async function loadSessionTypes() {
    if (!token) return;

    try {
      const data = await adminApi.getSessionTypes(token);
      setSessionTypes(data);
    } catch (error) {
      console.error("Erro ao carregar tipos de sessão:", error);
    } finally {
      setLoading(false);
    }
  }

  function resetForm() {
    setForm({ name: "", durationMinutes: 50, price: 0, active: true });
    setEditingId(null);
    setShowForm(false);
  }

  function startEdit(st: SessionType) {
    setForm({
      name: st.name,
      durationMinutes: st.durationMinutes,
      price: st.price,
      active: st.active,
    });
    setEditingId(st.id);
    setShowForm(true);
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!token) return;

    setSaving(true);
    try {
      if (editingId) {
        await adminApi.updateSessionType(token, editingId, form);
        toast({
          title: "Tipo de sessão atualizado",
          description: "As alterações foram salvas.",
        });
      } else {
        await adminApi.createSessionType(token, form);
        toast({
          title: "Tipo de sessão criado",
          description: "O novo tipo foi adicionado.",
        });
      }
      resetForm();
      loadSessionTypes();
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

  async function handleDelete(id: string) {
    if (!token) return;
    if (!confirm("Deseja realmente excluir este tipo de sessão?")) return;

    try {
      await adminApi.deleteSessionType(token, id);
      toast({
        title: "Tipo de sessão excluído",
        description: "O registro foi removido.",
      });
      loadSessionTypes();
    } catch (error) {
      toast({
        title: "Erro",
        description: "Não foi possível excluir.",
        variant: "destructive",
      });
    }
  }

  async function toggleActive(st: SessionType) {
    if (!token) return;

    try {
      await adminApi.updateSessionType(token, st.id, {
        ...st,
        active: !st.active,
      });
      loadSessionTypes();
    } catch (error) {
      toast({
        title: "Erro",
        description: "Não foi possível atualizar.",
        variant: "destructive",
      });
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Tipos de Sessão</h1>
        {!showForm && (
          <Button onClick={() => setShowForm(true)}>
            <Plus className="h-4 w-4 mr-2" />
            Novo Tipo
          </Button>
        )}
      </div>

      {showForm && (
        <Card>
          <CardHeader>
            <CardTitle>{editingId ? "Editar" : "Novo"} Tipo de Sessão</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="name">Nome</Label>
                  <Input
                    id="name"
                    value={form.name}
                    onChange={(e) => setForm({ ...form, name: e.target.value })}
                    placeholder="Ex: Sessão Individual"
                    required
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="duration">Duração (minutos)</Label>
                  <Input
                    id="duration"
                    type="number"
                    min="15"
                    step="15"
                    value={form.durationMinutes}
                    onChange={(e) =>
                      setForm({ ...form, durationMinutes: parseInt(e.target.value) })
                    }
                    required
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="price">Valor (R$)</Label>
                  <Input
                    id="price"
                    type="number"
                    min="0"
                    step="0.01"
                    value={form.price}
                    onChange={(e) =>
                      setForm({ ...form, price: parseFloat(e.target.value) })
                    }
                    required
                  />
                </div>

                <div className="space-y-2">
                  <Label>Status</Label>
                  <div className="flex items-center gap-2 pt-2">
                    <input
                      type="checkbox"
                      id="active"
                      checked={form.active}
                      onChange={(e) => setForm({ ...form, active: e.target.checked })}
                      className="h-4 w-4"
                    />
                    <Label htmlFor="active" className="font-normal">
                      Ativo (disponível para agendamento)
                    </Label>
                  </div>
                </div>
              </div>

              <div className="flex gap-2">
                <Button type="submit" disabled={saving}>
                  {saving && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
                  {editingId ? "Salvar" : "Criar"}
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
          <CardTitle>Tipos Cadastrados</CardTitle>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="flex justify-center py-8">
              <Loader2 className="h-8 w-8 animate-spin text-primary" />
            </div>
          ) : sessionTypes.length === 0 ? (
            <p className="text-muted-foreground text-center py-8">
              Nenhum tipo de sessão cadastrado
            </p>
          ) : (
            <div className="space-y-2">
              {sessionTypes.map((st) => (
                <div
                  key={st.id}
                  className={`flex items-center justify-between p-4 border rounded-lg ${
                    st.active ? "bg-white" : "bg-gray-50 opacity-60"
                  }`}
                >
                  <div>
                    <div className="flex items-center gap-2">
                      <p className="font-medium">{st.name}</p>
                      {!st.active && (
                        <span className="text-xs bg-gray-200 px-2 py-0.5 rounded">
                          Inativo
                        </span>
                      )}
                    </div>
                    <p className="text-sm text-muted-foreground">
                      {st.durationMinutes} min • R$ {st.price.toFixed(2)}
                    </p>
                  </div>

                  <div className="flex items-center gap-2">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => toggleActive(st)}
                      title={st.active ? "Desativar" : "Ativar"}
                    >
                      {st.active ? (
                        <X className="h-4 w-4 text-gray-500" />
                      ) : (
                        <Check className="h-4 w-4 text-green-500" />
                      )}
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => startEdit(st)}
                    >
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => handleDelete(st.id)}
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
