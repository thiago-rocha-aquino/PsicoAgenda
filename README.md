# PsicoAgenda

Sistema de agendamento online para consultorio de psicologia.

## Stack Tecnologica

- **Backend**: Java 21 + Spring Boot 3.2
- **Frontend**: Next.js 14 + TypeScript + Tailwind CSS + shadcn/ui
- **Banco de Dados**: PostgreSQL 16
- **Containerizacao**: Docker + Docker Compose

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

## Requisitos

- Docker 20.10+
- Docker Compose 2.0+

Para desenvolvimento local:
- Java 21+
- Node.js 20+
- PostgreSQL 16+

## Inicio Rapido (Docker)

1. Clone o repositorio:
```bash
git clone <repo-url>
cd psicologia
```

2. Configure as variaveis de ambiente:
```bash
cp .env.example .env
# Edite o arquivo .env com suas configuracoes
```

3. Inicie os containers:
```bash
docker-compose up -d
```

4. Acesse:
- Frontend: http://localhost:3075
- Backend API: http://localhost:8085
- Swagger UI: http://localhost:8085/swagger-ui.html

5. Login admin padrao:
- Usuario: `admin` (ou valor de ADMIN_USERNAME)
- Senha: `admin123` (ou valor de ADMIN_PASSWORD)

## Desenvolvimento Local

### Banco de Dados

Inicie apenas o PostgreSQL:
```bash
docker-compose -f docker-compose.dev.yml up -d
```

### Backend

```bash
cd backend

# Configurar variaveis (Linux/Mac)
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5436/psicoagenda
export SPRING_DATASOURCE_USERNAME=psicoagenda
export SPRING_DATASOURCE_PASSWORD=psicoagenda123
export JWT_SECRET=your-super-secret-jwt-key-must-be-at-least-256-bits-long
export ENCRYPTION_SECRET=your-32-character-encryption-key!

# Executar
./mvnw spring-boot:run

# Ou executar testes
./mvnw test
```

### Frontend

```bash
cd frontend

# Instalar dependencias
npm install

# Configurar API
export NEXT_PUBLIC_API_URL=http://localhost:8085

# Executar em desenvolvimento
npm run dev

# Build de producao
npm run build
```

## Variaveis de Ambiente

### Banco de Dados
| Variavel | Descricao | Padrao |
|----------|-----------|--------|
| POSTGRES_DB | Nome do banco | psicoagenda |
| POSTGRES_USER | Usuario do banco | psicoagenda |
| POSTGRES_PASSWORD | Senha do banco | psicoagenda123 |

### Seguranca JWT
| Variavel | Descricao | Padrao |
|----------|-----------|--------|
| JWT_SECRET | Chave secreta JWT (min 256 bits) | - |
| JWT_EXPIRATION | Expiracao do token (ms) | 86400000 (24h) |
| JWT_REFRESH_EXPIRATION | Expiracao do refresh token (ms) | 604800000 (7d) |

### Criptografia
| Variavel | Descricao | Padrao |
|----------|-----------|--------|
| ENCRYPTION_SECRET | Chave AES-256 (32 caracteres) | - |

### Admin
| Variavel | Descricao | Padrao |
|----------|-----------|--------|
| ADMIN_USERNAME | Usuario admin inicial | admin |
| ADMIN_PASSWORD | Senha admin inicial | admin123 |
| ADMIN_EMAIL | Email admin inicial | admin@psicoagenda.com |

### Regras de Agendamento
| Variavel | Descricao | Padrao |
|----------|-----------|--------|
| BOOKING_MIN_ADVANCE_HOURS | Antecedencia minima (horas) | 12 |
| BOOKING_MAX_ADVANCE_DAYS | Antecedencia maxima (dias) | 90 |
| BOOKING_CANCELLATION_HOURS | Prazo cancelamento (horas) | 24 |
| BOOKING_SLOT_DURATION_MINUTES | Duracao slot (minutos) | 15 |

### Notificacoes Email
| Variavel | Descricao | Padrao |
|----------|-----------|--------|
| NOTIFICATIONS_ENABLED | Habilitar notificacoes | false |
| SMTP_HOST | Servidor SMTP | smtp.gmail.com |
| SMTP_PORT | Porta SMTP | 587 |
| SMTP_USERNAME | Usuario SMTP | - |
| SMTP_PASSWORD | Senha SMTP | - |
| NOTIFICATION_FROM_EMAIL | Email remetente | noreply@psicoagenda.com |
| NOTIFICATION_FROM_NAME | Nome remetente | PsicoAgenda |

### Retencao de Dados
| Variavel | Descricao | Padrao |
|----------|-----------|--------|
| RETENTION_ENABLED | Habilitar anonimizacao | false |
| RETENTION_MONTHS | Meses ate anonimizar | 24 |

### Frontend
| Variavel | Descricao | Padrao |
|----------|-----------|--------|
| NEXT_PUBLIC_API_URL | URL da API backend | http://localhost:8085 |

## Dados Iniciais (Seeds)

O sistema cria automaticamente ao iniciar:

1. **Usuario admin** com credenciais configuradas
2. **Tipos de sessao**:
   - Sessao Individual (50min, R$200)
   - Primeira Consulta (60min, R$250)
   - Sessao de Casal (80min, R$350)
3. **Disponibilidade**:
   - Segunda a Sexta: 08:00 - 12:00, 14:00 - 18:00
4. **Termo de consentimento** LGPD padrao

## Testes

### Backend

```bash
cd backend

# Testes unitarios
./mvnw test

# Testes de integracao (requer Docker)
./mvnw verify -P integration-test
```

Os testes de integracao usam Testcontainers para criar um PostgreSQL temporario.

### Frontend

```bash
cd frontend
npm test
```

## API Documentation

Com o backend rodando, acesse:
- Swagger UI: http://localhost:8085/swagger-ui.html
- OpenAPI JSON: http://localhost:8085/v3/api-docs

### Endpoints Principais

#### Publicos (sem autenticacao)
- `GET /api/public/session-types` - Tipos de sessao ativos
- `GET /api/public/slots?date=YYYY-MM-DD&sessionTypeId=UUID` - Horarios disponiveis
- `POST /api/public/book` - Criar agendamento
- `POST /api/public/cancel` - Cancelar agendamento
- `POST /api/public/reschedule` - Reagendar
- `GET /api/public/consent` - Termo vigente

#### Admin (requer JWT)
- `POST /api/auth/login` - Login
- `POST /api/auth/refresh` - Renovar token
- `GET /api/admin/dashboard` - Metricas
- `GET /api/admin/appointments` - Lista agendamentos
- `PATCH /api/admin/appointments/{id}/status` - Alterar status
- `GET /api/admin/session-types` - CRUD tipos sessao
- `GET /api/admin/availabilities` - CRUD disponibilidade
- `GET /api/admin/blocks` - CRUD bloqueios
- `GET /api/admin/recurring-series` - Series recorrentes
- `GET /api/admin/payments` - Pagamentos
- `GET /api/admin/patients` - Pacientes

## Deploy em Producao

### Recomendacoes

1. **Seguranca**:
   - Use senhas fortes para JWT_SECRET e ENCRYPTION_SECRET
   - Configure HTTPS com proxy reverso (nginx/traefik)
   - Nao exponha a porta do PostgreSQL

2. **Performance**:
   - Configure limites de recursos no docker-compose
   - Use volumes nomeados para persistencia

3. **Backup**:
   - Configure backup automatico do PostgreSQL
   - Guarde a ENCRYPTION_SECRET (dados criptografados sao irrecuperaveis sem ela)

### Exemplo com Traefik

```yaml
# Adicione labels ao frontend no docker-compose.yml
frontend:
  labels:
    - "traefik.enable=true"
    - "traefik.http.routers.psicoagenda.rule=Host(`agenda.seudominio.com`)"
    - "traefik.http.routers.psicoagenda.tls.certresolver=letsencrypt"
```

## Estrutura do Projeto

```
psicologia/
├── backend/
│   ├── src/main/java/com/psicoagenda/
│   │   ├── domain/           # Entidades, Enums, Repositorios
│   │   ├── application/      # DTOs, Services, Exceptions
│   │   ├── infrastructure/   # Seguranca, Notificacoes, Audit
│   │   └── api/              # Controllers REST
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/     # Flyway migrations
│   └── src/test/
├── frontend/
│   ├── app/                  # Next.js App Router
│   │   ├── page.tsx          # Landing page
│   │   ├── agendar/          # Wizard de agendamento
│   │   ├── confirmacao/      # Confirmacao
│   │   ├── cancelar/         # Cancelamento
│   │   ├── reagendar/        # Reagendamento
│   │   ├── politicas/        # Termos
│   │   └── admin/            # Painel admin
│   ├── components/           # Componentes UI
│   ├── hooks/                # React hooks
│   └── lib/                  # API client, utils
├── docker-compose.yml        # Producao
├── docker-compose.dev.yml    # Desenvolvimento
└── .env.example
```

## Licenca

Projeto privado. Todos os direitos reservados.
