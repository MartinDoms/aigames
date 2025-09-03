import { SoundEffectsService } from '../../services/sound-effects-service.js';
import { HighScoreService } from '../../services/high-score-service.js';

/**
 * Creates a handler for the GAME_STATE message type
 *
 * @param {Object} lobbyManager - The lobby manager instance
 * @returns {Function} - The message handler function
 */
export function createGameStateHandler(
  lobbyManager,
  roundScoreboard,
  gameState,
  playerList,
) {
  return function handleGameState(data) {
    const newState = data.state;
    const lobbyState = gameState.lobbyState;

    // Handle state-specific actions based on the new state
    if (newState === 'GAME_IN_PROGRESS') {
      // Store current round information if available
      if (data.currentRound) {
        const previousRoundId = gameState.currentRound
          ? gameState.currentRound.id
          : null;
        const newRoundId = data.currentRound.id;

        // Check if this is a new round (different ID)
        const isNewRound = previousRoundId !== newRoundId;

        lobbyManager.addLogEntry(
          `Received round info: ${JSON.stringify(data.currentRound)}`,
          'success',
        );

        if (isNewRound) {
          lobbyManager.addLogEntry(`New round detected: ${newRoundId}`, 'info');

          // Reset the map markers for the new round
          // TODO move this to the map (I think it's already there) and take a direct dependency on map
          if (lobbyManager.map) {
            if (lobbyManager.map.clearAllVisualizations) {
              lobbyManager.map.clearAllVisualizations();
            } else {
              lobbyManager.map.resetMarker();
              lobbyManager.map.clearMap();
            }
          }
          gameState.hasSubmittedGuessForCurrentRound = false;

          // Record the round start time
          // TODO send this from the server
          gameState.roundStartTime = Date.now();
          lobbyManager.addLogEntry(
            `roundStartTime = ${gameState.roundStartTime}`,
          );

          // Get round duration from server data if available
          gameState.roundDuration = data.currentRound.durationSeconds || 30;

          // Start the countdown animation
          gameState.startCountdown();

          // Start the countdown timer for this round
          gameState.startRoundTimer();
        }

        gameState.currentRound = data.currentRound;

        lobbyManager.videoPlayer.playVideo(
          data.currentRound.youtubeVideoId,
          data.currentRound.startTimeSeconds,
        );
        lobbyManager.map.refreshMap();
      }
      // Give it a second to render, then scroll the viewport to put the top of the video at the top of the page
      setTimeout(() => {
        const element = document.getElementById('map-container');
        element.scrollIntoView({ block: 'start', behavior: 'smooth' });
      }, 200);
    } else if (newState === 'LOBBY') {
      gameState.currentRound = null;
      lobbyManager.videoPlayer.stopPlaying();

      // Clear submitted guesses when returning to lobby
      gameState.submittedGuesses.clear();
    } else if (newState === 'ROUND_SCOREBOARD') {
      gameState.currentRound = data.currentRound;

      // The map is about to change size, so invalidate it.
      lobbyManager.map.invalidateSize();

      if (data.lastRound) {
        SoundEffectsService.playSound('gameEnd');
      } else {
        SoundEffectsService.playSound('buzzer');
      }

      // Handle round scoreboard data
      gameState.clearRoundTimer();

      gameState.totalRounds = data.totalRounds;

      var isNewHighScore = false;
      var highScore = 0;
      const gameConfig = {
        rounds: gameState.totalRounds,
        geoType: lobbyManager.gameSettings?.geoType || 'DEFAULT',
        roundLength: lobbyManager.gameSettings?.roundLength || 30,
      };

      if (data.lastRound) {
        // Store high score if solo
        if (playerList.players.length === 1) {
          const playerId = playerList.players[0].id;
          if (playerId === lobbyManager.playerState.currentPlayerId) {
            // Get the current player's final score
            const playerScore = data.playerScores
              ? data.playerScores.find((score) => score.player.id === playerId)
                  ?.totalScore
              : 0;

            if (playerScore) {
              // Create game configuration object to identify the game mode

              // Try to save the high score
              const lobbyIdentifier = lobbyManager.playerState.lobbyShortCode;
              isNewHighScore = HighScoreService.saveHighScore(
                gameConfig,
                playerScore,
              );
              highScore = playerScore;
            } else {
              // Get the current high score to show comparison
              highScore = HighScoreService.getHighScore(gameConfig);
            }
          }
        }
      }

      // Set up scoreboard data
      roundScoreboard.currentRoundScoreboard = {
        isSoloGame: playerList.players.length == 1,
        roundNumber: data.roundOrder + 1,
        playerScores: data.playerScores || [],
        isLastRound: data.lastRound,
        locationPoint: data.currentRound.locationPoint,
        isNewHighScore,
        highScore,
      };
      // Clear submitted guesses for the current round
      if (gameState.currentRound && gameState.currentRound.id) {
        gameState.submittedGuesses.delete(gameState.currentRound.id);
      }

      roundScoreboard.showMult(0);
      roundScoreboard.triggerHighScoreAnimation();

      setTimeout(() => {
        const element = document.getElementById('scoreboard-container');
        element.scrollIntoView({ block: 'start', behavior: 'smooth' });
      }, 200);
    }

    // State changes that apply no matter the state
    if (data.gameConfiguration) {
      // TODO these two objects should be the same shape to make this easier and more reliable
      var config = data.gameConfiguration;
      var settings = lobbyManager.gameSettings;

      settings.rounds = config.numRounds;
      settings.roundLength = config.roundLengthSeconds;
      settings.geoType = config.geographyType;
    }

    // Only change state if it's different
    if (lobbyState !== newState) {
      gameState.changeGameState(newState);
    }
  };
}
