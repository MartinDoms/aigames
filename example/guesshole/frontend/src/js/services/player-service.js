// Player service for handling player-related operations
export const PlayerService = {
  sortPlayers(players) {
    return players.sort((a, b) => {
      // Host always comes first
      if (a.host && !b.host) return -1;
      if (!a.host && b.host) return 1;

      // If host status is the same, sort by ID
      if (a.id < b.id) return -1;
      if (a.id > b.id) return 1;
      return 0;
    });
  },

  highlightPlayer(playerId) {
    setTimeout(() => {
      const playerItem = document.querySelector(
        `li[data-player-id="${playerId}"]`,
      );
      if (playerItem) {
        playerItem.classList.add('animate-highlight');
        setTimeout(() => {
          playerItem.classList.remove('animate-highlight');
        }, 2000);
      }
    });
  },
};
