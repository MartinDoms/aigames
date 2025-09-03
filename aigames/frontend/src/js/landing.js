import '../css/main.css';
import Alpine from 'alpinejs';

// Make Alpine available globally
window.Alpine = Alpine;

// Landing page Alpine component
Alpine.data('gamesLanding', () => ({
  games: [],
  loading: true,
  error: null,

  async init() {
    try {
      await this.loadGames();
    } catch (error) {
      console.error('Failed to load games:', error);
      this.error = 'Failed to load games. Please try again later.';
    } finally {
      this.loading = false;
    }
  },

  async loadGames() {
    const response = await fetch('/api/games');
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }
    this.games = await response.json();
  },

  playGame(gameName) {
    window.location.href = `/games/${gameName}`;
  },

  getCategoryIcon(category) {
    const icons = {
      word: '📝',
      number: '🔢',
      geography: '🌍',
      trivia: '🧠',
      puzzle: '🧩',
      default: '🎮'
    };
    return icons[category] || icons.default;
  }
}));

// Start Alpine
Alpine.start();