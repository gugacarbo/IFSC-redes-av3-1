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

- [ ] Message interface with methods: getDate(), getTime(), getUsername(), getContent(), getType(), getMsgId(), toJson()
- [ ] ChatMessage class implementing Message with fields: date (dd/MM/yyyy), time (HH:mm:ss), username, message, type, msgId (UUID)
- [ ] JsonUtils utility class for serialization/deserialization using Gson or Jackson
- [ ] fromJson() static method to parse JSON strings
- [ ] getUniqueKey() method returning msgId for deduplication
- [ ] Unit tests for JSON serialization/deserialization including msgId generation

### US-003: Configuration Management

**Description:** As a user, I want my configuration (username, multicast group, port, TTL) to be saved and loaded automatically so that I don't need to re-enter settings each time.

**Group:** A

**Acceptance Criteria:**

- [ ] AppConfig class with fields: username, multicastGroup, port, ttl (1-255), windowWidth, windowHeight, windowX, windowY
- [ ] load() method reads from ~/.udp_chat/config.properties in Java Properties format
- [ ] save() method writes to ~/.udp_chat/config.properties with keys: username, multicastGroup, port, ttl, windowWidth, windowHeight, windowX, windowY
- [ ] Default values: port=5000, multicastGroup=224.0.0.1, ttl=1
- [ ] Validation: port range 1024-65535, multicast IP 224.0.0.0-239.255.255.255, ttl range 1-255
- [ ] Unit tests for load/save operations including all fields

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
- [ ] BlockingQueue<Message> with maximum capacity of 1000 for pending messages
- [ ] sendMessage(Message msg) method adding to queue with overflow handling (discard oldest non-CHAT first, then CHAT if needed)
- [ ] run() loop consuming queue and sending via DatagramSocket
- [ ] Proper UTF-8 encoding for message bytes
- [ ] Retry logic only for control messages (JOIN, PING, ACK): max 3 attempts with exponential backoff; CHAT messages sent once (best-effort)
- [ ] Graceful shutdown with interrupt handling and queue drain
- [ ] Unit tests for queue behavior, overflow policy, and retry logic

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

**Description:** As a developer, I need a MessageType enum and ProtocolHandler to process different message types (CHAT, JOIN, LEAVE, PING, PONG, ACK) so that the protocol is properly implemented.

**Group:** C

**Acceptance Criteria:**

- [ ] MessageType enum: CHAT, JOIN, LEAVE, PING, PONG, ACK
- [ ] ProtocolHandler class with process(DatagramPacket) method
- [ ] JSON parsing and validation of incoming messages
- [ ] Message type routing:
  - CHAT → ChatController (for display)
  - JOIN/LEAVE → PeerDiscovery (update peer list)
  - PING/PONG → LivenessMonitor (update lastSeen)
  - ACK → PendingRequestTracker (match and clear pending requests)
- [ ] Self-message filtering (ignore messages from own username+address)
- [ ] Invalid JSON handling (log and discard)
- [ ] Unit tests for each message type processing including ACK handling

### US-008: Message Reordering Buffer

**Description:** As a user, I want messages to be displayed in chronological order even if they arrive out of sequence so that the chat conversation makes sense.

**Group:** C

**Acceptance Criteria:**

- [ ] ReorderBuffer class with PriorityQueue ordered by receive timestamp (System.currentTimeMillis() at reception)
- [ ] add(ChatMessage) method inserting into buffer with timestamp set at reception time
- [ ] deliver() method returning messages in chronological order
- [ ] Tolerance of 1 second for same-timestamp messages (use secondary sort by msgId)
- [ ] Maximum buffer size of 100 messages
- [ ] Maximum delay of 5 seconds (discard messages older than 5s from current time)
- [ ] Gradual overflow handling: when buffer reaches 100, remove 20 oldest messages (FIFO)
- [ ] Thread-safe implementation using appropriate concurrency controls
- [ ] Unit tests for ordering scenarios (out-of-order, duplicates, delays, overflow)

### US-009: Message Deduplication

**Description:** As a user, I don't want to see duplicate messages so that the chat remains clean and readable.

**Group:** C

**Acceptance Criteria:**

- [ ] DeduplicationCache class with LRU Set<String> (1000 entries)
- [ ] Key format: msgId (UUID from ChatMessage)
- [ ] isDuplicate(ChatMessage) method checking cache by msgId
- [ ] add(ChatMessage) method adding msgId to cache
- [ ] Automatic cleanup of entries older than 60 seconds
- [ ] Thread-safe implementation using ConcurrentHashMap or synchronized blocks
- [ ] Unit tests for deduplication logic including duplicate detection

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

- [ ] ChatSession class with fields: username, multicastGroup, port, ttl, connected
- [ ] List<Peer> for active peers (thread-safe)
- [ ] Deque<ChatMessage> for pending outbound messages
- [ ] join() method initiating connection, JOIN broadcast, and registering shutdown hook
- [ ] leave() method sending LEAVE, cleaning up, and removing shutdown hook
- [ ] send(String message) method creating ChatMessage with msgId and queuing
- [ ] broadcast() method for protocol messages (JOIN, LEAVE, PING, ACK)
- [ ] Thread-safe state management using proper synchronization
- [ ] Unit tests for session lifecycle including shutdown behavior

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
- [ ] JScrollPane with non-editable JTextArea (or JTextPane with styling) for message display
- [ ] JTextField for message input with 500 character limit
- [ ] JButton "Enviar" for sending messages
- [ ] Enter key triggers send action
- [ ] appendMessage(ChatMessage) method formatting and displaying with:
  - Own messages: background #E3F2FD, right-aligned
  - Others' messages: default background, left-aligned
  - System messages: "[Sistema] ..." in gray italic, centered
- [ ] Auto-scroll to bottom on new messages
- [ ] Message format: "[time] username: message" (configurable via ChatController)
- [ ] Clear button with confirmation dialog (JOptionPane.showConfirmDialog)
- [ ] Input field and send button disabled when disconnected (controlled by ChatController)
- [ ] Real-time updates via SwingUtilities.invokeLater()

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

**Description:** As a user, I want a configuration dialog to set my username, multicast group, port, and TTL so that I can customize my connection settings.

**Group:** E

**Acceptance Criteria:**

- [ ] ConfigPanel class extending JDialog with form layout
- [ ] Form fields: Username (JTextField), Multicast Group (JTextField), Port (JTextField), TTL (JTextField)
- [ ] Buttons: Salvar, Cancelar, Testar Conexão
- [ ] Input validation with error messages (username regex, port range, multicast IP format, TTL 1-255)
- [ ] Save button validates, updates AppConfig, and closes dialog
- [ ] Cancel button discards changes and closes
- [ ] Test button validates network settings by:
  - Creating a temporary MulticastSocket with current settings
  - Joining the multicast group
  - Sending a TEST message and waiting for echo (3s timeout)
  - Reporting success/failure to user
- [ ] Modal dialog behavior blocking MainFrame
- [ ] Fields pre-populated with current AppConfig values

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

### US-021: Protocol ACK Handling

**Description:** As a developer, I need ACK/PONG message handling and retry mechanism so that control messages are reliably delivered.

**Group:** C

**Acceptance Criteria:**

- [ ] MessageType.ACK added to enum
- [ ] ACK message format with fields: type=ACK, originalMsgId (UUID being acknowledged)
- [ ] ProtocolHandler processes ACK messages and matches to pending requests
- [ ] PendingRequestTracker class to track JOIN/PING requests with timestamps and retry counts
- [ ] Automatic retry: resend JOIN/PING every 5s up to 3 times if no ACK received
- [ ] Cleanup of expired pending requests (older than 15s)
- [ ] Thread-safe implementation for tracking pending requests
- [ ] Unit tests for ACK handling, retry logic, and timeout scenarios

### US-022: About Dialog

**Description:** As a user, I want to see application information so that I know the version and can access documentation.

**Group:** E

**Acceptance Criteria:**

- [ ] AboutDialog class extending JDialog
- [ ] Displays: Application name ("UDP Chat P2P"), version ("1.0.0"), description
- [ ] Shows license information (MIT or similar)
- [ ] OK button to close dialog
- [ ] Accessible from Help menu (Ajuda -> Sobre)
- [ ] Modal dialog behavior
- [ ] Simple, clean layout with optional icon

### US-023: Look and Feel Configuration

**Description:** As a user, I want the application to use the system's native look and feel so that it integrates with my desktop environment.

**Group:** E

**Acceptance Criteria:**

- [ ] MainFrame initialization sets system L&F: UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
- [ ] Fallback to cross-platform L&F if system L&F unavailable
- [ ] Catch and log exceptions during L&F setup
- [ ] All UI components use the configured L&F
- [ ] Document L&F choice in Technical Considerations

## Functional Requirements

- FR-1: The system must allow users to set a username, multicast group IP, port number, and TTL via a configuration dialog
- FR-2: The system must send and receive messages using UDP multicast on the configured group and port
- FR-3: The system must serialize all messages to JSON format with fields: type, date, time, username, message, msgId (UUID)
- FR-4: The system must support message types: CHAT, JOIN, LEAVE, PING, PONG, ACK
- FR-5: The system must display messages in chronological order based on receive timestamp
- FR-6: The system must filter duplicate messages using msgId (UUID) with LRU cache (1000 entries, 60s TTL)
- FR-7: The system must discover peers via JOIN broadcasts and maintain an active peer list
- FR-8: The system must detect inactive peers using PING/PONG with 90-second timeout
- FR-9: The system must update the UI in real-time using SwingUtilities.invokeLater()
- FR-10: The system must persist configuration to ~/.udp_chat/config.properties in Java Properties format
- FR-11: The system must use separate threads for sending and receiving messages
- FR-12: The system must handle graceful shutdown by sending LEAVE and closing sockets (with shutdown hook)
- FR-13: The system must implement ACK/PONG for control messages (JOIN, PING) with retry logic (max 3, 5s interval)
- FR-14: The MulticastSender must limit queue to 1000 messages with overflow policy (discard oldest non-CHAT first)
- FR-15: The system must provide a "Test Connection" feature to validate network settings
- FR-16: The ChatPanel must disable input fields and send button when disconnected
- FR-17: The system must display system messages for peer join/leave events
- FR-18: The system must style own messages differently (background #E3F2FD, right-aligned)
- FR-19: The system must confirm with user before clearing chat history
- FR-20: The system must use system Look and Feel for native appearance
- FR-21: The ReorderBuffer must implement gradual overflow handling (remove 20 oldest when reaching 100)
- FR-22: The system must allow duplicate usernames but distinguish peers by IP:port in UI

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
- No support for multiple multicast groups simultaneously (MVP: 1 group at a time)
- No system tray integration (minimize to taskbar only)

## Technical Considerations

- **Architecture:** MVC pattern with clear separation of concerns
- **Concurrency:** BlockingQueue for sender, ConcurrentHashMap for peers, CopyOnWriteArrayList for history
- **Threading:** Main thread (EDT), ChatController thread, MulticastSender thread, MulticastReceiver thread
- **JSON Library:** Gson or Jackson for serialization
- **Build Tool:** Maven with Java 17+ compiler settings
- **Testing:** JUnit 5 for unit and integration tests
- **Logging:** java.util.logging or SLF4J with rotating file handler
- **Network:** java.net.MulticastSocket, DatagramPacket, InetAddress
- **Code Documentation:** All public classes and methods must have Javadoc comments; 4-space indentation; braces on same line; package names lowercase, classes PascalCase; constants UPPER_SNAKE_CASE; max line length 120; always specify visibility modifiers

## Success Metrics

### Quantitative Performance Metrics

- **Message Latency:** < 500ms in LAN environment (end-to-end)
- **Throughput:** > 50 messages/second sustained
- **CPU Usage:** < 10% at idle, < 30% during active chat
- **Memory Usage:** < 100MB heap allocation
- **Startup Time:** < 3 seconds to display main window

### Quantitative Scalability Metrics

- **2 peers:** latency < 100ms
- **5 peers:** latency < 200ms
- **10 peers:** latency < 500ms, throughput > 30 msg/s

### Quantitative Reliability Metrics

- **Message Loss Rate:** < 5% in stable LAN
- **Session Uptime:** > 99% during active session
- **Recovery Time:** < 5 seconds after network failure

### Integration Test Criteria

- **Stress Test:** 100 messages sent and received without loss
- **Scale Test:** 10 peers connected simultaneously for 30 minutes
- **Concurrency:** No UI freezing during network operations

### Qualitative Success Criteria

- Application successfully connects to multicast group and exchanges messages between 2-10 instances
- Messages appear in correct chronological order across all peers
- No duplicate messages displayed in chat
- UI remains responsive during network operations (no EDT blocking)
- Peer list accurately reflects connected peers with <5 second update delay
- Configuration persists across application restarts
- All unit tests pass with >70% code coverage
- Application handles network errors gracefully without crashing

All questions have been answered and decisions documented below.

---

## Decisions Log

### D-001: Maximum Message Length

- **Decision:** 500 characters maximum for message content
- **Rationale:** Keeps JSON payload under 600 bytes (with ~100 bytes overhead), well below MTU of 1500 bytes
- **Implementation:** Add validation in `ValidationUtils.isValidMessageLength()` and reject in ChatPanel

### D-002: Unique Message Identifier (msgId)

- **Decision:** Add `msgId` field (UUID) to JSON message format
- **Rationale:** `username:time` is not globally unique - two users can send at the same second, or same user can send two messages
- **Implementation:** Generate UUID in ChatMessage constructor, update deduplication key to `msgId`

### D-003: Message Ordering Strategy

- **Decision:** Use receive timestamp for ordering, with tolerance window
- **Rationale:** Send timestamps are unreliable with desynchronized clocks
- **Implementation:** Use `System.currentTimeMillis()` at reception in ReorderBuffer

### D-004: ACK for JOIN Messages

- **Decision:** Implement ACK for JOIN messages (required), PING (optional)
- **Rationale:** JOIN is critical for peer discovery - without ACK, peer may not be added
- **Implementation:** Peers send PONG in response to JOIN, original peer re-sends if no PONG in 5s

### D-005: Automatic Reconnection

- **Decision:** No automatic reconnection (aligned with PRD Non-Goals)
- **Rationale:** Keep it simple, user manually reconnects
- **Implementation:** Remove reconnection logic from planning document

### D-006: Retry Logic

- **Decision:** Retry only for control messages (JOIN, PING), not for CHAT
- **Rationale:** CHAT messages are best-effort, control messages are critical
- **Implementation:** Max 3 retries for JOIN/PING, no retry for CHAT

### D-007: Package Structure

- **Decision:** Use planning document structure (more complete)
- **Rationale:** Planning includes necessary classes
- **Implementation:** Adopt classes: HandshakeManager, EventDispatcher, MessageDisplay, RoundedPanel, TimestampLabel (Packet class removed as unnecessary)

### D-008: Visual Distinction for Own Messages

- **Decision:** Yes - own messages with different background color
- **Rationale:** Better UX
- **Implementation:** Own messages: background #E3F2FD (light blue), right-aligned; others: default, left-aligned

### D-009: System Messages for Peer Events

- **Decision:** Yes - display system messages on join/leave
- **Rationale:** Better awareness
- **Implementation:** Add "[Sistema] username entrou no grupo" messages in ChatPanel

### D-010: Disable Input When Disconnected

- **Decision:** Yes - disable send button and input field when not connected
- **Rationale:** Prevent user frustration from failed sends
- **Implementation:** Toggle enabled state in ChatController.connect/disconnect

### D-011: TTL Configuration

- **Decision:** Add TTL field to AppConfig with default value 1
- **Rationale:** Allows advanced users to configure for routed networks
- **Implementation:** Add `ttl` field (int, 1-255) to AppConfig

### D-012: Username Validation

- **Decision:** Allow alphanumeric, underscore, hyphen, 3-20 characters
- **Rationale:** Simple validation prevents issues
- **Implementation:** Regex: `^[a-zA-Z0-9_-]{3,20}$`

### D-013: Buffer Overflow Behavior

- **Decision:** Gradual discard (FIFO) instead of clear()
- **Rationale:** Prevent mass message loss
- **Implementation:** Remove oldest 20 messages when buffer reaches 100

### D-014: Performance Metrics (Quantitative)

- **Decision:** Define quantitative metrics
- **Rationale:** Measurable success criteria
- **Implementation:** Add to Success Metrics section

### D-015: Integration Test Criteria

- **Decision:** Define quantitative criteria
- **Rationale:** Testable acceptance criteria
- **Implementation:** Add to US-019

### D-016: Multiple Multicast Groups

- **Decision:** MVP supports only 1 group at a time (future feature)
- **Rationale:** Aligns with requirements ("apenas 1 grupo por vez")
- **Implementation:** Update Non-Goals

### D-017: System Tray

- **Decision:** Minimize to taskbar only (future feature)
- **Rationale:** Simpler MVP
- **Implementation:** Update Non-Goals

### D-018: Protocol ACK Handling

- **Decision:** Implement explicit ACK/PONG for JOIN and PING messages
- **Rationale:** Ensure reliable peer discovery and liveness detection
- **Implementation:** Add ACK message type, track pending requests with timestamps, retry up to 3 times with 5s timeout

### D-019: Deduplication Key

- **Decision:** Use `msgId` (UUID) for deduplication instead of `username:time`
- **Rationale:** Globally unique identifier prevents false negatives
- **Implementation:** Update all references (FR-6, US-002, US-009) to use msgId

### D-020: Message Ordering Timestamp

- **Decision:** Use receive timestamp (System.currentTimeMillis()) for ordering
- **Rationale:** Send timestamps unreliable due to clock desynchronization
- **Implementation:** Update US-008 to specify receive-time ordering

### D-021: Retry Logic Scope

- **Decision:** Retry only for control messages (JOIN, PING, ACK), not CHAT
- **Rationale:** CHAT is best-effort, control messages are critical for protocol
- **Implementation:** Update US-005 to conditionally retry based on message type

### D-022: Configuration File Format

- **Decision:** Use Java Properties format with specific keys
- **Rationale:** Standard, simple, human-editable
- **Implementation:** Define keys: username, multicastGroup, port, ttl, windowWidth, windowHeight, windowX, windowY

### D-023: Shutdown Hook

- **Decision:** Register shutdown hook to ensure LEAVE is sent on SIGTERM
- **Rationale:** Graceful cleanup even on forced termination
- **Implementation:** Add to Main.java or ChatSession.start()

### D-024: Duplicate Username Handling

- **Decision:** Allow duplicates but distinguish by IP:port in UI
- **Rationale:** Simpler conflict resolution, no central authority
- **Implementation:** Peer display: "username (IP:port)", unique ID remains IP:port

### D-025: Sender Queue Size Limit

- **Decision:** Set maximum queue size of 1000 messages with discard policy
- **Rationale:** Prevent memory exhaustion if network is down
- **Implementation:** When full, discard oldest non-CHAT messages first, then CHAT if needed

### D-026: Test Connection Feature

- **Decision:** Test creates socket, joins group, sends/receives test packet
- **Rationale:** Validates all network parameters work
- **Implementation:** Send special TEST message, wait for echo with 3s timeout

### D-027: UI Enhancements

- **Decision:** Implement own-message styling, system messages, input disabling
- **Rationale:** Better UX and clarity
- **Implementation:**
  - Own messages: background #E3F2FD, right-aligned
  - System messages: "[Sistema] ..." in gray italic
  - Disable input when disconnected

### D-028: Buffer Overflow Handling

- **Decision:** Gradual FIFO discard (20 oldest) when reaching 100
- **Rationale:** Prevents mass loss while controlling memory
- **Implementation:** Check size before add, removeFirst() if needed

### D-029: Look and Feel

- **Decision:** Use system L&F for native appearance
- **Rationale:** Better user experience on different platforms
- **Implementation:** UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

### D-030: Clear Chat Confirmation

- **Decision:** Show confirmation dialog before clearing chat
- **Rationale:** Prevent accidental data loss
- **Implementation:** JOptionPane.showConfirmDialog with "Tem certeza?"

### D-031: Packet Class

- **Decision:** Remove Packet class reference - use DatagramPacket directly
- **Rationale:** Unnecessary abstraction adds complexity
- **Implementation:** Remove from D-007 and all future references

### D-032: ChatController Naming

- **Decision:** Use ChatController consistently (not UIController)
- **Rationale:** Clearer, more descriptive
- **Implementation:** Update all references to use ChatController

### D-033: About Dialog Content

- **Decision:** Show app name, version, description, license
- **Rationale:** Standard about dialog information
- **Implementation:** Simple JDialog with labels and OK button

### D-034: Code Documentation

- **Decision:** Require Javadoc for all public classes and methods
- **Rationale:** Maintainability and API clarity
- **Implementation:** Add to Technical Considerations, enforce in code reviews

### D-035: Test Data and Seeds

- **Decision:** Create test scenarios with predefined message sequences
- **Rationale:** Repeatable, reliable testing
- **Implementation:** Add test data generators in test sources

### D-036: Performance Testing Plan

- **Decision:** Create separate performance test suite with benchmarks
- **Rationale:** Validate success metrics quantitatively
- **Implementation:** Use JMH or custom timing tests, document baseline results

## Additional Sections

### Deployment and Packaging

- Build executable JAR with dependencies using Maven Shade Plugin
- Include startup scripts for different platforms
- Document manual build steps in README.md
- Target: Single executable JAR with all dependencies bundled

### Code Standards

- All public APIs must have Javadoc comments
- Use 4-space indentation, braces on same line
- Package names in lowercase, classes in PascalCase
- Constants in UPPER_SNAKE_CASE
- Maximum line length: 120 characters
- No raw types, use generics where applicable
- Always specify visibility modifiers (private/protected/public)

### Test Plan

- Unit tests: All model and utility classes
- Integration tests: UDP communication, peer discovery, message ordering
- Stress tests: 100+ messages, 10+ peers
- Performance tests: Measure latency, throughput, memory
- Negative tests: Invalid config, network failures, malformed messages
- Seed data: Predefined scenarios for repeatable testing
