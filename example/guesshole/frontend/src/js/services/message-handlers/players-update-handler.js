/**
 * Creates a handler for the PLAYERS_UPDATE message type
 *
 * @param {Object} lobbyManager - The lobby manager instance
 * @returns {Function} - The message handler function
 */
export function createPlayersUpdateHandler(lobbyManager) {
  return function handlePlayersUpdate(data) {
    lobbyManager.addLogEntry(
      `Received players list with ${data.players ? data.players.length : 0} players`,
      'info',
    );
    lobbyManager.playerList.updatePlayerList(data.players || []);

    // Update current player info input if current player is in the list
    // This is just a safety in case we're out of sync
    const currentPlayer = data.players.find(
      (p) => p.id === lobbyManager.playerState.currentPlayerId,
    );
    if (currentPlayer) {
      if (lobbyManager.playerState.playerName !== currentPlayer.name) {
        lobbyManager.playerState.playerName = currentPlayer.name;
        lobbyManager.playerState.playerNameInput = currentPlayer.name;
      }

      if (currentPlayer.avatar) {
        lobbyManager.playerState.playerAvatar = currentPlayer.avatar;
        lobbyManager.avatarPicker.selectedAvatarId = currentPlayer.avatar;
      }

      // Also update stored info
      lobbyManager.savePlayerData();
    }
  };
}
