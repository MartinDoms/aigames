/**
 * Creates a handler for the GUESS_RESULT message type
 *
 * @param {Object} lobbyManager - The lobby manager instance
 * @returns {Function} - The message handler function
 */
export function createGuessResultHandler(lobbyManager, gameState, playerState) {
  return function handleGuessResult(data) {
    lobbyManager.addLogEntry(
      `Received guess result for player ${data.player.id} for round ${data.roundId}`,
      'success',
    );

    var isCurrentPlayerGuess = data.player.id == playerState.currentPlayerId;

    if (isCurrentPlayerGuess) {
      // Record that we've submitted a guess for this round (in case we didn't catch it on submit)
      gameState.submittedGuesses.set(data.roundId, true);
    }

    // Format distance for display
    const distanceFormatted = Math.round(data.distanceKm);

    lobbyManager.map.applyPlayerGuess(
      data.player,
      data.guessLocation,
      data.actualLocation,
      distanceFormatted,
      isCurrentPlayerGuess,
    );
  };
}
