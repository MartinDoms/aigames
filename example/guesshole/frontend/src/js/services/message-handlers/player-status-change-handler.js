/**
 * Creates a handler for the PLAYER_STATUS_CHANGE message type
 *
 * @param {Object} lobbyManager - The lobby manager instance
 * @returns {Function} - The message handler function
 */
export function createPlayerStatusChangeHandler(lobbyManager) {
  return function handlePlayerStatusChange(data) {
    const player = lobbyManager.playerList.changePlayerStatus(
      data.playerId,
      data.active,
    );

    lobbyManager.addLogEntry(
      `Player ${data.playerId} status changed to ${data.active ? 'active' : 'inactive'}`,
      'info',
    );

    if (data.playerId !== lobbyManager.playerState.currentPlayerId) {
      lobbyManager.notification.showNotification(
        `${player.name} is now ${data.active ? 'online' : 'offline'}`,
        data.active ? 'success' : 'warning',
      );
    }
  };
}
