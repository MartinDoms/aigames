import { LogService } from './log-service.js';

export const SoundEffectsService = {
  sounds: {},
  initialized: false,
  muted: false,

  // Default sound configuration
  soundConfig: {
    beep: {
      path: '/audio/whoosh_2_1.mp3',
      volume: 1.0,
    },
    go: {
      path: '/audio/whoosh_3_1.mp3',
      volume: 1.0,
    },
    plop: {
      path: '/audio/interface_click_7_4.mp3',
      volume: 1.0,
    },
    plop2: {
      path: '/audio/interface_click_2_1.mp3',
      volume: 1.0,
    },
    tick: {
      path: '/audio/tick.mp3',
      volume: 1.0,
    },
    buzzer: {
      path: '/audio/negative_10_1.mp3',
      volume: 1.0,
    },
    mult: {
      path: '/audio/special_21_1.mp3',
      volume: 1.0,
    },
    gameEnd: {
      path: '/audio/special_28_2.mp3',
      volume: 1.0,
    },
    confetti: {
      path: '/audio/confetti.mp3',
      volume: 1.0,
    },
  },

  // Initialize audio context and sounds
  init() {
    if (this.initialized) return;

    try {
      // Create audio elements for each sound in config
      Object.keys(this.soundConfig).forEach((soundId) => {
        this.loadSound(soundId);
      });

      this.initialized = true;
      LogService.add('Sound effects initialized', 'info');
    } catch (error) {
      LogService.add(`Failed to initialize sound effects: ${error}`, 'error');
    }
  },

  // Load a single sound
  loadSound(soundId) {
    if (!this.soundConfig[soundId]) {
      LogService.add(`Sound config not found for: ${soundId}`, 'warning');
      return;
    }

    const config = this.soundConfig[soundId];
    const sound = new Audio();
    sound.src = config.path;
    sound.volume = config.volume || 0.5;
    sound.load();

    this.sounds[soundId] = sound;
  },

  // Add a new sound to the service
  addSound(soundId, path, volume = 0.5) {
    this.soundConfig[soundId] = { path, volume };

    // If already initialized, load the new sound immediately
    if (this.initialized) {
      this.loadSound(soundId);
      LogService.add(`New sound added: ${soundId}`, 'info');
    }
  },

  // Generic play sound function
  playSound(soundId) {
    if (this.muted) return;
    if (!this.initialized) this.init();

    const sound = this.sounds[soundId];
    if (!sound) {
      LogService.add(`Sound not found: ${soundId}`, 'warning');
      return;
    }

    try {
      // Clone the sound to allow overlapping plays
      const clone = sound.cloneNode();
      clone.volume = sound.volume;
      clone
        .play()
        .catch((e) => LogService.add(`Audio play error: ${e}`, 'warning'));
    } catch (error) {
      LogService.add(`Failed to play ${soundId} sound: ${error}`, 'warning');
    }
  },

  // Convenience methods for backward compatibility
  playBeep() {
    this.playSound('beep');
  },

  playGoSound() {
    this.playSound('go');
  },

  playPlop() {
    this.playSound('plop');
  },

  // Update a sound's configuration
  updateSound(soundId, path, volume) {
    if (!this.soundConfig[soundId]) {
      LogService.add(`Cannot update nonexistent sound: ${soundId}`, 'warning');
      return;
    }

    const config = this.soundConfig[soundId];

    if (path) config.path = path;
    if (volume !== undefined) config.volume = volume;

    // Update the sound if already initialized
    if (this.initialized && this.sounds[soundId]) {
      const sound = this.sounds[soundId];
      if (path) sound.src = path;
      if (volume !== undefined) sound.volume = volume;
      sound.load();
      LogService.add(`Updated sound: ${soundId}`, 'info');
    }
  },

  // Toggle mute state
  toggleMute() {
    this.muted = !this.muted;
    LogService.add(`Sound effects ${this.muted ? 'muted' : 'unmuted'}`, 'info');
    return this.muted;
  },

  // Set mute state directly
  setMute(state) {
    this.muted = !!state;
  },
};
