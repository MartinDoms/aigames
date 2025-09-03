import { Notification } from '../notification.js';
import { StorageService } from '../../services/storage-service.js';
import { LogService } from '../../services/log-service.js';
import { WebSocketService } from '../../services/websocket-service.js';
import { AvatarPicker } from './avatar-picker.js';
import { PlayerList } from './player-list.js';
import { VideoPlayer } from './video-player.js';
import { GameMap } from './map.js';
import { SoundEffectsService } from '../../services/sound-effects-service.js';
import { InitialSetup } from './initial-setup.js';
import { GameSettings } from './game-settings.js';
import { RoundScoreboard } from './round-scoreboard.js';
import { createGameStateService } from '../../services/game-state-service.js';

import { createMessageHandlers } from '../../services/message-handlers/index.js';

export const createLobbyManager = () => ({
  avatarPicker: AvatarPicker,
  playerList: PlayerList,
  notification: Notification,
  videoPlayer: VideoPlayer,
  map: GameMap,
  initialSetup: InitialSetup,
  gameSettings: GameSettings,
  roundScoreboard: RoundScoreboard,
  gameState: createGameStateService(VideoPlayer, GameMap),

  messageHandlers: null,

  playerState: {
    lobbyId: null,
    lobbyShortCode: null,
    currentPlayerId: null,
    playerName: 'Anonymous player',
    playerNameInput: 'Anonymous player',
    playerAvatar: 'avatar1',
  },

  reconnectAttempts: 0,
  maxReconnectAttempts: 5,

  showAvatarPicker: false,
  logsVersion: 0,

  startGame() {
    if (!this.isHost) {
      LogService.add('Only the host can start the game', 'error');
      return;
    }

    // Check if we're already in a game state
    if (this.gameState.lobbyState === 'GAME_IN_PROGRESS') {
      LogService.add(
        'Cannot start game: game is already in progress',
        'warning',
      );
      return;
    }

    if (!WebSocketService.isConnected()) {
      LogService.add('WebSocket is not connected! Cannot start game.', 'error');
      return;
    }

    // Include game settings in the message
    const message = {
      type: 'START_GAME',
      rounds: this.gameSettings.rounds,
      roundLength: this.gameSettings.roundLength,
      geoType: this.gameSettings.geoType,
    };

    try {
      LogService.add(
        `Sending start game message: ${JSON.stringify(message)}`,
        'info',
      );
      WebSocketService.send(message);
    } catch (error) {
      LogService.add(`Failed to send start game message: ${error}`, 'error');
      Notification.showNotification(
        'Connection error. Please refresh the page.',
        'error',
      );
    }
  },

  startNextRound() {
    if (!this.lobbyManager.isHost) {
      LogService.add('Only the host can start the next round', 'error');
    }

    if (!WebSocketService.isConnected()) {
      LogService.add(
        'WebSocket is not connected! Cannot start next round.',
        'error',
      );
      Notification.showNotification(
        'Connection to server lost. Please refresh the page.',
        'error',
      );
      return;
    }

    const message = {
      type: 'START_NEXT_ROUND',
      currentRoundId: this.gameState.currentRound.id,
    };

    try {
      LogService.add('Starting next round...', 'info');
      WebSocketService.send(message);
    } catch (error) {
      LogService.add(`Failed to start next round: ${error}`, 'error');
      Notification.showNotification(
        'Connection error. Please refresh the page.',
        'error',
      );
    }
  },

  returnToLobby() {
    LogService.add('Returning game to lobby', 'info');

    if (!this.isHost) {
      LogService.add('Only the host can return the game to lobby', 'error');
      return;
    }

    // Check if we're already in lobby state
    if (this.gameState.lobbyState === 'LOBBY') {
      LogService.add('Already in lobby state', 'warning');
      return;
    }

    if (!WebSocketService.isConnected()) {
      LogService.add(
        'WebSocket is not connected! Cannot return to lobby.',
        'error',
      );
      Notification.showNotification(
        'Connection to server lost. Please refresh the page.',
        'error',
      );
      return;
    }

    const message = {
      type: 'RETURN_TO_LOBBY',
    };

    try {
      LogService.add(
        `Sending return to lobby message: ${JSON.stringify(message)}`,
        'info',
      );
      WebSocketService.send(message);
      Notification.showNotification('Returning to lobby...', 'info');
    } catch (error) {
      LogService.add(
        `Failed to send return to lobby message: ${error}`,
        'error',
      );
      Notification.showNotification(
        'Connection error. Please refresh the page.',
        'error',
      );
    }
  },

  // Computed properties
  get logEntries() {
    this.logsVersion; // we access this to force Alpine to track it
    return LogService.getLogs();
  },

  get isHost() {
    const currentPlayerId = this.playerState.currentPlayerId;
    if (!currentPlayerId) return false;

    const player = this.playerList.players.find(
      (p) => p.id === currentPlayerId,
    );
    return player && player.host;
  },

  initLobby() {
    // Get the lobby ID from the data attribute on the body tag
    this.playerState.lobbyId = document.body.getAttribute('data-lobby-id');
    this.playerState.lobbyShortCode = document.body.getAttribute(
      'data-lobby-short-code',
    );

    // TODO find a better way - I don't want components to depend on lobby-manager
    this.roundScoreboard.lobbyManager = this;

    if (!this.playerState.lobbyId) {
      this.addLogEntry('Error: Could not find lobby ID', 'error');
      return;
    }

    if (!this.playerState.lobbyShortCode) {
      this.addLogEntry(
        'Warning: Could not find lobby short code, using ID instead',
        'warning',
      );
      this.playerState.lobbyShortCode = this.playerState.lobbyId;
    }

    this.addLogEntry(
      `Initializing lobby manager for lobby ${this.playerState.lobbyShortCode}`,
      'info',
    );

    // Initialize message handlers with a reference to this lobby manager
    this.messageHandlers = createMessageHandlers(this);

    // Set up global event listener for log updates
    document.addEventListener('logs-updated', () => {
      // Increment version to trigger A   lpine's reactivity system
      this.logsVersion++;

      // Handle scrolling after logs update
      this.$nextTick(() => {
        const logContainer = document.getElementById('logContainer');
        if (logContainer) {
          logContainer.scrollTop = logContainer.scrollHeight;
        }
      });
    });

    this.addLogEntry('Initializing lobby manager', 'info');

    // Set up cross-references between components
    // TODO this is hacky, find another way
    this.map.gameState = this.gameState;
    this.map.playerState = this.playerState;
    this.initialSetup.lobbyManager = this; // Set reference to lobby manager
    this.gameState.lobbyManager = this;

    // Load player data from storage
    const hasPlayerData = this.loadPlayerData();

    // Initialize the sound effects service
    SoundEffectsService.init();

    // Check if we need to show the setup modal
    if (hasPlayerData) {
      // Player data exists, connect to WebSocket
      this.setupWebSocket();
    } else {
      // No player data, show setup modal
      this.initialSetup.openSetupModal();
    }
  },

  loadPlayerData() {
    const lobbyShortCode = this.playerState.lobbyShortCode;
    const lobbyId = this.playerState.lobbyId;

    // Try to get player data using short code first
    this.playerState.currentPlayerId = StorageService.getItem(
      'playerId',
      lobbyShortCode,
    );

    // If not found, try with the legacy UUID (for backward compatibility)
    if (!this.playerState.currentPlayerId && lobbyShortCode !== lobbyId) {
      this.playerState.currentPlayerId = StorageService.getItem(
        'playerId',
        lobbyId,
      );
      // If found with UUID, migrate it to use short code
      if (this.playerState.currentPlayerId) {
        this.addLogEntry(
          'Migrating player data from UUID to short code storage',
          'info',
        );
        StorageService.setItem(
          'playerId',
          this.playerState.currentPlayerId,
          lobbyShortCode,
        );
      }
    }

    this.playerList.currentPlayerId = this.playerState.currentPlayerId;

    // Same approach for player name and avatar
    let savedName = StorageService.getItem('playerName', lobbyShortCode);
    let savedAvatar = StorageService.getItem('playerAvatar', lobbyShortCode);

    // Try legacy ID if not found
    if (!savedName && lobbyShortCode !== lobbyId) {
      savedName = StorageService.getItem('playerName', lobbyId);
      if (savedName) {
        StorageService.setItem('playerName', savedName, lobbyShortCode);
      }
    }

    if (!savedAvatar && lobbyShortCode !== lobbyId) {
      savedAvatar = StorageService.getItem('playerAvatar', lobbyId);
      if (savedAvatar) {
        StorageService.setItem('playerAvatar', savedAvatar, lobbyShortCode);
      }
    }

    if (savedName) {
      this.playerState.playerName = savedName;
      this.playerState.playerNameInput = savedName;
    }

    if (savedAvatar) {
      this.playerState.playerAvatar = savedAvatar;
      this.avatarPicker.selectedAvatarId = savedAvatar;
    }

    if (this.playerState.currentPlayerId) {
      this.addLogEntry(
        `Found player ID for this lobby: ${this.playerState.currentPlayerId}`,
        'info',
      );
      return true;
    } else {
      this.addLogEntry('No player data found. Will prompt for setup.', 'info');
      return false;
    }
  },

  kickPlayer(playerId) {
    if (!this.isHost) {
      this.addLogEntry('Only the host can kick players', 'error');
      return;
    }

    // Don't allow kicking oneself
    if (playerId === this.playerState.currentPlayerId) {
      this.addLogEntry('Cannot kick yourself', 'error');
      return;
    }

    // Find the player to get their name for the confirmation message
    const playerToKick = this.playerList.players.find((p) => p.id === playerId);
    if (!playerToKick) {
      this.addLogEntry(`Cannot find player with ID ${playerId}`, 'error');
      return;
    }

    // Confirmation before kicking
    if (
      !confirm(
        `Are you sure you want to kick ${playerToKick.name} from the lobby?`,
      )
    ) {
      return;
    }

    if (!WebSocketService.isConnected()) {
      this.addLogEntry(
        'WebSocket is not connected! Cannot kick player.',
        'error',
      );
      this.notification.showNotification(
        'Connection to server lost. Please refresh the page.',
        'error',
      );
      return;
    }

    const message = {
      type: 'KICK_PLAYER',
      playerId: playerId,
    };

    try {
      this.addLogEntry(
        `Sending kick player message for player ${playerId}`,
        'info',
      );
      WebSocketService.send(message);
      this.notification.showNotification(
        `Kicking player ${playerToKick.name}...`,
        'info',
      );
    } catch (error) {
      this.addLogEntry(`Failed to send kick player message: ${error}`, 'error');
      this.notification.showNotification(
        'Connection error. Please refresh the page.',
        'error',
      );
    }
  },

  handleKicked() {
    this.addLogEntry('You have been kicked from the lobby', 'error');
    this.notification.showNotification(
      'You have been kicked from the lobby by the host',
      'error',
    );

    // We'll maintain player identity data in local storage
    // This allows them to reconnect with the same identity if they choose to

    // Set a flag to show a reconnection option
    this.kickedFromLobby = true;

    // Redirect to the home page after a brief delay
    // or show a reconnect option
    setTimeout(() => {
      if (
        confirm('You were kicked from the lobby. Would you like to rejoin?')
      ) {
        // Reload the page to trigger reconnection
        window.location.reload();
      } else {
        // If they don't want to rejoin, redirect to home
        window.location.href = '/';
      }
    }, 3000);
  },

  // Update savePlayerData to use short code
  savePlayerData() {
    const lobbyShortCode = this.playerState.lobbyShortCode;

    if (this.playerState.currentPlayerId) {
      StorageService.setItem(
        'playerId',
        this.playerState.currentPlayerId,
        lobbyShortCode,
      );
    }

    StorageService.setItem(
      'playerName',
      this.playerState.playerName,
      lobbyShortCode,
    );
    StorageService.setItem(
      'playerAvatar',
      this.playerState.playerAvatar,
      lobbyShortCode,
    );
  },

  setupWebSocket() {
    this.addLogEntry('Connecting to WebSocket server...', 'info');

    const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${wsProtocol}//${window.location.host}/ws/lobbies/${this.playerState.lobbyId}`;

    this.addLogEntry(`WebSocket URL: ${wsUrl}`, 'info');

    // Set up WebSocket event handlers
    WebSocketService.onOpen = () => this.handleSocketOpen();
    WebSocketService.onMessage = (event) => this.handleSocketMessage(event);
    WebSocketService.onError = (error) => this.handleSocketError(error);
    WebSocketService.onClose = (event) => this.handleSocketClose(event);

    // Connect to socket
    WebSocketService.connect(wsUrl);
  },

  handleSocketOpen() {
    this.addLogEntry('WebSocket connection established', 'success');
    this.reconnectAttempts = 0;
    this.playerList.message = 'Connected, waiting for player data...';

    // Handle reconnection case - send reconnect message with stored player ID
    if (this.playerState.currentPlayerId) {
      this.addLogEntry(
        `Attempting to reconnect as player ${this.playerState.currentPlayerId}`,
        'info',
      );
      this.sendReconnectMessage();

      // If the player was previously kicked and is now reconnecting,
      // we should also send their name and avatar immediately
      if (this.kickedFromLobby) {
        this.addLogEntry(
          'Auto-updating player info after reconnection from kick',
          'info',
        );
        // Wait a short moment for reconnection to process
        setTimeout(() => {
          this.savePlayerInfo();
          // Reset the kick flag
          this.kickedFromLobby = false;
        }, 1000);
      }
    } else {
      // If this is a new player (no ID yet), automatically send the default name and avatar
      this.addLogEntry('Auto-joining lobby as new player', 'info');
      this.savePlayerInfo();
    }
  },

  handleSocketMessage(event) {
    this.addLogEntry(
      `Received message: ${event.data.substring(0, 350)}...`,
      'info',
    );

    try {
      const data = JSON.parse(event.data);

      // Use the message handlers instead of the switch statement
      const handler = this.messageHandlers[data.type];
      if (handler) {
        handler(data);
      } else {
        this.addLogEntry(`Unknown message type: ${data.type}`, 'warning');
      }
    } catch (error) {
      this.addLogEntry(`Error parsing message: ${error}`, 'error');
    }
  },

  handleSocketError(error) {
    this.addLogEntry(`WebSocket error: ${error}`, 'error');
  },

  handleSocketClose(event) {
    this.addLogEntry(
      `WebSocket connection closed: ${event.code} ${event.reason}`,
      'warning',
    );

    // Check if the close was due to being kicked (code 1001 with reason "Kicked by host")
    if (event.code === 1001 && event.reason === 'Kicked by host') {
      this.handleKicked();
      return; // Don't attempt to reconnect
    }

    // Attempt to reconnect if not a normal closure
    if (
      event.code !== 1000 &&
      this.reconnectAttempts < this.maxReconnectAttempts
    ) {
      this.reconnectAttempts++;
      const reconnectDelay = Math.min(
        1000 * Math.pow(2, this.reconnectAttempts),
        10000,
      );
      this.addLogEntry(
        `Attempting to reconnect in ${reconnectDelay / 1000}s (attempt ${this.reconnectAttempts})`,
        'info',
      );

      this.playerList.message = `Connection lost. Reconnecting in ${reconnectDelay / 1000} seconds...`;

      setTimeout(() => this.setupWebSocket(), reconnectDelay);
    } else if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      this.addLogEntry('Maximum reconnection attempts reached', 'error');
      this.playerList.message =
        'Connection failed. Please refresh the page to try again.';
    }
  },

  // Messaging methods
  sendReconnectMessage() {
    if (!WebSocketService.isConnected()) {
      this.addLogEntry(
        'WebSocket is not connected! Cannot send reconnect message.',
        'error',
      );
      return;
    }

    const message = {
      type: 'PLAYER_RECONNECT',
      playerId: this.playerState.currentPlayerId,
    };

    try {
      this.addLogEntry(
        `Sending player reconnect: ${JSON.stringify(message)}`,
        'info',
      );
      WebSocketService.send(message);
    } catch (error) {
      this.addLogEntry(`Failed to send reconnect message: ${error}`, 'error');
      this.notification.showNotification(
        'Connection error. Please refresh the page.',
        'error',
      );
    }
  },

  savePlayerInfo() {
    const name = this.playerState.playerNameInput.trim() || 'Anonymous player';

    // Apply the currently selected avatar for the server communication
    this.playerState.playerAvatar = this.avatarPicker.selectedAvatarId;

    if (!WebSocketService.isConnected()) {
      this.addLogEntry(
        'WebSocket is not connected! Cannot save player info.',
        'error',
      );
      this.notification.showNotification(
        'Connection to server lost. Please refresh the page.',
        'error',
      );
      return;
    }

    const message = {
      type: 'UPDATE_PLAYER',
      name: name,
      avatar: this.playerState.playerAvatar,
      playerId: this.playerState.currentPlayerId || null,
    };

    try {
      this.addLogEntry(
        `Sending player update: ${JSON.stringify(message)}`,
        'info',
      );
      WebSocketService.send(message);

      // Save to local storage
      this.savePlayerData();
    } catch (error) {
      this.addLogEntry(`Failed to send player update: ${error}`, 'error');
      this.notification.showNotification(
        'Connection error. Please refresh the page.',
        'error',
      );
    }
  },

  // Utility methods
  addLogEntry(message, type = 'info') {
    return LogService.add(message, type);
  },

  // Clipboard functionality
  copyLinkToClipboard() {
    // Construct the URL using short code instead of ID
    const baseUrl = window.location.origin;
    const url = `${baseUrl}/lobbies/${this.playerState.lobbyShortCode}`;

    this.addLogEntry('Copying invite link to clipboard', 'info');

    this.copyToClipboard(url)
      .then(() => {
        this.notification.showNotification(
          'Link copied to clipboard!',
          'success',
        );
        this.addLogEntry('Link copied successfully', 'success');
      })
      .catch((err) => {
        this.addLogEntry(`Could not copy text: ${err}`, 'error');
        this.notification.showNotification('Failed to copy link', 'error');
      });
  },

  copyToClipboard(text) {
    return navigator.clipboard.writeText(text);
  },

  isPlayerActive(playerId) {
    const player = this.playerList.players.find((p) => p.id === playerId);
    return player && player.active;
  },
});
