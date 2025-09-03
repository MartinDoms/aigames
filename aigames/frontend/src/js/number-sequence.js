// Number Sequence Game Logic
import Alpine from 'alpinejs';

document.addEventListener('alpine:init', () => {
    Alpine.data('numberSequence', () => ({
        loading: true,
        currentSequence: [],
        currentHint: '',
        userAnswer: '',
        showResult: false,
        isCorrect: false,
        correctAnswer: null,
        resultMessage: '',
        showHint: false,
        score: 0,
        streak: 0,
        stats: {
            total: 0,
            correct: 0,
            incorrect: 0
        },

        init() {
            this.loadStats();
            this.newPuzzle();
        },

        async newPuzzle() {
            this.loading = true;
            this.showResult = false;
            this.showHint = false;
            this.userAnswer = '';
            this.currentHint = '';
            this.isCorrect = false;
            this.correctAnswer = null;
            this.resultMessage = '';

            try {
                const response = await fetch('/api/number-sequence/new-puzzle');
                if (response.ok) {
                    const puzzle = await response.json();
                    this.currentSequence = puzzle.sequence;
                    this.currentHint = puzzle.hint;
                    this.correctAnswer = puzzle.nextValue;
                } else {
                    console.error('Failed to fetch new puzzle');
                    // Fallback puzzle
                    this.currentSequence = [2, 4, 6, 8, 10];
                    this.currentHint = 'Each number increases by 2';
                    this.correctAnswer = 12;
                }
            } catch (error) {
                console.error('Error fetching puzzle:', error);
                // Fallback puzzle
                this.currentSequence = [2, 4, 6, 8, 10];
                this.currentHint = 'Each number increases by 2';
                this.correctAnswer = 12;
            }

            this.loading = false;
        },

        async submitAnswer() {
            if (!this.userAnswer || this.userAnswer === '') {
                return;
            }

            const userAnswerNum = parseInt(this.userAnswer);
            this.isCorrect = userAnswerNum === this.correctAnswer;
            
            // Update statistics
            this.stats.total++;
            if (this.isCorrect) {
                this.stats.correct++;
                this.streak++;
                // Score based on streak: base 10 points + streak bonus
                this.score += 10 + (this.streak - 1) * 2;
                this.resultMessage = this.getCorrectMessage();
            } else {
                this.stats.incorrect++;
                this.streak = 0;
                this.resultMessage = "Not quite right!";
            }

            this.saveStats();
            this.showResult = true;
        },

        getCorrectMessage() {
            const messages = [
                "Excellent! You got it!",
                "Perfect! Well done!",
                "Great job! You found the pattern!",
                "Awesome! You're on fire!",
                "Outstanding! Keep it up!",
                "Brilliant! You nailed it!"
            ];
            return messages[Math.floor(Math.random() * messages.length)];
        },

        nextPuzzle() {
            this.newPuzzle();
        },

        getHint() {
            this.showHint = true;
        },

        hideCurrentHint() {
            this.showHint = false;
        },

        getDifficultyName() {
            if (this.score < 50) return "Beginner";
            if (this.score < 150) return "Intermediate";
            if (this.score < 300) return "Advanced";
            return "Expert";
        },

        getAccuracy() {
            if (this.stats.total === 0) return "0%";
            return Math.round((this.stats.correct / this.stats.total) * 100) + "%";
        },

        handleKeyPress(event) {
            if (event.key === 'Enter' && !this.showResult) {
                this.submitAnswer();
            }
        },

        loadStats() {
            const stored = localStorage.getItem('numberSequenceStats');
            if (stored) {
                const data = JSON.parse(stored);
                this.score = data.score || 0;
                this.streak = data.streak || 0;
                this.stats = data.stats || { total: 0, correct: 0, incorrect: 0 };
            }
        },

        saveStats() {
            const data = {
                score: this.score,
                streak: this.streak,
                stats: this.stats
            };
            localStorage.setItem('numberSequenceStats', JSON.stringify(data));
        }
    }));
});