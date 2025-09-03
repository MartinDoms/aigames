// Import all individual message handlers
import { createPlayersUpdateHandler } from './players-update-handler.js';
import { createPlayerUpdatedHandler } from './player-updated-handler.js';
import { createPlayerJoinedHandler } from './player-joined-handler.js';
import { createPlayerStatusChangeHandler } from './player-status-change-handler.js';
import { createHeartbeatHandler } from './heartbeat-handler.js';
import { createPlayerIdAssignedHandler } from './player-id-assigned-handler.js';
import { createGameStateHandler } from './game-state-handler.js';
import { createGuessResultHandler } from './guess-result-handler.js';
import { createPlayerGuessHandler } from './player-guess-handler.js';

/**
 * Creates and returns a map of message types to their corresponding handler functions
 * Each handler is initialized with a reference to the lobby manager
 *
 * @param {Object} lobbyManager - The lobby manager instance
 * @returns {Object} - Map of message types to handler functions
 */
export function createMessageHandlers(lobbyManager) {
  return {
    PLAYERS_UPDATE: createPlayersUpdateHandler(lobbyManager),
    PLAYER_UPDATED: createPlayerUpdatedHandler(lobbyManager),
    PLAYER_JOINED: createPlayerJoinedHandler(lobbyManager),
    PLAYER_STATUS_CHANGE: createPlayerStatusChangeHandler(lobbyManager),
    HEARTBEAT: createHeartbeatHandler(lobbyManager),
    PLAYER_ID_ASSIGNED: createPlayerIdAssignedHandler(lobbyManager),
    GAME_STATE: createGameStateHandler(
      lobbyManager,
      lobbyManager.roundScoreboard,
      lobbyManager.gameState,
      lobbyManager.playerList,
    ),
    GUESS_RESULT: createGuessResultHandler(
      lobbyManager,
      lobbyManager.gameState,
      lobbyManager.playerState,
    ),
  };
}
