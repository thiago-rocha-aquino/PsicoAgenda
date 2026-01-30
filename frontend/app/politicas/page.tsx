"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { publicApi, ConsentVersion } from "@/lib/api";
import { ArrowLeft, Loader2 } from "lucide-react";

export default function PoliticasPage() {
  const [consent, setConsent] = useState<ConsentVersion | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadConsent() {
      try {
        const data = await publicApi.getConsent();
        setConsent(data);
      } catch (err) {
        console.error("Erro ao carregar termo:", err);
      } finally {
        setLoading(false);
      }
    }

    loadConsent();
  }, []);

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="container mx-auto px-4 max-w-3xl">
        <Link href="/" className="inline-flex items-center text-gray-600 hover:text-primary mb-6">
          <ArrowLeft className="h-4 w-4 mr-2" />
          Voltar ao inicio
        </Link>

        <Card>
          <CardHeader>
            <CardTitle>Termos de Uso e Politica de Privacidade</CardTitle>
          </CardHeader>

          <CardContent>
            {loading ? (
              <div className="flex justify-center py-8">
                <Loader2 className="h-8 w-8 animate-spin text-primary" />
              </div>
            ) : consent ? (
              <div className="prose prose-sm max-w-none">
                <div className="whitespace-pre-wrap text-gray-700">
                  {consent.content}
                </div>
                <p className="text-sm text-gray-500 mt-8">
                  Versao {consent.version} - Vigente desde{" "}
                  {new Date(consent.effectiveFrom).toLocaleDateString("pt-BR")}
                </p>
              </div>
            ) : (
              <p className="text-gray-500">Conteudo nao disponivel</p>
            )}
          </CardContent>
        </Card>

        <div id="cancelamento" className="mt-8">
          <Card>
            <CardHeader>
              <CardTitle>Politica de Cancelamento</CardTitle>
            </CardHeader>
            <CardContent className="prose prose-sm max-w-none text-gray-700">
              <ul>
                <li>
                  <strong>Cancelamentos com mais de 24 horas de antecedencia:</strong>
                  <br />
                  Podem ser feitos livremente atraves do link enviado na confirmacao.
                </li>
                <li>
                  <strong>Cancelamentos com menos de 24 horas:</strong>
                  <br />
                  Devem ser comunicados diretamente. Podem estar sujeitos a cobranca.
                </li>
                <li>
                  <strong>Faltas sem aviso:</strong>
                  <br />
                  Serao consideradas para fins de cobranca.
                </li>
                <li>
                  <strong>Reagendamentos:</strong>
                  <br />
                  Permitidos com no minimo 24 horas de antecedencia, sujeitos a disponibilidade.
                </li>
              </ul>
            </CardContent>
          </Card>
        </div>

        <div className="mt-8 text-center">
          <Link href="/agendar">
            <Button>Fazer Agendamento</Button>
          </Link>
        </div>
      </div>
    </div>
  );
}
