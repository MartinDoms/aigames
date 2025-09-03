/**
 * Creates a handler for the PLAYER_UPDATED message type
 *
 * @param {Object} lobbyManager - The lobby manager instance
 * @returns {Function} - The message handler function
 */
export function createPlayerUpdatedHandler(lobbyManager) {
  return function handlePlayerUpdated(data) {
    lobbyManager.addLogEntry(
      `Player updated: ${data.player ? data.player.name : 'unknown'}`,
      'info',
    );
    if (data.player) {
      lobbyManager.playerList.updateSinglePlayer(data.player);

      // Update notification message based on what was changed
      const changes = [];
      if (data.changedFields && data.changedFields.includes('name'))
        changes.push('name');
      if (data.changedFields && data.changedFields.includes('avatar'))
        changes.push('avatar');

      const changeMessage =
        changes.length > 0
          ? `updated their ${changes.join(' and ')}`
          : 'updated their profile';

      lobbyManager.notification.showNotification(
        `${data.player.name} ${changeMessage}`,
        'info',
      );
    }
  };
}
