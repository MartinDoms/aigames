import { MultiplierAnimations } from './multiplier-animations.js';
import { WebSocketService } from '../../services/websocket-service.js';
import { LogService } from '../../services/log-service.js';
import { SoundEffectsService } from '../../services/sound-effects-service.js';

export const RoundScoreboard = {
  currentRoundScoreboard: null,
  multiplierAnimations: MultiplierAnimations,
  locationPoint: null,

  // Reference to lobby manager, will be set by lobby manager
  lobbyManager: null,

  getPlayerScoresSortedByTotalScore() {
    var scores = this.currentRoundScoreboard?.playerScores || [];
    return scores.sort((a, b) => b.player.totalScore - a.player.totalScore);
  },

  // Add new methods to handle multiplier animations
  getMultiplierClass(type, isCurrentPlayer) {
    return this.multiplierAnimations.getMultiplierClass(type, isCurrentPlayer);
  },

  getMultiplierIcon(type) {
    return this.multiplierAnimations.getMultiplierIcon(type);
  },

  shouldShowMult(mult) {
    return !!mult.shown;
  },

  showMult(index) {
    var currentPlayerId = this.lobbyManager.playerState.currentPlayerId;
    var currentPlayerScore = this.currentRoundScoreboard.playerScores.find(
      (e) => e.player.id === currentPlayerId,
    );

    if (
      !currentPlayerScore ||
      currentPlayerScore.guess.scoreMultipliers.length < 1
    ) {
      return;
    }

    setTimeout(() => {
      currentPlayerScore.guess.scoreMultipliers[index].shown = true;

      SoundEffectsService.playSound('mult');
      if (index > 2) {
        SoundEffectsService.playSound('confetti');
        this.triggerMultiplierConfetti(index);
      }

      if (index < currentPlayerScore.guess.scoreMultipliers.length - 1) {
        this.showMult(index + 1);
      }
    }, 1000);
  },

  // Get isHost from lobby manager
  get isHost() {
    return this.lobbyManager && this.lobbyManager.isHost;
  },

  getPlayerScore(playerId) {
    return this.currentRoundScoreboard?.playerScores.find(
      (score) => score.player.id === playerId,
    );
  },

  getGuessAdminArea(adminLevel, playerId) {
    var playerScore = this.getPlayerScore(playerId);
    if (playerScore) {
      return playerScore.guess.locationPoint[`admin${adminLevel}Name`];
    } else {
      return 'Unknown';
    }
  },

  getTargetAdminArea(adminLevel) {
    if (this.currentRoundScoreboard) {
      return this.currentRoundScoreboard.locationPoint[
        `admin${adminLevel}Name`
      ];
    } else {
      return 'Unknown';
    }
  },

  getTargetAdminAreaType(adminLevel) {
    if (this.currentRoundScoreboard) {
      return this.currentRoundScoreboard.locationPoint[
        `admin${adminLevel}Type`
      ];
    } else {
      return 'Unknown';
    }
  },

  getDoesGuessMatchTargetAdminArea(adminLevel, playerId) {
    var playerScore = this.getPlayerScore(playerId);
    if (playerScore && this.currentRoundScoreboard) {
      return (
        playerScore.guess.locationPoint[`admin${adminLevel}Name`] ===
        this.currentRoundScoreboard.locationPoint[`admin${adminLevel}Name`]
      );
    } else {
      return false;
    }
  },

  triggerConfetti(containerId, particleCount) {
    var container = document.getElementById(containerId);

    const rect = container.getBoundingClientRect();
    const originX = rect.left + rect.width / 2;
    const originY = rect.top + rect.height / 2;

    // Confetti colors - using game theme colors
    const colors = [
      '#FF1493', // Deep Pink
      '#00BFFF', // Deep Sky Blue
      '#7FFF00', // Chartreuse
      '#FFD700', // Gold
      '#FF4500', // Orange Red
      '#9370DB', // Medium Purple
      '#FFA07A', // Light Salmon
      '#20B2AA', // Light Sea Green
      '#FFFF33', // Yellow
      '#87CEFA', // Light Sky Blue
      '#FF69B4', // Hot Pink
      '#32CD32', // Lime Green
    ];

    // Create and animate each particle
    for (let i = 0; i < particleCount; i++) {
      this.createConfettiParticle(container, originX, originY, colors);
    }
  },

  createConfettiParticle(container, originX, originY, colors) {
    // Create SVG element
    const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    svg.setAttribute('viewBox', '0 0 10 10');
    svg.style.position = 'absolute';
    svg.style.left = `${originX}px`;
    svg.style.top = `${originY}px`;
    svg.style.width = '16px';
    svg.style.height = '16px';
    svg.style.transform = 'translate(-50%, -50%)';

    // Randomize particle type (square, circle, or custom shape)
    const particleType = Math.floor(Math.random() * 3);
    const color = colors[Math.floor(Math.random() * colors.length)];
    let shape;

    if (particleType === 0) {
      // Square
      shape = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
      shape.setAttribute('x', '2');
      shape.setAttribute('y', '2');
      shape.setAttribute('width', '12');
      shape.setAttribute('height', '12');
      shape.setAttribute('fill', color);
    } else if (particleType === 1) {
      // Circle
      shape = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
      shape.setAttribute('cx', '10');
      shape.setAttribute('cy', '10');
      shape.setAttribute('r', '6');
      shape.setAttribute('fill', color);
    } else {
      // Star-like shape
      shape = document.createElementNS('http://www.w3.org/2000/svg', 'polygon');
      shape.setAttribute('points', '5,2 6,5 9,5 7,7 8,10 5,8 2,10 3,7 1,5 4,5');
      shape.setAttribute('fill', color);
      shape.setAttribute('transform', 'scale(1.0) translate(5, 5)');
    }

    svg.appendChild(shape);
    container.appendChild(svg);

    // Random animation values
    // Random animation values - biased toward upward direction
    const angle = Math.random() * Math.PI * 1 + Math.PI; // + Math.PI * 0.35; // Mostly upward (117° to 243°)
    // Add occasional particles in other directions (10% chance)
    const randomDirection =
      Math.random() < 0.1 ? Math.random() * Math.PI * 2 : angle;

    var velocityX = 9 + Math.random() * 15;
    var velocityY = 30 + Math.random() * 15;
    const rotationSpeed = (Math.random() - 0.5) * 3;
    const duration = 2000 + Math.random() * 1500;

    let startTime = null;

    // Animate particle
    const animate = (timestamp) => {
      if (!startTime) startTime = timestamp;
      const elapsed = timestamp - startTime;
      const progress = elapsed / duration;

      if (progress < 1) {
        // Calculate position
        const x =
          originX + Math.cos(randomDirection) * velocityX * progress * 50;
        const y =
          originY +
          Math.sin(randomDirection) * velocityY * progress * 50 +
          progress * progress * 2500; // Add gravity

        // Update position
        svg.style.left = `${x}px`;
        svg.style.top = `${y}px`;

        // Rotation and fade
        svg.style.transform = `translate(-50%, -50%) rotate(${rotationSpeed * elapsed}deg)`;
        svg.style.opacity = 1 - progress;

        requestAnimationFrame(animate);
      } else {
        // Remove element when animation completes
        svg.remove();
      }
    };

    requestAnimationFrame(animate);
  },

  triggerHighScoreAnimation() {
    if (this.currentRoundScoreboard.isNewHighScore) {
      SoundEffectsService.playSound('confetti');
      setTimeout(() => this.triggerConfetti('high-score-container', 80), 100);
    }
  },

  triggerMultiplierConfetti(multIndex, multiplierValue) {
    const particleCount = 40 + multIndex * 20;
    this.triggerConfetti('mult-container', particleCount);
  },

  // Helper to get computed CSS colors from your theme
  getComputedStyleValue(variable, fallback) {
    const style = getComputedStyle(document.documentElement);
    return style.getPropertyValue(variable) || fallback;
  },

  isLastRound() {
    return (
      this.currentRoundScoreboard?.roundNumber ===
      this.lobbyManager.gameState.totalRounds
    );
  },

  // Get player's final rank
  getFinalRank(playerId) {
    const sortedPlayers = this.getPlayerScoresSortedByTotalScore();
    for (let i = 0; i < sortedPlayers.length; i++) {
      if (sortedPlayers[i].player.id === playerId) {
        return i + 1;
      }
    }
    return 0;
  },
};
