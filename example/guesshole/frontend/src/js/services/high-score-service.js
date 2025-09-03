export const HighScoreService = {
  /**
   * Stores a high score for a specific game configuration
   * @param {Object} gameConfig - The game configuration (rounds, geoType, etc.)
   * @param {number} score - The player's score
   * @returns {boolean} - Whether a new high score was set
   */
  saveHighScore(gameConfig, score) {
    // Create a key that uniquely identifies this game mode
    const gameMode = this._createGameModeKey(gameConfig);
    const storageKey = `highScore_${gameMode}`;

    // Get existing high score if any
    const existingHighScore =
      parseInt(localStorage.getItem(`${storageKey}`), 10) || 0;

    // Only update if the new score is higher
    if (score > existingHighScore) {
      localStorage.setItem(`${storageKey}`, score.toString());

      // Store timestamp of when this high score was achieved
      const dateKey = `${storageKey}_date`;
      localStorage.setItem(dateKey, new Date().toISOString());

      return true; // New high score was set
    }

    return false; // No new high score
  },

  /**
   * Gets the high score for a specific game configuration
   * @param {Object} gameConfig - The game configuration
   * @returns {Object} - High score data including score and date
   */
  getHighScore(gameConfig) {
    const gameMode = this._createGameModeKey(gameConfig);
    const storageKey = `highScore_${gameMode}`;

    const score = parseInt(localStorage.getItem(`${storageKey}`), 10) || 0;
    const dateStr = localStorage.getItem(`${storageKey}_date`);

    return {
      score,
      date: dateStr ? new Date(dateStr) : null,
    };
  },

  /**
   * Creates a unique key for a game mode based on its configuration
   * @param {Object} gameConfig - The game configuration
   * @returns {string} - A unique identifier for this game mode
   */
  _createGameModeKey(gameConfig) {
    // Start with the number of rounds
    let key = `rounds_${gameConfig.rounds || 0}`;

    // Add other configuration parameters that affect gameplay
    if (gameConfig.geoType) {
      key += `_geo_${gameConfig.geoType}`;
    }

    if (gameConfig.roundLength) {
      key += `_length_${gameConfig.roundLength}`;
    }

    // Add any other configuration parameters here in the future

    return key;
  },
};
