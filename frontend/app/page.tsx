import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Calendar, Clock, Shield } from "lucide-react";

export default function HomePage() {
  return (
    <div className="min-h-screen bg-gradient-to-b from-purple-50 to-white">
      <header className="container mx-auto px-4 py-6">
        <nav className="flex items-center justify-between">
          <h1 className="text-2xl font-bold text-primary">Agenda Online</h1>
          <div className="flex gap-4">
            <Link href="/politicas">
              <Button variant="ghost">Politicas</Button>
            </Link>
            <Link href="/admin">
              <Button variant="outline">Area Restrita</Button>
            </Link>
          </div>
        </nav>
      </header>

      <main className="container mx-auto px-4 py-12">
        <section className="text-center mb-16">
          <h2 className="text-4xl font-bold text-gray-900 mb-4">
            Agende seu compromisso online
          </h2>
          <p className="text-xl text-gray-600 mb-8 max-w-2xl mx-auto">
            Escolha o melhor horario para voce de forma pratica e segura.
          </p>
          <Link href="/agendar">
            <Button size="lg" className="text-lg px-8 py-6">
              Agendar Agora
            </Button>
          </Link>
        </section>

        <section className="grid md:grid-cols-3 gap-8 max-w-4xl mx-auto">
          <Card>
            <CardHeader>
              <Calendar className="h-10 w-10 text-primary mb-2" />
              <CardTitle>Agendamento Facil</CardTitle>
              <CardDescription>
                Escolha a data e horario que melhor se encaixa na sua rotina
              </CardDescription>
            </CardHeader>
          </Card>

          <Card>
            <CardHeader>
              <Clock className="h-10 w-10 text-primary mb-2" />
              <CardTitle>Lembretes Automaticos</CardTitle>
              <CardDescription>
                Receba lembretes por email antes do seu compromisso
              </CardDescription>
            </CardHeader>
          </Card>

          <Card>
            <CardHeader>
              <Shield className="h-10 w-10 text-primary mb-2" />
              <CardTitle>Privacidade Garantida</CardTitle>
              <CardDescription>
                Seus dados sao protegidos e tratados com confidencialidade
              </CardDescription>
            </CardHeader>
          </Card>
        </section>
      </main>

      <footer className="container mx-auto px-4 py-8 mt-16 border-t">
        <div className="flex flex-col md:flex-row justify-between items-center gap-4">
          <p className="text-gray-600">
            &copy; {new Date().getFullYear()} Agenda Online
          </p>
          <div className="flex gap-4">
            <Link href="/politicas" className="text-gray-600 hover:text-primary">
              Politica de Privacidade
            </Link>
            <Link href="/politicas#cancelamento" className="text-gray-600 hover:text-primary">
              Politica de Cancelamento
            </Link>
          </div>
        </div>
      </footer>
    </div>
  );
}
