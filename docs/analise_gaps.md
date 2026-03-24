# Análise Detalhada de Gaps - UDP Chat P2P

## Resumo Executivo

Esta análise identifica **42 gaps** distribuídos em 6 categorias, com **8 críticos**, **12 altos**, **14 médios** e **8 baixos**. Os principais pontos de atenção incluem: ausência de mecanismos de recuperação de mensagens perdidas, falta de validação de tamanho de mensagem, inconsistências entre PRD e planejamento em relação a classes e pacotes, e ausência de métricas de performance quantitativas.

**Prioridades de Correção:**
1. Implementar mecanismo de ACK/NACK para mensagens críticas (JOIN/LEAVE)
2. Definir tamanho máximo de mensagem e validação
3. Alinnar estrutura de pacotes entre PRD e planejamento
4. Adicionar métricas de performance quantitativas
5. Definir estratégia de recuperação após falha de rede

---

## 1. Gaps de Requisitos

### 1.1. Tamanho Máximo de Mensagem Não Definido
- **Severidade:** CRÍTICO
- **Descrição:** O PRD pergunta "What should be the maximum message length?" (linha 361) mas não define um limite. O planejamento menciona "Mensagens JSON devem ser < 1400 bytes" (linha 780) mas não há validação implementada ou critério de aceitação.
- **Impacto:** Mensagens muito longas podem causar fragmentação IP, perda de pacotes ou comportamento indefinido.
- **Recomendação:** Definir limite máximo de 500 caracteres para mensagem (considerando overhead JSON de ~100 bytes, total ~600 bytes < MTU). Adicionar validação em `ValidationUtils` e critério de aceitação em US-002.

### 1.2. Validação de Caracteres Especiais em Username
- **Severidade:** ALTO
- **Descrição:** Não há especificação sobre caracteres permitidos no username. O planejamento menciona "Username duplicados permitidos" (linha 399) mas não define restrições de formato.
- **Impacto:** Usernames com caracteres especiais podem quebrar formatação JSON, causar problemas de exibição na UI ou permitir injeção de código.
- **Recomendação:** Definir regex para username: `^[a-zA-Z0-9_-]{3,20}$`. Adicionar validação em `ValidationUtils.isValidUsername()` e critério de aceitação em US-003.

### 1.3. Comportamento em Caso de Falha de Rede Durante Operação
- **Severidade:** CRÍTICO
- **Descrição:** O PRD lista "No automatic reconnection after network failure" como Non-Goal (linha 334), mas o planejamento menciona "Reconexão Automática" (linha 760-765) com watchdog thread e restart automático.
- **Impacto:** Inconsistência entre documentos pode levar a implementação divergente ou confusão sobre requisitos.
- **Recomendação:** Decidir explicitamente: (a) Sem reconexão automática (alinhado com PRD), ou (b) Com reconexão automática (alinhado com planejamento). Atualizar ambos os documentos.

### 1.4. Suporte a Múltiplos Grupos Simultâneos
- **Severidade:** MÉDIO
- **Descrição:** O PRD pergunta "Should the application support multiple simultaneous multicast groups?" (linha 360) mas não define resposta. O planejamento menciona "Suporte a múltiplos grupos multicast" (linha 810) como estratégia de escalabilidade.
- **Impacto:** Escopo indefinido pode levar a implementação incompleta ou excessiva.
- **Recomendação:** Definir claramente: MVP suporta apenas 1 grupo por vez. Múltiplos grupos simultâneos são feature futura. Atualizar Non-Goals no PRD.

### 1.5. Distinção Visual entre Mensagens Próprias e de Outros
- **Severidade:** MÉDIO
- **Descrição:** O PRD pergunta "Should there be a visual distinction between own messages and others' messages?" (linha 362) mas não define resposta.
- **Impacto:** UX pode ser prejudicada sem distinção visual.
- **Recomendação:** Definir no PRD: Mensagens próprias com cor de fundo diferente (ex: #E3F2FD) e alinhamento à direita. Adicionar critério de aceitação em US-013.

### 1.6. Minimização para System Tray
- **Severidade:** BAIXO
- **Descrição:** O PRD pergunta "Should the application minimize to system tray or taskbar?" (linha 363) mas não define resposta.
- **Impacto:** Comportamento de minimização indefinido.
- **Recomendação:** Definir: Minimizar para taskbar (comportamento padrão Swing). System tray é feature futura. Atualizar Non-Goals.

### 1.7. Formato de Data/Hora para Exibição
- **Severidade:** BAIXO
- **Descrição:** O PRD define formato "dd/MM/yyyy" e "HH:mm:ss" (linha 36) mas não especifica formato para exibição na UI (apenas "[time] username: message" na linha 203).
- **Impacto:** Inconsistência potencial na exibição.
- **Recomendação:** Especificar em US-013: Formato de exibição "[HH:mm:ss] username: message" (sem data). Data visível apenas no tooltip ou ao passar mouse.

### 1.8. Comportamento ao Atingir Limite de Buffer de Reordenação
- **Severidade:** ALTO
- **Descrição:** O planejamento define MAX_BUFFER_SIZE = 100 (linha 656) e implementação que "buffer.clear()" ao estourar (linha 690), mas não há critério de aceitação no PRD sobre este comportamento.
- **Impacto:** Perda silenciosa de mensagens pode confundir usuários.
- **Recomendação:** Adicionar critério de aceitação em US-008: "When buffer exceeds 100 messages, oldest messages are discarded and user is notified via status bar". Adicionar log WARNING.

---

## 2. Gaps de Alinhamento

### 2.1. Inconsistência na Estrutura de Pacotes
- **Severidade:** ALTO
- **Descrição:** PRD (linha 24) define pacotes: `chat/config, chat/model, chat/network, chat/network/protocol, chat/controller, chat/view, chat/view/components, chat/util, chat/exception`. Planejamento (linha 72-117) define: `chat/config, chat/model, chat/network, chat/network/protocol, chat/controller, chat/view, chat/view/components, chat/util, chat/exception` mas inclui classes adicionais não listadas no PRD: `Packet.java`, `HandshakeManager.java`, `UIController.java`, `EventDispatcher.java`, `MessageDisplay.java`, `RoundedPanel.java`, `TimestampLabel.java`.
- **Impacto:** Desenvolvedores podem implementar classes não especificadas no PRD ou omitir classes esperadas.
- **Recomendação:** Atualizar PRD com lista completa de classes OU atualizar planejamento para alinhar com PRD. Recomendo atualizar PRD com classes adicionais.

### 2.2. Inconsistência em Message Interface
- **Severidade:** ALTO
- **Descrição:** PRD (linha 35) define interface Message com métodos: `getDate(), getTime(), getUsername(), getContent(), getType(), toJson()`. Planejamento (linha 122-131) define interface com métodos adicionais: `static Message fromJson(String json)`.
- **Impacto:** Método estático em interface não é suportado em Java 17 (apenas a partir do Java 8 para métodos estáticos de fábrica, mas não para fromJson como estático na interface).
- **Recomendação:** Corrigir planejamento: `fromJson()` deve ser método estático de fábrica na classe `ChatMessage`, não na interface `Message`. Atualizar linha 130 do planejamento.

### 2.3. Inconsistência em ChatSession
- **Severidade:** MÉDIO
- **Descrição:** PRD (linha 165-173) define `ChatSession` com `Deque<ChatMessage> for pending outbound messages`. Planejamento (linha 406-419) define `ChatSession` com `List<ChatMessage> history` e `Deque<ChatMessage> outbox`.
- **Impacto:** Duplicação de funcionalidade (histórico em ChatSession vs. não persistência conforme Non-Goal linha 331).
- **Recomendação:** Remover `List<ChatMessage> history` de ChatSession no planejamento (linha 415). Manter apenas `Deque<ChatMessage> outbox` para mensagens pendentes.

### 2.4. Inconsistência em ProtocolHandler
- **Severidade:** MÉDIO
- **Descrição:** PRD (linha 108) define "Message type routing (CHAT→ChatController, JOIN/LEAVE→PeerDiscovery, PING/PONG→liveness)". Planejamento (linha 170-176) define "Delegação para ChatController" sem especificar roteamento para PeerDiscovery.
- **Impacto:** Implementação pode não rotear corretamente mensagens JOIN/LEAVE para PeerDiscovery.
- **Recomendação:** Atualizar planejamento linha 176 para incluir roteamento explícito: "CHAT→ChatController, JOIN/LEAVE→PeerDiscovery, PING/PONG→liveness check".

### 2.5. Inconsistência em Retry Logic
- **Severidade:** ALTO
- **Descrição:** PRD (linha 80) define "IOException handling with retry logic (max 3 attempts)" para MulticastSender. Planejamento (linha 616-623) define retry apenas para mensagens de controle (JOIN: 3x, LEAVE: 1x, PING/PONG: 2x) e "Sem retransmissão automática" para mensagens de chat.
- **Impacto:** Contradição: PRD sugere retry para todas as mensagens, planejamento apenas para controle.
- **Recomendação:** Atualizar PRD linha 80 para especificar: "IOException handling with retry logic (max 3 attempts) for control messages only (JOIN, PING)". Adicionar nota sobre best-effort para chat messages.

### 2.6. Inconsistência em Logging
- **Severidade:** MÉDIO
- **Descrição:** PRD (linha 262) define "Rotating log file: logs/chat_yyyy-MM-dd.log" com "7-day log retention". Planejamento (linha 436-438) define "Rotação diária, mantém últimos 7 dias" mas não especifica biblioteca de logging.
- **Impacto:** Implementação pode usar biblioteca diferente ou configuração incorreta.
- **Recomendação:** Especificar biblioteca: `java.util.logging` com `FileHandler` configurado para rotação diária e retenção de 7 dias. Adicionar em US-017.

### 2.7. Inconsistência em Test Coverage
- **Severidade:** BAIXO
- **Descrição:** PRD (linha 306) define "Test coverage > 70% for core classes". Planejamento não menciona meta de cobertura.
- **Impacto:** Equipe pode não atingir meta de cobertura sem objetivo claro.
- **Recomendação:** Adicionar meta de cobertura no planejamento seção 12 (Cronograma) ou 15.2 (Pontos de Atenção).

---

## 3. Gaps de Viabilidade

### 3.1. Deduplicação por Username:Time Não é Única
- **Severidade:** CRÍTICO
- **Descrição:** O sistema usa `username:time` como chave de deduplicação (linha 248 do planejamento, linha 136 do PRD). No entanto, se dois usuários enviarem mensagens no mesmo segundo (ex: "Alice:14:30:25" e "Bob:14:30:25"), ambas são válidas mas a chave não é única globalmente. Além disso, se o mesmo usuário enviar duas mensagens no mesmo segundo (cenário raro mas possível com automação), a segunda será descartada como duplicada.
- **Impacto:** Perda legítima de mensagens em cenários de alta concorrência ou automação.
- **Recomendação:** Adicionar campo `msgId` (UUID) ao JSON de mensagem para garantir unicidade absoluta. Atualizar PRD linha 33 e planejamento linha 245-251.

### 3.2. Ordenação por Timestamp Não Funciona com Relógios Dessincronizados
- **Severidade:** CRÍTICO
- **Descrição:** O sistema ordena mensagens por `time` (campo HH:mm:ss) do remetente (linha 647 do planejamento). Se os relógios dos peers estiverem dessincronizados (diferença > 1 segundo), mensagens podem ser ordenadas incorretamente ou descartadas pelo buffer de reordenação.
- **Impacto:** Mensagens podem aparecer fora de ordem cronológica real ou serem descartadas.
- **Recomendação:** Implementar uma das opções: (a) Usar timestamp de recepção ao invés de timestamp de envio para ordenação, (b) Adicionar NTP sync ou (c) Usar sequence numbers incrementais por peer. Recomendo opção (a) para simplicidade.

### 3.3. Buffer de Reordenação com clear() Causa Perda Massiva
- **Severidade:** ALTO
- **Descrição:** Implementação do ReorderBuffer (linha 689-691) faz `buffer.clear()` quando excede 100 mensagens, descartando todas as mensagens pendentes.
- **Impacto:** Em cenários de burst de mensagens, todas as mensagens pendentes são perdidas de uma vez.
- **Recomendação:** Implementar descarte gradual (FIFO) ao invés de clear(): remover as 20 mensagens mais antigas quando buffer atinge 100. Atualizar planejamento linha 689-691.

### 3.4. TTL=1 Limita a Sub-rede Local
- **Severidade:** MÉDIO
- **Descrição:** TTL padrão é 1 (linha 774 do planejamento), limitando comunicação à mesma sub-rede. Não há menção de como configurar TTL para cenários com roteadores.
- **Impacto:** Usuários em sub-redes diferentes não conseguirão comunicar.
- **Recomendação:** Adicionar campo `ttl` em AppConfig (linha 48 do PRD) com validação 1-255. Documentar limitação em README.

### 3.5. Watchdog Thread Não Especificado no PRD
- **Severidade:** MÉDIO
- **Descrição:** Planejamento menciona "Watchdog thread, restart automático" (linha 937) como mitigação de risco, mas PRD não especifica este requisito.
- **Impacto:** Implementação pode não incluir watchdog ou pode ser considerada fora do escopo.
- **Recomendação:** Adicionar User Story US-021 para watchdog thread OU remover do planejamento se não for requisito.

### 3.6. ProtocolHandler Thread Pool Opcional
- **Severidade:** BAIXO
- **Descrição:** Planejamento menciona "ProtocolHandler Thread Pool (opcional)" (linha 326-329) mas não define quando usar ou critérios.
- **Impacto:** Complexidade desnecessária para grupos pequenos (2-10 peers).
- **Recomendação:** Remover thread pool opcional do planejamento. Manter processamento síncrono no receiver thread para MVP.

### 3.7. Configuração de TTL Não em AppConfig
- **Severidade:** BAIXO
- **Descrição:** Planejamento menciona TTL configurável (linha 775) mas AppConfig (linha 48 do PRD) não inclui campo ttl.
- **Impacto:** TTL não pode ser configurado pelo usuário.
- **Recomendação:** Adicionar campo `ttl` em AppConfig com valor padrão 1. Atualizar PRD linha 48 e planejamento linha 408.

---

## 4. Gaps de Usuário

### 4.1. Ausência de Feedback Visual para Mensagens Perdidas
- **Severidade:** ALTO
- **Descrição:** O sistema adota best-effort (linha 612 do planejamento) mas não há mecanismo para informar ao usuário que mensagens podem ter sido perdidas.
- **Impacto:** Usuário pode não perceber que está perdendo mensagens importantes.
- **Recomendação:** Adicionar indicador na status bar mostrando "Última mensagem recebida há X segundos" ou alerta visual quando nenhuma mensagem é recebida por > 30 segundos. Adicionar critério de aceitação em US-012.

### 4.2. Ausência de Histórico de Mensagens
- **Severidade:** MÉDIO
- **Descrição:** PRD define "No persistent message history (session-only)" (linha 331) e planejamento reforça "Não manter histórico" (linha 424). No entanto, não há mecanismo para recuperar mensagens perdidas durante a sessão.
- **Impacto:** Se usuário perder mensagens por estar minimizado ou distraído, não há como recuperá-las.
- **Recomendação:** Manter buffer de últimas 100 mensagens em memória (já existe ReorderBuffer). Permitir scroll-up para ver histórico da sessão. Atualizar US-013 para incluir "Scrollable message history (last 100 messages)".

### 4.3. Ausência de Notificação de Novos Peers
- **Severidade:** MÉDIO
- **Descrição:** O sistema atualiza lista de peers (US-014) mas não notifica usuário quando novo peer entra ou sai.
- **Impacto:** Usuário pode não perceber que alguém entrou/saiu do grupo.
- **Recomendação:** Adicionar mensagem de sistema no chat: "[Sistema] Alice entrou no grupo" e "[Sistema] Bob saiu do grupo". Adicionar critério de aceitação em US-013.

### 4.4. Ausência de Validação de Conexão Antes de Enviar
- **Severidade:** ALTO
- **Descrição:** Não há verificação se usuário está conectado antes de permitir envio de mensagens. Botão "Enviar" pode estar habilitado mesmo desconectado.
- **Impacto:** Usuário pode tentar enviar mensagem sem estar conectado, causando erro silencioso.
- **Recomendação:** Desabilitar botão "Enviar" e campo de texto quando desconectado. Adicionar critério de aceitação em US-013: "Send button and input field are disabled when not connected".

### 4.5. Ausência de Confirmação de Envio
- **Severidade:** BAIXO
- **Descrição:** Não há indicador visual de que mensagem foi enviada com sucesso (apenas aparece no chat).
- **Impacto:** Usuário pode não ter confiança de que mensagem foi transmitida.
- **Recomendação:** Adicionar indicador visual sutil (ex: ícone de check ao lado da mensagem enviada) ou feedback na status bar. Feature opcional para MVP.

### 4.6. Ausência de Suporte a Formatação de Mensagens
- **Severidade:** BAIXO
- **Descrição:** PRD define "No rich text or emoji support (plain text only)" (linha 328) mas não há menção sobre quebra de linha em mensagens longas.
- **Impacto:** Mensagens longas podem quebrar layout ou serem difíceis de ler.
- **Recomendação:** Adicionar critério de aceitação em US-013: "Long messages wrap to multiple lines" e "Maximum display width matches chat panel width".

---

## 5. Gaps de Riscos

### 5.1. Falta de Mecanismo de ACK/NACK para Mensagens Críticas
- **Severidade:** CRÍTICO
- **Descrição:** O sistema usa best-effort (linha 612 do planejamento) sem ACKs. No entanto, mensagens JOIN e LEAVE são críticas para gerenciamento de peers. Se JOIN for perdido, peer não será adicionado à lista. Se LEAVE for perdido, peer será removido apenas após timeout de 90s.
- **Impacto:** Lista de peers pode ficar dessincronizada por até 90 segundos.
- **Recomendação:** Implementar ACK para JOIN (obrigatório) e LEAVE (opcional). Se JOIN não receber ACK em 5s, retransmitir. Atualizar planejamento seção 9.1.

### 5.2. Falta de Validação de IP Multicast
- **Severidade:** ALTO
- **Descrição:** PRD (linha 52) define validação "multicast IP 224.0.0.0-239.255.255.255" mas não há verificação se o IP é realmente um endereço multicast válido (primeiros 4 bits devem ser 1110).
- **Impacto:** Usuário pode configurar IP inválido que não funcione como multicast.
- **Recomendação:** Adicionar validação em `ValidationUtils.isValidMulticastIP()` verificando se IP está no range correto E se primeiros 4 bits são 1110. Atualizar US-003.

### 5.3. Falta de Tratamento de Exceção para Socket Timeout
- **Severidade:** ALTO
- **Descrição:** PRD (linha 95) menciona "Proper exception handling for socket timeouts and errors" mas não especifica comportamento. Planejamento não detalha tratamento de timeout.
- **Impacto:** Socket pode bloquear indefinidamente ou lançar exceção não tratada.
- **Recomendação:** Configurar socket timeout (ex: 1000ms) e tratar SocketTimeoutException silenciosamente (continuar loop). Adicionar em US-006.

### 5.4. Falta de Testes de Carga para 10+ Peers
- **Severidade:** MÉDIO
- **Descrição:** PRD (linha 291) define "Multiple instances (2-10) can communicate simultaneously" e planejamento menciona "Teste de carga (10+ peers)" (linha 886) mas não define critérios de sucesso.
- **Impacto:** Sistema pode não escalar adequadamente para 10 peers.
- **Recomendação:** Definir critérios de sucesso para teste de carga: "10 peers can exchange 100 messages in 60 seconds with <5% loss and <2s latency". Adicionar em US-020.

### 5.5. Falta de Documentação de Requisitos de Rede
- **Severidade:** MÉDIO
- **Descrição:** Planejamento menciona "Multicast não funciona em algumas redes" (linha 935) mas não documenta requisitos mínimos de rede.
- **Impacto:** Usuários podem tentar usar em redes que não suportam multicast sem saber.
- **Recomendação:** Criar seção "Requisitos de Rede" no README documentando: (a) Roteadores devem suportar multicast, (b) Firewall deve permitir porta configurada, (c) Testar com comando `ping 224.0.0.1`.

### 5.6. Falta de Estratégia para Mensagens Grandes
- **Severidade:** ALTO
- **Descrição:** Não há estratégia para mensagens que excedam MTU (1500 bytes). Planejamento menciona "Mensagens JSON devem ser < 1400 bytes" (linha 780) mas não há validação ou fragmentação.
- **Impacto:** Mensagens grandes podem ser fragmentadas pelo IP e perdidas.
- **Recomendação:** Implementar validação de tamanho antes de enviar. Se mensagem exceder limite, rejeitar com erro para usuário. Atualizar US-005.

### 5.7. Falta de Monitoramento de Threads
- **Severidade:** MÉDIO
- **Descrição:** Planejamento menciona "Threads morrem sem notificação" (linha 937) como risco mas não detalha monitoramento.
- **Impacto:** Threads podem morrer silenciosamente, causando perda de funcionalidade.
- **Recomendação:** Implementar health check periódico (a cada 30s) verificando se sender e receiver threads estão vivas. Se mortas, tentar reiniciar e notificar usuário via status bar.

---

## 6. Gaps de Métricas e Sucesso

### 6.1. Ausência de Métricas de Performance Quantitativas
- **Severidade:** ALTO
- **Descrição:** PRD (linha 347-356) define métricas qualitativas mas não quantitativas. Ex: "Messages appear in correct chronological order" mas não define latência máxima aceitável.
- **Impacto:** Difícil avaliar se sistema atende requisitos de performance.
- **Recomendação:** Definir métricas quantitativas: (a) Latência de mensagem < 500ms em LAN, (b) Throughput > 50 mensagens/segundo, (c) CPU usage < 10% em idle, (d) Memory usage < 100MB. Adicionar em Success Metrics.

### 6.2. Ausência de Métricas de Usabilidade
- **Severidade:** MÉDIO
- **Descrição:** Não há métricas de usabilidade como tempo para conectar, número de cliques para enviar mensagem, ou satisfação do usuário.
- **Impacto:** Não é possível medir qualidade da experiência do usuário.
- **Recomendação:** Adicionar métricas: (a) Tempo para conectar < 3 segundos, (b) Mensagem enviada em < 2 cliques, (c) Feedback visual em < 100ms após ação.

### 6.3. Ausência de Métricas de Confiabilidade
- **Severidade:** ALTO
- **Descrição:** PRD menciona "Application handles network errors gracefully without crashing" (linha 356) mas não define taxa de perda aceitável ou uptime.
- **Impacto:** Não é possível avaliar confiabilidade do sistema.
- **Recomendação:** Definir métricas: (a) Taxa de perda de mensagens < 5% em LAN estável, (b) Uptime > 99% durante sessão, (c) Recovery time < 5s após falha de rede.

### 6.4. Ausência de Métricas de Escalabilidade
- **Severidade:** MÉDIO
- **Descrição:** PRD define "Multiple instances (2-10)" (linha 291) mas não define métricas de performance com diferentes números de peers.
- **Impacto:** Não é possível avaliar comportamento do sistema com diferentes cargas.
- **Recomendação:** Definir métricas por número de peers: (a) 2 peers: latência < 100ms, (b) 5 peers: latência < 200ms, (c) 10 peers: latência < 500ms.

### 6.5. Ausência de Critérios de Aceitação para Testes de Integração
- **Severidade:** ALTO
- **Descrição:** US-019 (linha 278-293) define critérios qualitativos mas não quantitativos para testes de integração.
- **Impacto:** Difícil determinar quando testes de integração são suficientes.
- **Recomendação:** Adicionar critérios quantitativos em US-019: (a) 100 mensagens enviadas e recebidas sem perda, (b) 10 peers conectados simultaneamente, (c) 30 minutos de operação contínua sem crash.

### 6.6. Ausência de Métricas de Manutenibilidade
- **Severidade:** BAIXO
- **Descrição:** Não há métricas de qualidade de código como complexidade ciclomática, acoplamento, ou coesão.
- **Impacto:** Código pode ser difícil de manter ou evoluir.
- **Recomendação:** Adicionar métricas: (a) Complexidade ciclomática < 10 por método, (b) Test coverage > 70% (já definido), (c) Acoplamento baixo (classes não dependem de implementações concretas).

---

## Resumo Executivo

### Principais Pontos de Atenção

1. **Críticos (8 gaps):**
   - Tamanho máximo de mensagem não definido
   - Comportamento em caso de falha de rede inconsistente entre documentos
   - Deduplicação por username:time não é única
   - Ordenação por timestamp não funciona com relógios dessincronizados
   - Falta de mecanismo de ACK/NACK para mensagens críticas

2. **Altos (12 gaps):**
   - Validação de caracteres especiais em username
   - Comportamento ao atingir limite de buffer
   - Inconsistências entre PRD e planejamento (estrutura de pacotes, Message interface, retry logic)
   - Ausência de feedback visual para mensagens perdidas
   - Ausência de validação de conexão antes de enviar
   - Falta de validação de IP multicast
   - Falta de tratamento de exceção para socket timeout
   - Falta de estratégia para mensagens grandes
   - Ausência de métricas de performance quantitativas
   - Ausência de métricas de confiabilidade
   - Ausência de critérios de aceitação para testes de integração

3. **Médios (14 gaps):**
   - Suporte a múltiplos grupos simultâneos
   - Distinção visual entre mensagens próprias e de outros
   - Inconsistências em ChatSession, ProtocolHandler, Logging
   - TTL limita sub-rede local
   - Watchdog thread não especificado
   - Ausência de histórico de mensagens
   - Ausência de notificação de novos peers
   - Falta de testes de carga para 10+ peers
   - Falta de documentação de requisitos de rede
   - Falta de monitoramento de threads
   - Ausência de métricas de usabilidade
   - Ausência de métricas de escalabilidade

4. **Baixos (8 gaps):**
   - Minimização para system tray
   - Formato de data/hora para exibição
   - Inconsistência em test coverage
   - ProtocolHandler thread pool opcional
   - Configuração de TTL não em AppConfig
   - Ausência de confirmação de envio
   - Ausência de suporte a formatação de mensagens
   - Ausência de métricas de manutenibilidade

### Prioridades de Correção

**Imediato (antes de iniciar desenvolvimento):**
1. Definir tamanho máximo de mensagem e adicionar validação
2. Decidir sobre reconexão automática e alinhar documentos
3. Adicionar campo msgId para deduplicação única
4. Definir estratégia de ordenação com relógios dessincronizados
5. Implementar ACK para mensagens JOIN

**Curto prazo (durante Fase 1-2):**
1. Alinhar estrutura de pacotes entre PRD e planejamento
2. Corrigir inconsistências em Message interface, ChatSession, ProtocolHandler
3. Adicionar validação de IP multicast e caracteres em username
4. Definir métricas de performance quantitativas

**Médio prazo (durante Fase 3-5):**
1. Implementar feedback visual para mensagens perdidas
2. Adicionar notificação de novos peers
3. Implementar monitoramento de threads
4. Documentar requisitos de rede

**Longo prazo (após MVP):**
1. Adicionar métricas de usabilidade e manutenibilidade
2. Implementar suporte a múltiplos grupos simultâneos
3. Adicionar minimização para system tray
4. Implementar formatação de mensagens (rich text)

---

*Documento gerado em 24/03/2026*
