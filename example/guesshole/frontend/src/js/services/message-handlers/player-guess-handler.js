import { SoundEffectsService } from '../../services/sound-effects-service.js';

/**
 * Creates a handler for the PLAYER_GUESS message type
 *
 * @param {Object} lobbyManager - The lobby manager instance
 * @returns {Function} - The message handler function
 */
export function createPlayerGuessHandler(lobbyManager) {
  return function handlePlayerGuess(data) {
    lobbyManager.addLogEntry(
      `Received guess from player ${data.playerName}`,
      'info',
    );

    // Skip if this is our own guess (we already handled it in handleGuessResult)
    if (data.playerId === lobbyManager.playerState.currentPlayerId) {
      lobbyManager.addLogEntry('Skipping own guess visualization', 'info');
      return;
    }

    // Find the player in the player list
    const player = lobbyManager.playerList.players.find(
      (p) => p.id === data.playerId,
    );
    if (!player) {
      lobbyManager.addLogEntry(
        `Cannot visualize guess: Player ${data.playerId} not found in player list`,
        'warning',
      );
      return;
    }

    // Store or visualize the player's guess on the map
    // This is debounced for a cool effect
    if (lobbyManager.map && lobbyManager.map.map) {
      // Get current time
      const currentTime = Date.now();

      // Check if enough time has passed since the last sound
      if (
        currentTime - lobbyManager.gameState.lastPlayerGuessSoundPlayedTime >=
        lobbyManager.gameState.guessSoundDelay
      ) {
        // Play the sound immediately
        SoundEffectsService.playSound('plop2');
        lobbyManager.gameState.lastPlayerGuessSoundPlayedTime = currentTime;
        lobbyManager.map.storePlayerGuess(
          data.playerId,
          player,
          data,
          lobbyManager.gameState.hasSubmittedGuessForCurrentRound,
        );
      } else {
        // Schedule the sound to play after the required delay
        const timeToWait =
          lobbyManager.gameState.guessSoundDelay -
          (currentTime - lobbyManager.gameState.lastPlayerGuessSoundPlayedTime);
        setTimeout(() => {
          SoundEffectsService.playSound('plop2');
          lobbyManager.gameState.lastPlayerGuessSoundPlayedTime = Date.now();
          lobbyManager.map.storePlayerGuess(
            data.playerId,
            player,
            data,
            lobbyManager.gameState.hasSubmittedGuessForCurrentRound,
          );
        }, timeToWait);
      }
    }
  };
}
