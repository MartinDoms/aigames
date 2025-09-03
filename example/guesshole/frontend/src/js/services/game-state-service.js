import { LogService } from './log-service.js';
import { WebSocketService } from './websocket-service.js';
import { Notification } from '../features/notification.js';
import { SoundEffectsService } from './sound-effects-service.js';

export const createGameStateService = (videoPlayer, map) => ({
  lobbyState: 'LOBBY',
  transitionInProgress: false,
  currentRound: null,
  submittedGuesses: new Map(),
  roundStartTime: null,
  roundDuration: 30,
  hasSubmittedGuessForCurrentRound: false,
  showCountdown: false,
  countdownValue: '3',
  countdownInterval: null,
  guessSoundDelay: 600,
  lastPlayerGuessSoundPlayedTime: Date.now(),

  // Round coundown timer state TODO clean this up
  timerDisplay: '',
  timerInterval: null, // this is the setInterval promise

  startCountdown() {
    // If video is muted, also mute the sound effects
    SoundEffectsService.setMute(videoPlayer.isMuted);

    // Reset countdown state
    this.countdownValue = '3';
    this.showCountdown = true;

    // Play first beep immediately
    SoundEffectsService.playBeep();

    // Clear any existing interval
    if (this.countdownInterval) {
      clearInterval(this.countdownInterval);
    }

    let count = 3;
    this.countdownInterval = setInterval(() => {
      count--;

      if (count === 0) {
        // Show GO!
        this.countdownValue = 'GO!';

        // Play the GO sound
        SoundEffectsService.playGoSound();

        // Hide the countdown after a short delay
        setTimeout(() => {
          this.showCountdown = false;
          clearInterval(this.countdownInterval);
          this.countdownInterval = null;
        }, 800);
      } else if (count > 0) {
        this.countdownValue = count.toString();

        // Play the beep sound
        SoundEffectsService.playBeep();
      }
    }, 1000);
  },

  submitGuess() {
    if (!map.selectedLocation) {
      LogService.add('Cannot submit guess: No location selected', 'warning');
      Notification.showNotification(
        'Please select a location on the map first',
        'warning',
      );
      return;
    }

    // Get current round ID
    let currentRoundId = null;
    if (this.currentRound) {
      currentRoundId = this.currentRound.id;
    }

    if (!currentRoundId) {
      LogService.add(
        'Cannot submit guess: No current round ID found',
        'warning',
      );
      Notification.showNotification(
        'Round information is missing. Please wait or refresh.',
        'warning',
      );
      return;
    }

    // Check if the player has already submitted a guess for this round
    if (this.hasSubmittedGuessForCurrentRound) {
      LogService.add(
        'Cannot submit guess: Already submitted for this round',
        'warning',
      );
      Notification.showNotification(
        'You have already submitted a guess for this round',
        'warning',
      );
      return;
    }

    if (!WebSocketService.isConnected()) {
      LogService.add(
        'WebSocket is not connected! Cannot submit guess.',
        'error',
      );
      Notification.showNotification(
        'Connection to server lost. Please refresh the page.',
        'error',
      );
      return;
    }

    // Clear any previous result visualizations
    map.clearMap();

    // Calculate time elapsed since round started (in seconds)
    const now = Date.now();
    const guessTime = this.roundStartTime
      ? Math.floor((now - this.roundStartTime) / 1000)
      : 0;

    // Clamp guess time to the round duration
    const clampedGuessTime = Math.min(guessTime, this.roundDuration);

    // Prepare the message payload
    const guessData = {
      type: 'GUESS_SUBMITTED',
      latitude: map.selectedLocation.lat,
      longitude: map.selectedLocation.lng,
      roundId: currentRoundId,
      roundDuration: this.roundDuration,
      guessTime: clampedGuessTime,
    };

    try {
      LogService.add(`Sending guess: ${JSON.stringify(guessData)}`, 'info');
      WebSocketService.send(guessData);

      // Record that we've submitted a guess for this round
      this.submittedGuesses.set(currentRoundId, true);
      this.hasSubmittedGuessForCurrentRound = true;
      // Show success notification
      Notification.showNotification(
        'Your guess has been submitted!',
        'success',
      );
    } catch (error) {
      LogService.add(`Failed to submit guess: ${error}`, 'error');
      Notification.showNotification(
        'Failed to submit your guess. Please try again.',
        'error',
      );
    }
  },

  changeGameState(newState) {
    const oldState = this.lobbyState;

    // Only change state if it's actually changing
    if (oldState === newState) return;

    LogService.add(`Changing game state: ${oldState} -> ${newState}`, 'info');
    this.transitionInProgress = true;

    // If we're leaving the GAME_IN_PROGRESS state, clear the timer
    if (oldState === 'GAME_IN_PROGRESS' && newState !== 'GAME_IN_PROGRESS') {
      this.clearRoundTimer();
    }

    // Use requestAnimationFrame for smooth transitions
    requestAnimationFrame(() => {
      // Update state
      this.lobbyState = newState;

      // Mark transition as complete after animation finishes
      setTimeout(() => {
        this.transitionInProgress = false;
        LogService.add('Game view transition complete', 'info');
      }, 600); // Slightly longer than transition duration
    });
  },

  // TODO all this timer stuff should be in a RoundTimer component
  startRoundTimer() {
    // Clear any existing timer
    this.clearRoundTimer();

    // Make sure we have valid duration and start time
    if (!this.roundDuration || !this.roundStartTime) {
      LogService.add(
        'Cannot start timer: missing duration or start time',
        'warning',
      );
      return;
    }

    // Calculate initial time remaining
    this.updateTimerDisplay();

    // Set up interval to update the timer every second
    this.timerInterval = setInterval(() => {
      this.updateTimerDisplay();
    }, 1000);

    // Set up timer for end-of-round ticking sound
    this.endOfRoundCountdownTickInterval = setTimeout(
      () => {
        SoundEffectsService.playSound('tick');
      },
      (this.roundDuration - 6) * 1000,
    );

    LogService.add(
      `Started round timer for ${this.roundDuration} seconds`,
      'info',
    );
  },

  updateTimerDisplay() {
    // Calculate time elapsed since round start
    const now = Date.now();
    const elapsedSeconds = Math.floor((now - this.roundStartTime) / 1000);

    // Calculate remaining time
    let remainingSeconds = Math.max(0, this.roundDuration - elapsedSeconds);

    // Format the time as MM:SS
    const minutes = Math.floor(remainingSeconds / 60);
    const seconds = remainingSeconds % 60;
    this.timerDisplay = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;

    // If timer reaches zero, clear the interval
    if (remainingSeconds <= 0) {
      this.clearRoundTimer();
    }
  },

  clearRoundTimer() {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }

    if (this.endOfRoundCountdownTickInterval) {
      clearTimeout(this.endOfRoundCountdownTickInterval);
      this.endOfRoundCountdownTickInterval = null;
    }
  },

  // TODO classic AI code - do this properly
  getSecondsRemaining() {
    if (!this.timerDisplay) return 0;

    const timeParts = this.timerDisplay.split(':');
    const minutes = parseInt(timeParts[0], 10);
    const seconds = parseInt(timeParts[1], 10);

    return minutes * 60 + seconds;
  },

  // Function to get percentage of time remaining
  getTimerPercentage() {
    // Use the round duration from gameState as the maximum time
    const maxTime = this.roundDuration || 60;
    const timeRemaining = this.getSecondsRemaining();

    return (timeRemaining / maxTime) * 100;
  },

  // Function to get timer color based on time remaining
  getTimerColor() {
    const percentage = this.getTimerPercentage();

    if (percentage > 50) {
      return 'var(--color-secondary)'; // Your accent color for plenty of time
    } else if (percentage > 25) {
      return 'var(--color-secondary-light)'; // Your accent-light color for moderate time
    } else {
      return 'var(--color-secondary-dark)'; // Your accent-dark color for low time
    }
  },

  // Function to get Tailwind text color class based on time remaining
  getTimerTextColorClass() {
    const percentage = this.getTimerPercentage();

    if (percentage > 50) {
      return 'text-secondary'; // Using your Tailwind color classes
    } else if (percentage > 25) {
      return 'text-secondary-light';
    } else {
      return 'text-secondary-dark';
    }
  },
});
