"use client";

import { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { useAuth } from "@/hooks/use-auth";
import { adminApi, Patient } from "@/lib/api";
import { formatPhoneDisplay } from "@/lib/utils";
import { Search, Loader2 } from "lucide-react";

export default function PacientesPage() {
  const { token } = useAuth();

  const [patients, setPatients] = useState<Patient[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");

  useEffect(() => {
    loadPatients();
  }, [token]);

  async function loadPatients() {
    if (!token) return;

    try {
      const data = await adminApi.getPatients(token);
      setPatients(data);
    } catch (error) {
      console.error("Erro ao carregar pacientes:", error);
    } finally {
      setLoading(false);
    }
  }

  const filteredPatients = patients.filter((p) =>
    p.name.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Pacientes</h1>

      <Card>
        <CardHeader>
          <div className="flex items-center gap-4">
            <CardTitle>Lista de Pacientes</CardTitle>
            <div className="flex-1 max-w-sm relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Buscar por nome..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="pl-9"
              />
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="flex justify-center py-8">
              <Loader2 className="h-8 w-8 animate-spin text-primary" />
            </div>
          ) : filteredPatients.length === 0 ? (
            <p className="text-muted-foreground text-center py-8">
              Nenhum paciente encontrado
            </p>
          ) : (
            <div className="space-y-2">
              {filteredPatients.map((patient) => (
                <div
                  key={patient.id}
                  className="flex items-center justify-between p-4 border rounded-lg hover:bg-gray-50"
                >
                  <div>
                    <p className="font-medium">{patient.name}</p>
                    <p className="text-sm text-muted-foreground">
                      {patient.anonymized ? "***" : formatPhoneDisplay(patient.phone)}
                    </p>
                    {patient.email && !patient.anonymized && (
                      <p className="text-sm text-muted-foreground">{patient.email}</p>
                    )}
                  </div>
                  {patient.anonymized && (
                    <span className="text-xs text-muted-foreground">Anonimizado</span>
                  )}
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
