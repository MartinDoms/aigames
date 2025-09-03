export const MultiplierAnimations = {
  // This will hold the animations in progress
  animationPromises: new Map(),

  // Multiplier type to class and icon mapping
  multiplierClasses: {
    TIME: 'multiplier-time',
    CORRECT_CONTINENT: 'multiplier-continent',
    CORRECT_COUNTRY: 'multiplier-country',
    CORRECT_CITY: 'multiplier-city',
    FIRST_GUESS: 'multiplier-first',
    TRIGGER_HAPPY: 'multiplier-trigger-happy',
  },

  // Multiplier icons in SVG format
  multiplierIcons: {
    TIME: '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>',
    CORRECT_COUNTRY: '🌎',
    CORRECT_CITY: '🏛️',
    CORRECT_COUNTY: '🚗',
    FIRST_GUESS: '🏁',
    TRIGGER_HAPPY: '⚡',
  },

  // Method to get the multiplier class based on type
  getMultiplierClass(type, isCurrentPlayer) {
    const baseClass = isCurrentPlayer
      ? 'multiplier'
      : 'other-player-multiplier';
    return `${baseClass} ${this.multiplierClasses[type] || 'multiplier-default'}`;
  },

  // Method to get the multiplier icon based on type
  getMultiplierIcon(type) {
    return this.multiplierIcons[type] || this.multiplierIcons.TIME; // Default to TIME icon
  },
};
