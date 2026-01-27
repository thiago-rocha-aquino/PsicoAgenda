-- Seed data for PsicoAgenda

-- Default session types
INSERT INTO session_type (id, name, duration_minutes, price, description, active, display_order) VALUES
(gen_random_uuid(), 'Sessão Individual (50 min)', 50, 200.00, 'Atendimento individual padrão', TRUE, 1),
(gen_random_uuid(), 'Primeira Consulta (60 min)', 60, 250.00, 'Consulta inicial para novos pacientes', TRUE, 0),
(gen_random_uuid(), 'Sessão Estendida (80 min)', 80, 300.00, 'Sessão com duração estendida', TRUE, 2);

-- Default availability (Monday to Friday, 8h-12h and 14h-18h)
INSERT INTO availability (id, day_of_week, start_time, end_time, active) VALUES
-- Segunda-feira
(gen_random_uuid(), 'MONDAY', '08:00', '12:00', TRUE),
(gen_random_uuid(), 'MONDAY', '14:00', '18:00', TRUE),
-- Terça-feira
(gen_random_uuid(), 'TUESDAY', '08:00', '12:00', TRUE),
(gen_random_uuid(), 'TUESDAY', '14:00', '18:00', TRUE),
-- Quarta-feira
(gen_random_uuid(), 'WEDNESDAY', '08:00', '12:00', TRUE),
(gen_random_uuid(), 'WEDNESDAY', '14:00', '18:00', TRUE),
-- Quinta-feira
(gen_random_uuid(), 'THURSDAY', '08:00', '12:00', TRUE),
(gen_random_uuid(), 'THURSDAY', '14:00', '18:00', TRUE),
-- Sexta-feira
(gen_random_uuid(), 'FRIDAY', '08:00', '12:00', TRUE),
(gen_random_uuid(), 'FRIDAY', '14:00', '18:00', TRUE);

-- Default consent version
INSERT INTO consent_version (id, version, content, effective_from, active) VALUES
(gen_random_uuid(), '1.0',
'TERMO DE CONSENTIMENTO E POLÍTICA DE PRIVACIDADE

1. COLETA DE DADOS
Coletamos apenas os dados estritamente necessários para o agendamento: nome e contato (telefone e/ou e-mail).

2. USO DOS DADOS
Seus dados serão utilizados exclusivamente para:
- Agendamento e confirmação de compromissos
- Envio de lembretes
- Comunicação necessária sobre seus agendamentos

3. ARMAZENAMENTO E PROTEÇÃO
- Seus dados de contato são armazenados de forma criptografada
- Implementamos medidas técnicas de segurança para proteger suas informações
- Não compartilhamos seus dados com terceiros

4. SEUS DIREITOS (LGPD)
Você tem direito a:
- Solicitar acesso aos seus dados
- Solicitar correção de dados incorretos
- Solicitar exclusão dos seus dados (observadas obrigações legais de retenção)
- Revogar seu consentimento a qualquer momento

5. POLÍTICA DE CANCELAMENTO
- Cancelamentos devem ser feitos com pelo menos 24 horas de antecedência
- Cancelamentos com menos de 24h de antecedência podem ser cobrados

6. CONTATO
Para exercer seus direitos ou esclarecer dúvidas, entre em contato conosco.

Ao prosseguir com o agendamento, você declara que leu e concorda com este termo.',
CURRENT_TIMESTAMP, TRUE);
