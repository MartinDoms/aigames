/**
 * Creates a handler for the PLAYER_JOINED message type
 *
 * @param {Object} lobbyManager - The lobby manager instance
 * @returns {Function} - The message handler function
 */
export function createPlayerJoinedHandler(lobbyManager) {
  return function handlePlayerJoined(data) {
    lobbyManager.addLogEntry(
      `Player joined: ${data.player ? data.player.name : 'unknown'}`,
      'success',
    );
    if (data.player) {
      lobbyManager.playerList.addNewPlayer(data.player);
      lobbyManager.notification.showNotification(
        `${data.player.name} joined the lobby`,
        'success',
      );
    }
  };
}
