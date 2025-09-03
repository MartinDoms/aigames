// Geography Quiz Game Logic
import Alpine from 'alpinejs';

document.addEventListener('alpine:init', () => {
    Alpine.data('geographyQuiz', () => ({
        loading: true,
        currentQuestion: null,
        currentQuestionNumber: 1,
        selectedAnswer: null,
        answered: false,
        isCorrect: false,
        feedbackMessage: '',
        score: 0,
        totalQuestions: 0,
        streak: 0,
        bestScore: 0,
        questionType: 'random',
        
        init() {
            this.loadStats();
            this.newQuestion();
        },

        async newQuestion() {
            this.loading = true;
            this.answered = false;
            this.selectedAnswer = null;
            this.isCorrect = false;
            this.feedbackMessage = '';
            this.totalQuestions++;

            try {
                const endpoint = this.questionType === 'random' 
                    ? '/api/geography-quiz/new-question'
                    : `/api/geography-quiz/question/${this.questionType}`;
                    
                const response = await fetch(endpoint);
                if (response.ok) {
                    this.currentQuestion = await response.json();
                } else {
                    console.error('Failed to fetch question');
                    this.feedbackMessage = 'Error loading question. Please try again.';
                }
            } catch (error) {
                console.error('Error fetching question:', error);
                this.feedbackMessage = 'Network error. Please check your connection.';
            } finally {
                this.loading = false;
            }
        },

        async selectAnswer(answer) {
            if (this.answered) return;

            this.selectedAnswer = answer;
            this.answered = true;

            const requestData = {
                question: this.currentQuestion.question,
                userAnswer: answer,
                correctAnswer: this.currentQuestion.correctAnswer,
                explanation: this.currentQuestion.explanation
            };

            try {
                const response = await fetch('/api/geography-quiz/check-answer', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(requestData)
                });

                if (response.ok) {
                    const result = await response.json();
                    this.isCorrect = result.isCorrect;
                    this.feedbackMessage = result.message;

                    if (this.isCorrect) {
                        this.score++;
                        this.streak++;
                        if (this.streak > this.bestScore) {
                            this.bestScore = this.streak;
                        }
                    } else {
                        this.streak = 0;
                    }

                    this.saveStats();
                } else {
                    this.feedbackMessage = 'Error checking answer. Please try again.';
                }
            } catch (error) {
                console.error('Error checking answer:', error);
                this.feedbackMessage = 'Network error. Please try again.';
            }
        },

        nextQuestion() {
            this.currentQuestionNumber++;
            this.newQuestion();
        },

        setQuestionType(type) {
            this.questionType = type;
            this.newQuestion();
        },

        getOptionButtonClass(option) {
            if (!this.answered) {
                return 'border-gray-300 text-gray-700 hover:border-blue-400';
            }

            if (option === this.currentQuestion.correctAnswer) {
                return 'border-green-500 bg-green-100 text-green-800';
            } else if (option === this.selectedAnswer && !this.isCorrect) {
                return 'border-red-500 bg-red-100 text-red-800';
            } else {
                return 'border-gray-300 text-gray-500';
            }
        },

        loadStats() {
            try {
                const saved = localStorage.getItem('geography-quiz-stats');
                if (saved) {
                    const stats = JSON.parse(saved);
                    this.score = stats.score || 0;
                    this.totalQuestions = stats.totalQuestions || 0;
                    this.streak = stats.streak || 0;
                    this.bestScore = stats.bestScore || 0;
                }
            } catch (error) {
                console.error('Error loading stats:', error);
            }
        },

        saveStats() {
            try {
                const stats = {
                    score: this.score,
                    totalQuestions: this.totalQuestions,
                    streak: this.streak,
                    bestScore: this.bestScore,
                    lastPlayed: new Date().toISOString()
                };
                localStorage.setItem('geography-quiz-stats', JSON.stringify(stats));
            } catch (error) {
                console.error('Error saving stats:', error);
            }
        },

        resetStats() {
            this.score = 0;
            this.totalQuestions = 0;
            this.streak = 0;
            this.bestScore = 0;
            this.saveStats();
        }
    }));
});

// Initialize Alpine
Alpine.start();