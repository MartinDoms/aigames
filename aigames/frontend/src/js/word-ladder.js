import '../css/main.css';
import Alpine from 'alpinejs';

window.Alpine = Alpine;

Alpine.data('wordLadder', () => ({
  startWord: 'cat',
  endWord: 'dog',
  currentWord: '',
  wordChain: [],
  currentInput: '',
  isCompleted: false,
  score: 0,
  moves: 0,
  error: '',
  hints: [],
  showHints: false,
  loading: false,
  optimalSteps: 0,

  async init() {
    await this.loadRandomPair();
    this.resetGame();
  },

  async loadRandomPair() {
    try {
      this.loading = true;
      const response = await fetch('/api/word-ladder/random-pair');
      const pair = await response.json();
      this.startWord = pair.startWord;
      this.endWord = pair.endWord;
      await this.checkOptimalSolution();
    } catch (error) {
      console.error('Failed to load word pair:', error);
      this.error = 'Failed to load new puzzle';
    } finally {
      this.loading = false;
    }
  },

  async checkOptimalSolution() {
    try {
      const response = await fetch('/api/word-ladder/check-solution', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          startWord: this.startWord,
          endWord: this.endWord
        })
      });
      const result = await response.json();
      this.optimalSteps = result.optimalSteps;
    } catch (error) {
      console.error('Failed to check solution:', error);
    }
  },

  resetGame() {
    this.currentWord = this.startWord;
    this.wordChain = [this.startWord];
    this.currentInput = '';
    this.isCompleted = false;
    this.score = 0;
    this.moves = 0;
    this.error = '';
    this.hints = [];
    this.showHints = false;
  },

  async makeMove() {
    if (!this.currentInput.trim()) {
      this.error = 'Please enter a word';
      return;
    }

    const newWord = this.currentInput.trim().toLowerCase();
    
    try {
      const response = await fetch('/api/word-ladder/validate-move', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          fromWord: this.currentWord,
          toWord: newWord
        })
      });
      
      const result = await response.json();
      
      if (result.isValid) {
        this.wordChain.push(newWord);
        this.currentWord = newWord;
        this.moves++;
        this.currentInput = '';
        this.error = '';

        // Check if puzzle is solved
        if (newWord === this.endWord) {
          this.isCompleted = true;
          this.calculateScore();
        }
      } else {
        this.error = result.message;
      }
    } catch (error) {
      console.error('Failed to validate move:', error);
      this.error = 'Failed to validate move. Please try again.';
    }
  },

  calculateScore() {
    // Score based on efficiency vs optimal solution
    if (this.optimalSteps > 0) {
      const efficiency = this.optimalSteps / this.moves;
      this.score = Math.max(100, Math.round(1000 * efficiency));
    } else {
      this.score = Math.max(100, 1000 - (this.moves * 50));
    }
  },

  async showPossibleMoves() {
    try {
      this.loading = true;
      const response = await fetch(`/api/word-ladder/possible-words/${this.currentWord}`);
      this.hints = await response.json();
      this.showHints = true;
    } catch (error) {
      console.error('Failed to get hints:', error);
      this.error = 'Failed to get hints';
    } finally {
      this.loading = false;
    }
  },

  hideHints() {
    this.showHints = false;
    this.hints = [];
  },

  async newGame() {
    await this.loadRandomPair();
    this.resetGame();
  },

  handleKeyPress(event) {
    if (event.key === 'Enter') {
      this.makeMove();
    }
  },

  getStepStatus(index) {
    if (index === 0) return 'start';
    if (index === this.wordChain.length - 1 && this.isCompleted) return 'end';
    return 'step';
  },

  getScoreColor() {
    if (this.score >= 800) return 'text-green-600';
    if (this.score >= 600) return 'text-yellow-600';
    return 'text-red-600';
  }
}));

Alpine.start();