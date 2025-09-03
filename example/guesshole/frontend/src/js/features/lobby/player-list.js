import { PlayerService } from '../../services/player-service.js';
import { LogService } from '../../services/log-service.js';

export const PlayerList = {
  players: [],
  message: 'Loading players...',
  currentPlayerId: null, // maintain this state so we can render the UI properly

  updatePlayerList(players) {
    // Add menu state to each player
    const enhancedPlayers = players.map((player) => ({
      ...player,
      showMenu: false,
    }));

    this.players = PlayerService.sortPlayers(enhancedPlayers);
  },

  updateSinglePlayer(updatedPlayer) {
    // Check if player exists in the list
    const playerIndex = this.players.findIndex(
      (p) => p.id === updatedPlayer.id,
    );

    if (playerIndex >= 0) {
      // Preserve the showMenu state if it exists
      const showMenu = this.players[playerIndex].showMenu || false;

      // Update player in the list, maintaining the menu state
      this.players[playerIndex] = {
        ...updatedPlayer,
        showMenu,
      };

      // Add animation
      this.$nextTick(() => {
        PlayerService.highlightPlayer(updatedPlayer.id);
      });
    } else {
      // If player not found, add them with initial menu state
      this.addNewPlayer(updatedPlayer);
    }
  },

  addNewPlayer(player) {
    // Check if player already exists
    if (this.players.some((p) => p.id === player.id)) {
      return;
    }

    // Add player to the list with menu state
    const playerWithMenuState = {
      ...player,
      showMenu: false,
    };

    this.players.push(playerWithMenuState);

    // Sort players
    this.players = PlayerService.sortPlayers(this.players);

    // Add animation
    this.$nextTick(() => {
      PlayerService.highlightPlayer(player.id);
    });
  },

  changePlayerStatus(playerId, isActive) {
    LogService.add(
      `Player ${playerId} status changed to ${isActive ? 'active' : 'inactive'}`,
      'info',
    );

    const player = this.players.find((p) => p.id === playerId);
    if (player) {
      player.active = isActive;
    }
    return player;
  },
};
