import { PlayerService } from '../../services/player-service.js';
import { LogService } from '../../services/log-service.js';

export const GameSettings = {
  rounds: 10,
  roundLength: 60,
  geoType: 'WORLD',

  incrementRounds() {
    if (this.gameSettings.rounds < 20) {
      this.gameSettings.rounds++;
    }
  },

  decrementRounds() {
    if (this.gameSettings.rounds > 1) {
      this.gameSettings.rounds--;
    }
  },

  incrementTime() {
    if (this.gameSettings.roundLength < 120) {
      this.gameSettings.roundLength += 5;
    }
  },

  decrementTime() {
    if (this.gameSettings.roundLength > 10) {
      this.gameSettings.roundLength -= 5;
    }
  },
};
