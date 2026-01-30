# PsicoAgenda

Sistema de agendamento online para consultorio de psicologia.

## Stack Tecnologica
- **Backend**: Java 21 + Spring Boot 3.2
- **Frontend**: Next.js  + TypeScript + Tailwind CSS + shadcn/ui
- **Banco de Dados**: PostgreSQL 
- **Containerizacao**: Docker

## Funcionalidades

### Area Publica
- Agendamento online com selecao de tipo de sessao, data e horario
- Confirmacao de agendamento com link unico
- Cancelamento e reagendamento pelo paciente
- Visualizacao de termos de uso e politica de cancelamento
- Aceite de consentimento LGPD

### Painel Administrativo
- Dashboard com metricas e agendamentos do dia
- Gerenciamento de agendamentos (confirmar, cancelar, marcar presenca)
- Configuracao de tipos de sessao (duracao, valor)
- Configuracao de disponibilidade semanal
- Bloqueios de agenda (ferias, feriados)
- Series recorrentes (semanal/quinzenal)
- Gestao de pagamentos
- Lista de pacientes

### Seguranca e Conformidade
- Autenticacao JWT com refresh token
- Criptografia AES-256-GCM para dados sensiveis (telefone, email)
- Audit log de todas as operacoes
- Anonimizacao opcional de dados antigos (LGPD)
- Notificacoes por email (lembretes 24h e 2h)


