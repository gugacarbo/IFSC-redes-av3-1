# PRD: UDP Chat P2P Application

## Introduction

Build a desktop chat application in Java using UDP multicast peer-to-peer communication with a Swing GUI. The application enables multiple users to exchange messages in real-time within a local network without a central server, following an MVC architecture pattern.

## Goals

- Enable real-time text chat between multiple peers on a local network
- Provide a responsive Swing-based graphical interface
- Implement reliable UDP multicast communication with message ordering and deduplication
- Support dynamic peer discovery and group management
- Maintain a clean, maintainable codebase following MVC architecture

## User Stories

### US-001: Maven Project Setup and Package Structure
**Description:** As a developer, I need a properly configured Maven project with the correct package structure so that the codebase is organized and buildable.

**Group:** A

**Acceptance Criteria:**
- [ ] pom.xml configured with Java 17+ and required dependencies (JSON library, JUnit 5)
- [ ] Package structure created: chat/config, chat/model, chat/network, chat/network/protocol, chat/controller, chat/view, chat/view/components, chat/util, chat/exception
- [ ] Main.java class created with basic main method
- [ ] Project compiles successfully with `mvn clean compile`
- [ ] .gitignore configured for Maven/Java projects

### US-002: Message Model and JSON Serialization
**Description:** As a developer, I need a Message interface and ChatMessage implementation with JSON serialization so that messages can be transmitted over the network.

**Group:** A

**Acceptance Criteria:**
- [ ] Message interface with methods: getDate(), getTime(), getUsername(), getContent(), getType(), toJson()
- [ ] ChatMessage class implementing Message with fields: date (dd/MM/yyyy), time (HH:mm:ss), username, message, type
- [ ] JsonUtils utility class for serialization/deserialization using Gson or Jackson
- [ ] fromJson() static method to parse JSON strings
- [ ] getUniqueKey() method returning "username:time" for deduplication
- [ ] Unit tests for JSON serialization/deserialization

### US-003: Configuration Management
**Description:** As a user, I want my configuration (username, multicast group, port) to be saved and loaded automatically so that I don't need to re-enter settings each time.

**Group:** A

**Acceptance Criteria:**
- [ ] AppConfig class with fields: username, multicastGroup, port, windowWidth, windowHeight, windowX, windowY
- [ ] load() method reads from ~/.udp_chat/config.properties
- [ ] save() method writes to ~/.udp_chat/config.properties
- [ ] Default values: port=5000, multicastGroup=224.0.0.1
- [ ] Validation: port range 1024-65535, multicast IP 224.0.0.0-239.255.255.255
- [ ] Unit tests for load/save operations

### US-004: UDP Network Manager (Singleton)
**Description:** As a developer, I need a singleton UDPNetworkManager to manage the MulticastSocket lifecycle so that network resources are properly controlled.

**Group:** B

**Acceptance Criteria:**
- [ ] UDPNetworkManager class with singleton pattern (getInstance())
- [ ] createSocket(int port, String multicastGroup) method
- [ ] getSocket() method returning the active MulticastSocket
- [ ] shutdown() method closing socket and cleaning resources
- [ ] Thread-safe implementation using synchronized or atomic references
- [ ] Proper exception handling for socket creation failures
- [ ] Unit tests for singleton behavior and socket lifecycle

### US-005: Multicast Sender Thread
**Description:** As a user, I want to send messages asynchronously so that the UI remains responsive while messages are being transmitted.

**Group:** B

**Acceptance Criteria:**
- [ ] MulticastSender class extending Thread
- [ ] BlockingQueue<Message> for pending messages
- [ ] sendMessage(Message msg) method adding to queue
- [ ] run() loop consuming queue and sending via DatagramSocket
- [ ] Proper UTF-8 encoding for message bytes
- [ ] IOException handling with retry logic (max 3 attempts)
- [ ] Graceful shutdown with interrupt handling
- [ ] Unit tests for queue behavior and send operations

### US-006: Multicast Receiver Thread
**Description:** As a user, I want to receive messages from other peers automatically so that I can see incoming chat messages in real-time.

**Group:** B

**Acceptance Criteria:**
- [ ] MulticastReceiver class extending Thread
- [ ] run() loop calling socket.receive() continuously
- [ ] DatagramPacket parsing with UTF-8 decoding
- [ ] Filtering of self-sent messages (compare source address)
- [ ] Delegation to ProtocolHandler for message processing
- [ ] Proper exception handling for socket timeouts and errors
- [ ] Graceful shutdown with interrupt handling
- [ ] Unit tests for receive loop and message parsing

### US-007: Message Type Enum and Protocol Handler
**Description:** As a developer, I need a MessageType enum and ProtocolHandler to process different message types (CHAT, JOIN, LEAVE, PING, PONG) so that the protocol is properly implemented.

**Group:** C

**Acceptance Criteria:**
- [ ] MessageType enum: CHAT, JOIN, LEAVE, PING, PONG
- [ ] ProtocolHandler class with process(DatagramPacket) method
- [ ] JSON parsing and validation of incoming messages
- [ ] Message type routing (CHAT→ChatController, JOIN/LEAVE→PeerDiscovery, PING/PONG→liveness)
- [ ] Self-message filtering (ignore messages from own username+address)
- [ ] Invalid JSON handling (log and discard)
- [ ] Unit tests for each message type processing

### US-008: Message Reordering Buffer
**Description:** As a user, I want messages to be displayed in chronological order even if they arrive out of sequence so that the chat conversation makes sense.

**Group:** C

**Acceptance Criteria:**
- [ ] ReorderBuffer class with PriorityQueue ordered by time field
- [ ] add(ChatMessage) method inserting into buffer
- [ ] deliver() method returning messages in chronological order
- [ ] Tolerance of 1 second for same-timestamp messages
- [ ] Maximum buffer size of 100 messages
- [ ] Maximum delay of 5 seconds (discard older messages)
- [ ] Thread-safe implementation
- [ ] Unit tests for ordering scenarios (out-of-order, duplicates, delays)

### US-009: Message Deduplication
**Description:** As a user, I don't want to see duplicate messages so that the chat remains clean and readable.

**Group:** C

**Acceptance Criteria:**
- [ ] DeduplicationCache class with LRU Set<String> (1000 entries)
- [ ] Key format: "username:time"
- [ ] isDuplicate(ChatMessage) method checking cache
- [ ] add(ChatMessage) method adding to cache
- [ ] Automatic cleanup of entries older than 60 seconds
- [ ] 1-second tolerance for same-user messages
- [ ] Thread-safe implementation
- [ ] Unit tests for deduplication logic

### US-010: Peer Model and Peer Discovery
**Description:** As a user, I want to see which peers are currently online in my chat group so that I know who I'm communicating with.

**Group:** D

**Acceptance Criteria:**
- [ ] Peer class with fields: username, address (InetAddress), port, lastSeen (timestamp), isActive
- [ ] getUniqueId() method returning "IP:port"
- [ ] equals() and hashCode() based on uniqueId
- [ ] PeerDiscovery class managing peer list
- [ ] JOIN broadcast on connection (every 2s for 10s)
- [ ] PING broadcast every 30s for liveness
- [ ] Peer timeout detection (90s without PONG)
- [ ] ConcurrentHashMap for thread-safe peer list
- [ ] Unit tests for peer lifecycle and discovery

### US-011: Chat Session Management
**Description:** As a user, I want to join and leave chat groups dynamically so that I can participate in different conversations.

**Group:** D

**Acceptance Criteria:**
- [ ] ChatSession class with fields: username, multicastGroup, port, connected
- [ ] List<Peer> for active peers
- [ ] Deque<ChatMessage> for pending outbound messages
- [ ] join() method initiating connection and JOIN broadcast
- [ ] leave() method sending LEAVE and cleaning up
- [ ] send(String message) method creating ChatMessage and queuing
- [ ] broadcast() method for protocol messages (JOIN, LEAVE, PING)
- [ ] Thread-safe state management
- [ ] Unit tests for session lifecycle

### US-012: Main Frame Window
**Description:** As a user, I want a main application window with menu bar, toolbar, and status bar so that I have a complete desktop application interface.

**Group:** E

**Acceptance Criteria:**
- [ ] MainFrame class extending JFrame
- [ ] BorderLayout with MenuBar (Arquivo, Configurações, Ajuda)
- [ ] ToolBar with buttons: Conectar, Desconectar, Limpar
- [ ] JSplitPane (horizontal) for ChatPanel and UsersListPanel
- [ ] StatusBar showing connection status and group info
- [ ] Window close handler with proper cleanup
- [ ] Save/restore window position and size from config
- [ ] Minimum window size: 600x400

### US-013: Chat Panel
**Description:** As a user, I want a chat panel where I can see messages and type new ones so that I can participate in conversations.

**Group:** E

**Acceptance Criteria:**
- [ ] ChatPanel class extending JPanel with BorderLayout
- [ ] JScrollPane with non-editable JTextArea for message display
- [ ] JTextField for message input
- [ ] JButton "Enviar" for sending messages
- [ ] Enter key triggers send action
- [ ] appendMessage(ChatMessage) method formatting and displaying
- [ ] Auto-scroll to bottom on new messages
- [ ] Message format: "[time] username: message"
- [ ] Clear button functionality

### US-014: Users List Panel
**Description:** As a user, I want to see a list of connected peers with their status so that I know who is online.

**Group:** E

**Acceptance Criteria:**
- [ ] UsersListPanel class extending JPanel with BoxLayout
- [ ] JList<Peer> with DefaultListModel
- [ ] Custom CellRenderer showing "username (IP:port)" format
- [ ] Visual indicator for active/inactive peers
- [ ] addPeer(Peer) method
- [ ] removePeer(Peer) method
- [ ] updatePeerStatus(Peer) method
- [ ] Auto-refresh on peer list changes

### US-015: Configuration Dialog
**Description:** As a user, I want a configuration dialog to set my username, multicast group, and port so that I can customize my connection settings.

**Group:** E

**Acceptance Criteria:**
- [ ] ConfigPanel class extending JDialog
- [ ] Form fields: Username (JTextField), Multicast Group (JTextField), Port (JTextField)
- [ ] Buttons: Salvar, Cancelar, Testar Conexão
- [ ] Input validation with error messages
- [ ] Save button updates AppConfig and closes dialog
- [ ] Cancel button discards changes and closes
- [ ] Test button validates network settings
- [ ] Modal dialog behavior

### US-016: Controller Integration
**Description:** As a developer, I need a ChatController to coordinate between the UI and network layers so that user actions are properly handled.

**Group:** F

**Acceptance Criteria:**
- [ ] ChatController class managing ChatSession and UI interaction
- [ ] MessageListener interface for UI notifications
- [ ] PeerListener interface for peer list updates
- [ ] sendMessage(String text) method for user input
- [ ] connect() method initiating session
- [ ] disconnect() method ending session
- [ ] SwingUtilities.invokeLater() for all UI updates
- [ ] Event routing from Receiver to UI via Observer pattern
- [ ] Integration tests for controller flow

### US-017: Exception Handling and Logging
**Description:** As a developer, I need proper exception handling and logging so that errors are diagnosable and the application is robust.

**Group:** F

**Acceptance Criteria:**
- [ ] NetworkException for socket/network errors
- [ ] InvalidMessageException for malformed JSON
- [ ] ConfigurationException for invalid settings
- [ ] Logger utility class with INFO/DEBUG levels
- [ ] Rotating log file: logs/chat_yyyy-MM-dd.log
- [ ] 7-day log retention
- [ ] Try-catch blocks in all network operations
- [ ] User-friendly error messages in UI

### US-018: Utility Classes
**Description:** As a developer, I need utility classes for common operations so that code is reusable and maintainable.

**Group:** F

**Acceptance Criteria:**
- [ ] DateFormatter for dd/MM/yyyy and HH:mm:ss formatting
- [ ] ThreadSafeList wrapper for concurrent list operations
- [ ] ValidationUtils for port, IP, username validation
- [ ] Unit tests for all utility methods

### US-019: End-to-End Integration
**Description:** As a user, I want the complete application to work seamlessly so that I can chat with peers on my network.

**Group:** G

**Acceptance Criteria:**
- [ ] Application starts and displays MainFrame
- [ ] Configuration dialog opens and saves settings
- [ ] Connect button initiates UDP multicast session
- [ ] Messages sent appear in chat area
- [ ] Messages from other peers appear in real-time
- [ ] Users list updates when peers join/leave
- [ ] Disconnect button properly closes session
- [ ] Multiple instances (2-10) can communicate simultaneously
- [ ] No UI freezing during network operations
- [ ] Graceful error handling for network failures

### US-020: Unit and Integration Tests
**Description:** As a developer, I need comprehensive tests so that the application is reliable and maintainable.

**Group:** G

**Acceptance Criteria:**
- [ ] JUnit 5 test classes for all model classes
- [ ] Unit tests for ProtocolHandler, ReorderBuffer, DeduplicationCache
- [ ] Unit tests for JsonUtils, ValidationUtils
- [ ] Integration tests for UDP send/receive
- [ ] Integration tests for peer discovery
- [ ] Test coverage > 70% for core classes
- [ ] All tests pass with `mvn test`

## Functional Requirements

- FR-1: The system must allow users to set a username, multicast group IP, and port number via a configuration dialog
- FR-2: The system must send and receive messages using UDP multicast on the configured group and port
- FR-3: The system must serialize all messages to JSON format with fields: type, date, time, username, message
- FR-4: The system must support message types: CHAT, JOIN, LEAVE, PING, PONG
- FR-5: The system must display messages in chronological order based on the time field
- FR-6: The system must filter duplicate messages using username:time combination
- FR-7: The system must discover peers via JOIN broadcasts and maintain an active peer list
- FR-8: The system must detect inactive peers using PING/PONG with 90-second timeout
- FR-9: The system must update the UI in real-time using SwingUtilities.invokeLater()
- FR-10: The system must persist configuration to ~/.udp_chat/config.properties
- FR-11: The system must use separate threads for sending and receiving messages
- FR-12: The system must handle graceful shutdown by sending LEAVE and closing sockets

## Non-Goals

- No private messaging (unicast) between specific peers
- No file transfer capabilities
- No rich text or emoji support (plain text only)
- No sound notifications
- No customizable themes
- No persistent message history (session-only)
- No encryption or authentication (trusted LAN environment)
- No support for WAN/Internet (LAN only)
- No automatic reconnection after network failure

## Technical Considerations

- **Architecture:** MVC pattern with clear separation of concerns
- **Concurrency:** BlockingQueue for sender, ConcurrentHashMap for peers, CopyOnWriteArrayList for history
- **Threading:** Main thread (EDT), ChatController thread, MulticastSender thread, MulticastReceiver thread
- **JSON Library:** Gson or Jackson for serialization
- **Build Tool:** Maven with Java 17+ compiler settings
- **Testing:** JUnit 5 for unit and integration tests
- **Logging:** java.util.logging or SLF4J with rotating file handler
- **Network:** java.net.MulticastSocket, DatagramPacket, InetAddress

## Success Metrics

- Application successfully connects to multicast group and exchanges messages between 2-10 instances
- Messages appear in correct chronological order across all peers
- No duplicate messages displayed in chat
- UI remains responsive during network operations (no EDT blocking)
- Peer list accurately reflects connected peers with <5 second update delay
- Configuration persists across application restarts
- All unit tests pass with >70% code coverage
- Application handles network errors gracefully without crashing

## Open Questions

- Should the application support multiple simultaneous multicast groups?
- What should be the maximum message length?
- Should there be a visual distinction between own messages and others' messages?
- Should the application minimize to system tray or taskbar?
