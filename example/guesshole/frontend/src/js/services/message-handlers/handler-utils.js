/**
 * Logs a message being received
 *
 * @param {Object} lobbyManager - The lobby manager instance
 * @param {string} messageType - The type of message received
 * @param {Object} data - The message data
 * @param {string} [logLevel='info'] - The log level
 */
export function logMessageReceived(
  lobbyManager,
  messageType,
  data,
  logLevel = 'info',
) {
  let logMessage = `Received ${messageType} message`;

  // Add context for certain message types
  if (messageType === 'PLAYERS_UPDATE') {
    logMessage += ` with ${data.players ? data.players.length : 0} players`;
  } else if (messageType === 'PLAYER_UPDATED' && data.player) {
    logMessage += `: ${data.player.name}`;
  } else if (messageType === 'PLAYER_JOINED' && data.player) {
    logMessage += `: ${data.player.name}`;
  } else if (messageType === 'PLAYER_STATUS_CHANGE') {
    logMessage += ` for player ${data.playerId}: ${data.active ? 'active' : 'inactive'}`;
  } else if (messageType === 'GUESS_RESULT') {
    logMessage += ` for round ${data.roundId}`;
  }

  lobbyManager.addLogEntry(logMessage, logLevel);
}

/**
 * Shows a notification to the user
 *
 * @param {Object} lobbyManager - The lobby manager instance
 * @param {string} message - The notification message
 * @param {string} [type='info'] - The notification type
 * @param {number} [duration=5000] - Duration in milliseconds
 */
export function showNotification(
  lobbyManager,
  message,
  type = 'info',
  duration = 5000,
) {
  lobbyManager.notification.showNotification(message, type, duration);
}

/**
 * Get a player from the player list by ID
 *
 * @param {Object} lobbyManager - The lobby manager instance
 * @param {string} playerId - The player ID to find
 * @returns {Object|null} - The player object or null if not found
 */
export function getPlayerById(lobbyManager, playerId) {
  return lobbyManager.playerList.players.find((p) => p.id === playerId) || null;
}

/**
 * Checks if the player is the current user
 *
 * @param {Object} lobbyManager - The lobby manager instance
 * @param {string} playerId - The player ID to check
 * @returns {boolean} - True if this is the current user
 */
export function isCurrentPlayer(lobbyManager, playerId) {
  return playerId === lobbyManager.playerState.currentPlayerId;
}
