/**
 * Creates a handler for the PLAYER_ID_ASSIGNED message type
 *
 * @param {Object} lobbyManager - The lobby manager instance
 * @returns {Function} - The message handler function
 */
export function createPlayerIdAssignedHandler(lobbyManager) {
  return function handlePlayerIdAssigned(data) {
    // Store the player ID and information
    lobbyManager.playerState.currentPlayerId = data.playerId;
    lobbyManager.playerList.currentPlayerId = data.playerId;

    lobbyManager.savePlayerData();
    lobbyManager.addLogEntry(`Player ID assigned: ${data.playerId}`, 'success');
  };
}
