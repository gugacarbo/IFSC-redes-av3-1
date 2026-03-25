# Planejamento Completo - Aplicativo UDP Chat P2P

## 1. Introdução e Escopo

Este documento apresenta o planejamento completo para o desenvolvimento de uma aplicação de chat desktop em Java utilizando comunicação UDP multicast peer-to-peer com interface gráfica Swing.

### 1.1. Requisitos Principais

- Interface gráfica (Swing)
- Comunicação exclusivamente via UDP multicast
- Formato JSON para mensagens
- Suporte a múltiplos usuários simultâneos
- Capacidade de entrar/sair de grupos dinamicamente
- Threads distintas para envio e recebimento

### 1.2. Restrições Técnicas

- Java 17+
- Maven como build tool
- UDP multicast (não unicast direto)
- Codificação UTF-8
- Porta configurável pelo usuário

---

## 2. Arquitetura do Sistema

### 2.1. Visão Geral

A arquitetura segue o padrão **MVC (Model-View-Controller)** adaptado para aplicações desktop:

```
┌─────────────────────────────────────────────────────────────┐
│                        VIEW (Swing)                         │
│  ┌─────────────┐  ┌─────────────┐  ┌──────────────────┐     │
│  │ Config Panel│  │ Chat Panel  │  │ Users List Panel │     │
│  └─────────────┘  └─────────────┘  └──────────────────┘     │
└─────────────────────────────┬───────────────────────────────┘
                              │ eventos
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   CONTROLLER                                │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  ChatController (gerencia interação View↔Model)      │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────┬───────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   MODEL                                     │
│  ┌─────────────┐  ┌─────────────┐  ┌──────────────────┐     │
│  │ Message     │  │ ChatSession │  │ PeerManager      │     │
│  └─────────────┘  └─────────────┘  └──────────────────┘     │
│  ┌─────────────┐  ┌─────────────┐                           │
│  │ UDPMulticast│  │ Config      │                           │
│  │ Sender      │  │             │                           │
│  └─────────────┘  └─────────────┘                           │
└─────────────────────────────────────────────────────────────┘
```

### 2.2. Padrões de Projeto Utilizados

1. **Singleton**: Para `UDPNetworkManager` (gerenciador único da rede)
2. **Observer**: Para notificar a UI sobre novas mensagens recebidas
3. **Factory Method**: Para criação de mensagens JSON
4. **Strategy**: Para diferentes estratégias de retransmissão/recuperação
5. **Thread-per-role**: Threads dedicadas para sender e receiver

---

## 3. Estrutura de Pacotes e Classes

### 3.1. Organização de Pacotes

```
chat/
├── Main.java                         # Classe principal (inicialização)
├── config/                           # Configurações
│   ├── AppConfig.java
│   └── ConfigDialog.java
├── model/                            # Modelo de dados
│   ├── Message.java                 # Classe mensagem (JSON)
│   ├── ChatMessage.java             # Implementação concreta
│   ├── Peer.java                    # Representa um peer
│   ├── ChatSession.java             # Estado da sessão atual
│   └── MessageQueue.java            # Fila para ordenação
├── network/                         # Camada de rede
│   ├── UDPNetworkManager.java       # Singleton (factory)
│   ├── MulticastSender.java         # Thread de envio
│   ├── MulticastReceiver.java       # Thread de recebimento
│   ├── PeerDiscovery.java           # Descoberta de peers
│   └── protocol/                    # Protocolo
│       ├── MessageType.java         # Enum (CHAT, JOIN, LEAVE, PING, PONG, ACK)
│       ├── ProtocolHandler.java     # Processador de protocolo
│       └── PendingRequestTracker.java # Gerenciador de retry (ACK tracking)
├── controller/                      # Controladores
│   ├── ChatController.java          # Controlador principal (MVC)
│   └── EventDispatcher.java         # Dispatcher de eventos (opcional)
├── view/                            # Interface gráfica
│   ├── MainFrame.java               # Janela principal (JFrame)
│   ├── ChatPanel.java               # Painel de chat
│   ├── ConfigPanel.java             # Painel de configuração
│   ├── UsersListPanel.java          # Lista de peers
│   ├── MessageDisplay.java          # Componente de exibição
│   └── components/                  # Componentes customizados
│       ├── RoundedPanel.java
│       └── TimestampLabel.java
├── util/                            # Utilitários
│   ├── JsonUtils.java               # Serialização JSON
│   ├── DateFormatter.java           # Formatação data/hora
│   ├── ThreadSafeList.java          # Lista thread-safe
│   ├── Logger.java                  # Logging
│   └── ValidationUtils.java         # Validações
└── exception/                       # Exceções customizadas
    ├── NetworkException.java
    ├── InvalidMessageException.java
    └── ConfigurationException.java
```

### 3.2. Classes Principais (Descrição Detalhada)

#### `Message.java` (Interface/Classe Abstrata)

```java
public interface Message {
    String getDate();
    String getTime();
    String getUsername();
    String getContent();
    MessageType getType();
    String toJson();
    static Message fromJson(String json);
}
```

#### `ChatMessage.java` (Implementação)

- Campos: date, time, username, message, type, msgId (UUID)
- Implementa Message
- Construtores: vazio, com parâmetros
- Métodos: toJson(), fromJson(), getMsgId()
- Chave única para deduplicação: msgId (UUID)

#### `ChatSession.java`

- Estado atual: username, multicastGroup, port, ttl, isConnected
- Lista de peers ativos (thread-safe)
- Fila de mensagens pendentes (outbox) com capacidade máxima 1000
- Métodos: join(), leave(), send(), broadcast()
- Registra shutdown hook para LEAVE graceful ao encerrar JVM

#### `UDPNetworkManager.java` (Singleton)

- Gerenciamento único do socket UDP
- Configuração: porta, grupo multicast, TTL
- Factory methods: createSender(), createReceiver()
- shutdown()

#### `MulticastSender.java` (Thread)

- Envio assíncrono de mensagens
- Queue de mensagens pendentes
- Retransmissão em caso de falha (opcional)
- Controle de taxa (throttling)

#### `MulticastReceiver.java` (Thread)

- Escuta contínua na porta multicast
- Parse de pacotes recebidos
- Filtragem de mensagens próprias
- Delegação para ProtocolHandler

#### `PeerDiscovery.java`

- Broadcast de presença (JOIN messages)
- Detecção de novos peers
- Timeout para peers inativos
- Manutenção da lista de peers

#### `ProtocolHandler.java`

- Processa diferentes MessageType (CHAT, JOIN, LEAVE, PING, PONG, ACK)
- Filtragem de mensagens próprias (ignorar mensagens enviadas por si mesmo)
- Ordenação de mensagens por receive timestamp (System.currentTimeMillis() ao receber)
- Deduplicação por msgId (UUID) com cache LRU (1000 entradas, 60s TTL)
- PING/PONG para liveness
- ACK para confirmação de mensagens de controle (JOIN, PING)
- Delegação para ChatController e PendingRequestTracker

---

## 4. Protocolo de Comunicação UDP Multicast

### 4.1. Descoberta de Peers

**Estratégia**: Broadcast periódico de mensagens JOIN

```
Peer A (novo)                           Rede Multicast
    │
    ├─ JOIN {username, ip, port, timestamp} ──────────►
    │                                              │ Peer B
    │                                              │ Peer C
    │                                              │ Peer D
    │◄─────────────────────────────────────────────┤
    │      ACK (opcional) ou nova mensagem JOIN    │
    │                                              │
    └─ Mantém lista local de peers ────────────────┘
```

**Detalhes**:

- Ao conectar, envia JOIN message a cada 2 segundos por 10 segundos
- Peers recebem JOIN e adicionam à lista
- Envia PING a cada 30s para manter presença
- Remove peers após 3 PINGs não respondidos (90s timeout)

### 4.2. Handshake e ACK

**Decisão: ACK obrigatório para mensagens de controle**

O sistema utiliza ACK/PONG para garantir confiabilidade de mensagens críticas:

1. **JOIN broadcast**: Peer envia JOIN a cada 2s por 10s na inicialização
2. **ACK response**: Peers recebem JOIN e respondem com PONG (ACK)
3. **Retry mechanism**: Se não houver ACK em 5s, reenvia JOIN (max 3 tentativas)
4. **PendingRequestTracker**: Rastreia JOIN/PING pendentes com timestamps e retry count
5. **Timeout cleanup**: Remove pending requests após 15s

**Vantagens**:

- Garante que peers são descobertos mesmo com perda de pacotes
- Confirmação explícita de recebimento
- Adequado para pequenos grupos (2-10 peers)

**Desvantagens**:

- Complexidade adicional (tracking de pending requests)
- Overhead de tráfego (ACKs)
- Necessidade de timeout e retry logic

### 4.3. Troca de Mensagens

**Formato JSON Padrão** (com msgId para deduplicação):

```json
{
	"type": "CHAT",
	"date": "24/03/2025",
	"time": "14:30:25",
	"username": "Alice",
	"message": "Olá a todos!",
	"msgId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Campos**:

- `type`: CHAT, JOIN, LEAVE, PING, PONG, ACK (obrigatório para protocolo)
- `date`: data de envio (dd/MM/yyyy)
- `time`: hora de envio (HH:mm:ss)
- `username`: nome do usuário
- `message`: conteúdo da mensagem (vazio para JOIN/LEAVE/PING/PONG/ACK)
- `msgId`: UUID único para deduplicação (obrigatório)

**Deduplicação**:

- Baseada em `msgId` (UUID) - identificador global único
- Cache LRU de 1000 entradas com TTL de 60 segundos
- Thread-safe implementation (ConcurrentHashMap ou synchronized)
- Limpeza automática de entries expiradas

**Ordenação**:

- Ordenar por **receive timestamp** (System.currentTimeMillis() ao receber)
- Se mesmo timestamp, ordenar por `msgId` (UUID)
- Buffer de reordenação: máximo 100 mensagens
- Delay máximo: 5 segundos (descartar mensagens mais antigas)
- Overflow handling: quando buffer atinge 100, remover 20 mensagens mais antigas (FIFO)

### 4.4. Fluxo de Mensagem

```
┌─────────────┐
│   Sender    │
│   Thread    │
└──────┬──────┘
       │ envia JSON via MulticastSocket
       ▼
┌─────────────────────────────────────────────┐
│        Rede UDP Multicast (224.0.0.0+)      │
└─────────────────────────────────────────────┘
       │
       ▼
┌─────────────┐
│  Receiver   │
│   Thread    │
└──────┬──────┘
       │ parse JSON
       ▼
┌─────────────────┐
│ ProtocolHandler │
└────────┬────────┘
         │ valida, ordena, deduplica
         ▼
┌─────────────────┐
│ ChatController  │
└────────┬────────┘
         │ notifica UI
         ▼
┌─────────────────┐
│   Swing UI      │ (Event Dispatch Thread)
└─────────────────┘
```

---

## 5. Tratamento de Concorrência

### 5.1. Modelo de Threads

```
Main Thread (EDT)
    │
    ├─ Inicializa UI
    ├─ Cria ChatController
    └─ Aguarda eventos da UI

ChatController Thread
    │
    ├─ Gerencia sessão
    ├─ Processa comandos do usuário
    └─ Delega para Sender/Receiver

MulticastSender Thread
    │
    ├─ BlockingQueue<Message> sendQueue
    ├─ Envio síncrono via DatagramSocket
    └─ Retry em caso de IOException

MulticastReceiver Thread
    │
    ├─ Loop infinito: socket.receive()
    ├─ Parse de DatagramPacket
    ├─ ProtocolHandler.process()
    └─ Notifica Controller via Observer

ProtocolHandler Thread Pool (opcional)
    │
    ├─ Processa mensagens em paralelo
    └─ Evita bloqueio do Receiver thread
```

### 5.2. Sincronização

- **BlockingQueue** para comunicação Sender↔Controller
- **ConcurrentHashMap** para lista de peers
- **CopyOnWriteArrayList** para histórico de mensagens (leitura pesada)
- **SwingUtilities.invokeLater()** para atualizar UI
- **volatile** para flags (isRunning, isConnected)

### 5.3. Deadlock Prevention

- Ordem de lock: PeerManager → MessageQueue → UI
- Timeouts em todas as operações de rede
- Não bloquear EDT (Event Dispatch Thread)

---

## 6. Modelo de Dados

### 6.1. Classes de Dados

#### `ChatMessage`

```java
public class ChatMessage implements Message {
    private String date;      // dd/MM/yyyy
    private String time;      // HH:mm:ss
    private String username;
    private String message;
    private MessageType type;

    // Construtores, getters, setters

    public String toJson() {
        return JsonUtils.toJson(this);
    }

    public static Message fromJson(String json) {
        return JsonUtils.fromJson(json, ChatMessage.class);
    }

    // Chave única para deduplicação: username + time
    public String getUniqueKey() {
        return username + ":" + time;
    }
}
```

#### `Peer`

```java
public class Peer {
    private String username;
    private InetAddress address;
    private int port;
    private long lastSeen;      // timestamp
    private boolean isActive;

    // Métodos: updateLastSeen(), isAlive(), etc.

    // Identificador único (IP:porta) para diferenciar duplicados
    public String getUniqueId() {
        return address.getHostAddress() + ":" + port;
    }

    // equals() e hashCode() baseados em uniqueId
    // Permite múltiplos peers com mesmo username mas IP:porta diferentes
}
```

**Decisão: Username duplicados permitidos** - Diferenciar por IP:porta

- Vários peers podem usar o mesmo username simultaneamente
- Identificação única: `IP:porta`
- Na UI, exibir: `username (IP:porta)`
- Simplifica a experiência (sem restrições) e adequado para pequenos grupos

#### `ChatSession`

```java
public class ChatSession {
    private String username;
    private String multicastGroup;  // ex: "224.0.0.1"
    private int port;
    private int ttl;  // 1-255
    private boolean connected;

    private List<Peer> peers;  // thread-safe (ConcurrentHashMap)
    private Deque<ChatMessage> outbox;  // capacity: 1000, overflow: discard oldest non-CHAT first
    // Nota: history não persistido (session-only)

    // Métodos: join(), leave(), send(), broadcast(), addPeer(), removePeer()
    // Registra shutdown hook no join() para enviar LEAVE na saída
}
```

### 6.2. Persistência

**Decisão: Não manter histórico** - Apenas mensagens em tempo real

- **Configurações**: `Properties` file em `~/.udp_chat/config.properties`

  - Salva: username, multicastGroup, port, ttl, windowWidth, windowHeight, windowX, windowY
  - Formato: Java Properties (key=value)
  - Valores padrão: port=5000, multicastGroup=224.0.0.1, ttl=1
  - Validação: port 1024-65535, multicast IP 224.0.0.0-239.255.255.255, ttl 1-255
  - Carregado na inicialização

- **Histórico de mensagens**: NÃO persistido

  - Mensagens são exibidas apenas durante a sessão atual
  - Não há buffer em memória além do necessário para ordenação
  - Ao desconectar/reconectar, histórico é limpo
  - Justificativa: Chat em tempo real, sem necessidade de histórico

- **Logs**: Arquivo rotativo `logs/chat_yyyy-MM-dd.log`
  - Nível: INFO para eventos, DEBUG para troubleshooting
  - Rotação diária, mantém últimos 7 dias

---

## 7. Interface Gráfica Swing

### 7.1. Layout Principal

```
┌─────────────────────────────────────────────────────────────┐
│                    MainFrame (JFrame)                      │
├─────────────────────────────────────────────────────────────┤
│  MenuBar:                                                 │
│  [Arquivo] [Configurações] [Ajuda]                        │
├─────────────────────────────────────────────────────────────┤
│  ToolBar:                                                 │
│  [Conectar] [Desconectar] [Limpar]                        │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────────┬──────────────────────────────┐  │
│  │                     │                              │  │
│  │  ChatPanel          │  UsersListPanel              │  │
│  │  (BorderLayout)     │  (List<Peer>)                │  │
│  │                     │                              │  │
│  │  ┌─────────────┐   │  ┌──────────────────────┐   │  │
│  │  │MessageArea  │   │  │ Username1 (online)   │   │  │
│  │  │(JScrollPane)│   │  │ Username2 (away)     │   │  │
│  │  │JTextArea    │   │  │ ...                  │   │  │
│  │  └─────────────┘   │  └──────────────────────┘   │  │
│  │                     │                              │  │
│  │  ┌─────────────┐   │                              │  │
│  │  │InputField   │   │                              │  │
│  │  │JTextField   │   │                              │  │
│  │  └─────────────┘   │                              │  │
│  │                     │                              │  │
│  └─────────────────────┴──────────────────────────────┘  │
├─────────────────────────────────────────────────────────────┤
│  StatusBar:                                               │
│  Conectado como "Alice" | Grupo: 224.0.0.1:5000         │
└─────────────────────────────────────────────────────────────┘
```

### 7.2. Componentes Swing

#### `MainFrame` (extends JFrame)

- Layout: BorderLayout
- MenuBar: JMenuBar com JMenu
- ToolBar: JToolBar com JButtons
- ContentPane: JSplitPane (horizontal)
- StatusBar: JLabel na SOUTH

#### `ChatPanel` (extends JPanel)

- Layout: BorderLayout
- NORTH: JLabel "Chat"
- CENTER: JScrollPane + JTextArea (não editável)
- SOUTH: JPanel com JTextField + JButton "Enviar"

#### `ConfigPanel` (extends JDialog)

- Formulário com JLabels e JTextFields
- Campos: Username, Multicast Group, Port
- Botões: Salvar, Cancelar, Testar Conexão

#### `UsersListPanel` (extends JPanel)

- Layout: BoxLayout (Y_AXIS)
- JList<Peer> com DefaultListModel
- CellRenderer customizado para mostrar status

### 7.3. Eventos e Listeners

```java
// No ChatController
controller.addMessageListener(new MessageListener() {
    @Override
    public void onMessageReceived(ChatMessage msg) {
        SwingUtilities.invokeLater(() -> {
            chatPanel.appendMessage(msg);
        });
    }
});

controller.addPeerListener(new PeerListener() {
    @Override
    public void onPeerJoined(Peer peer) {
        SwingUtilities.invokeLater(() -> {
            usersListPanel.addPeer(peer);
        });
    }
});

// Botão Enviar
sendButton.addActionListener(e -> {
    String text = inputField.getText();
    if (!text.trim().isEmpty()) {
        controller.sendMessage(text);
        inputField.setText("");
    }
});

// Tecla Enter no campo de input
inputField.addActionListener(e -> sendButton.doClick());
```

### 7.4. Atualização em Tempo Real

- **Receiver thread** → `ProtocolHandler` → `ChatController`
- `ChatController` notifica `MessageListener`
- `SwingUtilities.invokeLater()` garante atualização na EDT
- `JTextArea.append()` para adicionar mensagens
- `DefaultListModel` para atualizar lista de peers

---

## 8. Fluxo de Execução Principal

### 8.1. Inicialização

```
1. Main.main()
   │
   ├─ Carrega configurações (AppConfig.load())
   ├─ Cria ChatController
   ├─ Cria MainFrame (Swing)
   ├─ Configura listeners
   └─ Exibe frame (setVisible(true))

2. ChatController.init()
   │
   ├─ Inicializa ChatSession (com config)
   ├─ Cria UDPNetworkManager (singleton)
   ├─ Inicia MulticastReceiver thread
   ├─ Inicia MulticastSender thread
   └─ Dispara JOIN broadcast
```

### 8.2. Ciclo de Vida

```
┌─────────────────────────────────────────────────────────────┐
│                    Aplicação Rodando                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌────────────┐         ┌────────────┐                    │
│  │   UI       │◄───────►│ Controller │◄───────┐           │
│  │ (Swing)    │ eventos │            │ rede   │           │
│  └────────────┘         └────────────┘        │           │
│                                                │           │
│  ┌────────────────────────────────────────────┘           │
│  │                                                         │
│  │  ┌─────────────┐         ┌─────────────┐             │
│  │  │   Sender    │◄───────►│  Receiver   │             │
│  │  │   Thread    │ msg     │   Thread    │             │
│  │  └─────────────┘         └─────────────┘             │
│  │         │                       │                     │
│  │         ▼                       ▼                     │
│  │  ┌─────────────────────────────────────┐             │
│  │  │      Rede UDP Multicast             │             │
│  │  └─────────────────────────────────────┘             │
│  │                                                         │
│  └─────────────────────────────────────────────────────────┘
│                                                             │
│  Desconectar:                                               │
│  1. UI → controller.leave()                                │
│  2. Envia LEAVE broadcast                                  │
│  3. Para threads sender/receiver                           │
│  4. Fecha socket                                           │
│  5. Limpa lista de peers                                   │
└─────────────────────────────────────────────────────────────┘
```

---

## 9. Estratégias para Perda de Pacotes e Ordenação

### 9.1. Perda de Pacotes (UDP limitation)

**Decisão: Tolerar perdas (best-effort)** - Simples, sem retransmissão

O sistema adota uma abordagem de "melhor esforço" (best-effort):

1. **Sem retransmissão automática**:

   - Mensagens de chat podem se perder sem notificação
   - Adequado para chat casual onde perda ocasional é aceitável

2. **Retry apenas para mensagens de controle**:

   - JOIN: retry até 3x com exponential backoff (1s, 2s, 4s)
   - LEAVE: tentativa única (não crítico)
   - PING/PONG: retry até 2x

3. **Buffer de reordenação**:

   - Apesar de não haver retransmissão, mensagens podem chegar fora de ordem
   - Ordenação por timestamp (time) + username
   - Buffer limitado a 100 mensagens (PriorityQueue)

4. **Deduplicação**:
   - Baseada em username + time (combinação única)
   - Tolerância de 1 segundo
   - Cache de 1000 entradas (username:time)

**Justificativa**:

- Simplicidade de implementação
- Adequado para pequenos grupos (2-10 peers) em LAN confiável
- Overhead mínimo
- Não compromete a experiência do usuário em chat casual

### 9.2. Ordenação de Mensagens

**Problema**: UDP não garante ordem de entrega

**Solução**:

1. **Ordenação por timestamp**:

   - Mensagens ordenadas pelo campo `time` (HH:mm:ss)
   - Se mesmo timestamp, ordenar por `username` (alfabética)
   - Sem sequence numbers

2. **Buffer de Reordenação**:

```java
class ReorderBuffer {
    private Queue<ChatMessage> buffer;  // PriorityQueue ordenado por time
    private String lastTime;            // HH:mm:ss da última msg entregue
    private static final int MAX_BUFFER_SIZE = 100;
    private static final long MAX_DELAY_MS = 5000; // 5 segundos

    public void add(ChatMessage msg) {
        long msgTime = parseTime(msg.getTime());
        long lastTimeMs = parseTime(lastTime);

        if (msgTime <= lastTimeMs) {
            // Mensagem atrasada ou duplicada
            if (msgTime == lastTimeMs && isDuplicate(msg)) {
                // Duplicada: descarta
                return;
            }
            // Muito atrasada: descarta
            return;
        }

        // Adiciona ao buffer ordenado
        buffer.add(msg);

        // Tenta entregar mensagens em ordem
        while (!buffer.isEmpty()) {
            ChatMessage next = buffer.peek();
            if (parseTime(next.getTime()) <= lastTimeMs + 1000) {
                // Within 1 second tolerance, pode entregar
                deliver(buffer.poll());
                lastTime = next.getTime();
            } else {
                break;
            }
        }

        // Limite de buffer
        if (buffer.size() > MAX_BUFFER_SIZE) {
            buffer.clear();  // descarta tudo se buffer estourar
        }
    }

    private boolean isDuplicate(ChatMessage msg) {
        // Verificar cache de deduplicação: username + time
        String key = msg.getUsername() + ":" + msg.getTime();
        return dedupCache.contains(key);
    }
}
```

3. **Timeout de buffer**:
   - Mensagens buffered por > 5s são descartadas
   - Evita memória infinita e mensagens muito atrasadas

### 9.3. Deduplicação

**Decisão: Sem msgId, usar username + time**

- Cada mensagem identificada por `username + time` (combinação)
- Tolerância: 1 segundo (mensagens no mesmo segundo do mesmo usuário são consideradas duplicadas)
- Receiver mantém `Set<String> dedupCache` com chaves `username:time`
- Tamanho do cache: 1000 entradas (LRU)
- Cache limpo a cada 60 segundos (remove entradas > 60s)
- Se `username:time` já existe no cache, descarta mensagem

**Justificativa**:

- Simplifica o protocolo (sem campos adicionais)
- Adequado para chat humano (difícil enviar duas msg no mesmo segundo)
- Atende requisitos de deduplicação sem complexidade extra

---

## 10. Tratamento de Desconexão

### 10.1. Detecção de Peer Inativo

```
Peer A                                    Peer B
  │                                        │
  │─── PING (a cada 30s) ─────────────────►│
  │                                        │
  │◄────────── PONG (imediato) ────────────┤
  │                                        │
  │ (se não recebe PONG por 3 tentativas) │
  │─── marca Peer B como inativo ─────────►│
  │                                        │
  │─── remove da lista de peers ───────────┤
```

**Implementação**:

- `Peer.lastSeen` (timestamp)
- `PeerDiscovery` roda a cada 30s
- Verifica `System.currentTimeMillis() - peer.lastSeen > 90000`
- Remove peers inativos

### 10.2. Desconexão Graceful

```
1. Usuário clica "Desconectar"
2. ChatController.leave()
3. Envia LEAVE broadcast (3x com intervalo)
4. Para receiver thread
5. Para sender thread
6. Fecha socket
7. Limpa lista de peers
8. Notifica UI (status "Desconectado")
```

### 10.3. Reconexão Automática

- Se receiver thread morrer, tentar reiniciar após 5s
- Se socket falhar, recriar com exponential backoff
- Notificar usuário sobre reconexão automática

---

## 11. Escalabilidade e Limitações do UDP

### 11.1. Limitações do UDP Multicast

1. **TTL (Time To Live)**:

   - Limita propagação na rede
   - Padrão: 1 (mesma sub-rede)
   - Configurável: 1-255

2. **Tamanho máximo de pacote**:

   - Ethernet MTU: 1500 bytes
   - UDP payload: ~1472 bytes
   - Mensagens JSON devem ser < 1400 bytes

3. **Confiança**:

   - Sem garantia de entrega
   - Sem garantia de ordem
   - Pode duplicar

4. **Escalabilidade**:
   - Multicast não escala para WAN (Internet)
   - Limitado a LAN (mesma sub-rede)
   - Muitos peers (>50) causam storm de JOINs

### 11.2. Estratégias de Escalabilidade

**Decisão: Grupo pequeno (2-10 peers)** - Escalabilidade não é crítica

1. **Limitar broadcast de JOIN**:

   - Apenas 5-10 JOINs na inicialização (período de 10s)
   - Após estabilizado, apenas PING a cada 30s (muito menor overhead)
   - Adequado para grupos pequenos

2. **Filtragem na recepção**:

   - Ignorar mensagens de peers se a lista local > 20 (limite alto para 10 peers)
   - Sem histórico persistido, memória mínima

3. **Otimização de banda**:

   - Não necessário compressão para mensagens de chat típicas (< 200 chars)
   - Sem ACKs (best-effort), overhead mínimo

4. **Particionamento**:
   - Suporte a múltiplos grupos multicast (usuário pode trocar)
   - Cada grupo opera independentemente
   - Adequado para separar turmas ou tópicos

**Considerações de segurança**:

- **Decisão: Sem segurança** - Ambiente confiável (LAN)
- Sem autenticação ou criptografia
- Apenas para uso em redes locais controladas
- Mensagens em texto claro

---

## 12. Cronograma Estimado

### Fase 1: Fundação (3-4 dias)

- [x] Setup do projeto Maven
- [ ] Estrutura de pacotes
  - [ ] Testes
  - [ ] Coverage
  - [ ] Swing
- [ ] Implementar `Message` e `ChatMessage` (JSON)
- [ ] Implementar `AppConfig` (configurações)
- [ ] Criar `MainFrame` básica (layout vazio)

**Entregável**: Projeto compilável com estrutura base

### Fase 2: Rede UDP Básica (3-4 dias)

- [ ] `UDPNetworkManager` (singleton)
- [ ] `MulticastSender` (envio simples)
- [ ] `MulticastReceiver` (recepção simples)
- [ ] Teste de comunicação entre 2 instâncias
- [ ] Tratamento de exceções de rede

**Entregável**: Envio/recebimento de mensagens brutas

### Fase 3: Protocolo e Ordenação (3-5 dias)

- [ ] `MessageType` enum (CHAT, JOIN, LEAVE, PING, PONG)
- [ ] `ProtocolHandler` (processamento de tipos, filtragem)
- [ ] `ReorderBuffer` (ordenção por timestamp)
- [ ] Deduplicação (username + time)
- [ ] Testes de ordenação e perda

**Entregável**: Mensagens ordenadas e sem duplicação

### Fase 4: Descoberta de Peers (2-3 dias)

- [ ] `Peer` class
- [ ] `PeerDiscovery` (JOIN broadcast)
- [ ] Detecção de peers inativos (PING/PONG)
- [ ] Manutenção da lista de peers
- [ ] Teste com 3+ instâncias

**Entregável**: Lista de peers dinâmica

### Fase 5: Interface Gráfica (4-5 dias)

- [ ] `ChatPanel` (exibição de mensagens)
- [ ] `ConfigPanel` (configuração)
- [ ] `UsersListPanel` (lista de peers)
- [ ] Integração Controller ↔ View
- [ ] Atualização em tempo real (EDT)
- [ ] Estilização (cores, fonts)

**Entregável**: UI funcional completa

### Fase 6: Integração e Refinamento (3-4 dias)

- [ ] Integrar todas as camadas
- [ ] Teste de ponta-a-ponta
- [ ] Tratamento de erros (UI feedback)
- [ ] Logging
- [ ] Configuração persistente
- [ ] Limpeza de código

**Entregável**: Aplicação funcional completa

### Fase 7: Testes e Documentação (2-3 dias)

- [ ] Testes unitários (JUnit 5)
- [ ] Testes de integração
- [ ] Testes de carga (10+ peers)
- [ ] Documentação do usuário
- [ ] README.md
- [ ] Guia de execução

**Entregável**: Aplicação testada e documentada

### Cronograma Total

- **Duração estimada**: 20-28 dias
- **Caminho crítico**: Fase 2 → Fase 3 → Fase 5
- **Buffer**: 20% para imprevistos

---

### 13. Recursos Avançados

**Decisão: Apenas o básico** - Sem recursos adicionais

**Escopo mínimo viável**:

- [x] Chat multicast (todos recebem todas as mensagens)
- [x] Lista de peers com status (online/offline)
- [x] Configuração de username, grupo multicast, porta
- [x] Entrar/sair de grupos dinamicamente
- [x] Interface Swing responsiva

**Recursos explicitamente NÃO incluídos**:

- [ ] Mensagens privadas (unicast)
- [ ] Transferência de arquivos
- [ ] Emojis/rich text (apenas texto plano)
- [ ] Notificações sonoras
- [ ] Temas customizáveis
- [ ] Histórico persistente

**Justificativa**:

- Foco nos requisitos obrigatórios da especificação
- Simplicidade e robustez
- Cronograma realista (20-28 dias)
- Facilidade de testes e validação

---

## 14. Riscos e Mitigações

| Risco                                   | Probabilidade | Impacto | Mitigação                                        |
| --------------------------------------- | ------------- | ------- | ------------------------------------------------ |
| Perda de pacotes excessiva              | Alta          | Médio   | Implementar NACK + buffer                        |
| Deadlocks na UI                         | Média         | Alto    | Usar SwingUtilities.invokeLater()                |
| Memory leak (peers/histórico)           | Média         | Médio   | Limitar tamanho, LRU cleanup                     |
| Multicast não funciona em algumas redes | Alta          | Alto    | Testar em múltiplas redes, documentar requisitos |
| Muitos peers causam storm               | Média         | Médio   | Limitar JOIN broadcasts, throttle                |
| Threads morrem sem notificação          | Média         | Alto    | Watchdog thread, restart automático              |
| JSON malformado de outros clients       | Baixa         | Médio   | Try-catch, validar schema                        |

---

## 15. Considerações Finais

### 15.1. Decisões Tomadas

**Arquitetura e Tecnologia**:

1. Java 17 com Maven
2. Swing para GUI (leve, nativo)
3. Padrão MVC
4. Multicast UDP (não unicast)

**Protocolo e Comunicação**: 5. Sem handshake explícito - JOIN broadcast direto 6. Best-effort (tolerar perdas) - sem retransmissão 7. Sem ACKs para mensagens de chat 8. Ordenação por timestamp (time) + username 9. Deduplicação por username + time (tolerância 1s) 10. PING/PONG para detecção de peers inativos (30s/90s timeout)

**Modelo de Dados**: 11. Sem histórico persistente (apenas sessão atual) 12. Username duplicados permitidos (diferenciar por IP:porta) 13. Configurações salvas em properties file

**Segurança e Escala**: 14. Sem segurança (ambiente LAN confiável) 15. Grupo pequeno (2-10 peers) - foco em simplicidade 16. Sem recursos adicionais (apenas chat básico)

**Threading**: 17. Sender e Receiver em threads separadas 18. BlockingQueue para comunicação 19. SwingUtilities.invokeLater() para UI updates

### 15.2. Pontos de Atenção

1. **Thread safety**: Todas as coleções compartilhadas devem ser thread-safe
2. **EDT**: Nunca bloquear a Event Dispatch Thread
3. **Recursos**: Fechar sockets e parar threads no shutdown
4. **Configuração**: Validar entrada do usuário (porta válida 1024-65535, IP multicast 224.0.0.0-239.255.255.255)
5. **Multicast**: Testar em diferentes redes (algumas podem bloquear multicast)
6. **JSON**: Validar schema de todas as mensagens recebidas

### 15.3. Próximos Passos

1. ✅ Aprovar este planejamento
2. Inicializar estrutura de pacotes (Fase 1)
3. Implementar modelo de dados e JSON
4. Implementar camada de rede UDP multicast
5. Implementar protocolo (ordenção, deduplicação)
6. Implementar descoberta de peers
7. Implementar interface Swing
8. Integração e testes
9. Documentação final

### 15.4. Entregas por Fase

| Fase      | Entregável                              | Duração     |
| --------- | --------------------------------------- | ----------- |
| 1         | Projeto Maven + estrutura de pacotes    | 1 dia       |
| 2         | Envio/recebimento UDP básico            | 3 dias      |
| 3         | Protocolo (ordenção, tipos de mensagem) | 3 dias      |
| 4         | Descoberta de peers (JOIN/PING)         | 2 dias      |
| 5         | Interface Swing completa                | 4 dias      |
| 6         | Integração e refinamento                | 3 dias      |
| 7         | Testes e documentação                   | 2 dias      |
| **Total** | **Aplicação funcional**                 | **18 dias** |

\*Nota: Cronograma otimista. Buffer recomendado: 20% (4 dias) → **22 dias úteis**.

---

## Apêndices

### A. Referências

- Java Multicast API: `java.net.MulticastSocket`, `java.net.InetAddress`
- Swing: `javax.swing.*`, `java.awt.*`
- JSON: `com.fasterxml.jackson.databind.ObjectMapper` ou `com.google.gson.Gson`
- Concorrência: `java.util.concurrent.*`

### B. Comandos Maven Úteis

```bash
mvn clean compile          # Compilar
mvn exec:java              # Executar
mvn test                   # Testes
mvn package                # Criar JAR
```

### C. Portas e Grupos Multicast Sugeridos

- Porta padrão: `5000` (configurável)
- Grupo multicast: `224.0.0.1` (all hosts on subnet) ou `230.0.0.1`
- TTL: `1` (local network)

---

**Documento preparado para revisão e aprovação.**
