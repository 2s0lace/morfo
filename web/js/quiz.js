// Morfo FDI Quiz Logic
import { ALL_TEETH } from './data.js';

export class QuizSession {
  constructor(viewer, onStateChange) {
    this.viewer = viewer;
    this.onStateChange = onStateChange;

    this.currentTooth = null;
    this.questionId = 0;
    this.modelStatus = 'idle';
    this.input = '';
    this.isAnswered = false;
    this.isCorrect = false;
    this.streak = 0;
    this.score = 0;
    this.totalAnswered = 0;
    this.recentTeeth = [];
  }

  start() {
    this.nextQuestion();
  }

  async nextQuestion() {
    const questionId = ++this.questionId;
    // Pick random tooth avoiding immediate repetition
    let candidates = ALL_TEETH.filter(t => !this.recentTeeth.includes(t.fdi));
    if (candidates.length === 0) candidates = ALL_TEETH;

    const randomIndex = Math.floor(Math.random() * candidates.length);
    this.currentTooth = candidates[randomIndex];

    this.recentTeeth.push(this.currentTooth.fdi);
    if (this.recentTeeth.length > 5) this.recentTeeth.shift();

    this.input = '';
    this.isAnswered = false;
    this.isCorrect = false;
    this.modelStatus = 'loading';
    this.notify();

    const loaded = await this.viewer.setTeeth(
      [{ toothData: this.currentTooth, variant: 'standard' }],
      { randomRotation: true }
    );
    if (questionId !== this.questionId) return;
    this.modelStatus = loaded ? 'ready' : 'failed';
    this.notify();
  }

  typeDigit(digit) {
    if (this.modelStatus !== 'ready' || this.isAnswered) return;
    if (this.input.length >= 2) return;

    // First digit must be 1-4 (quadrant)
    if (this.input.length === 0 && !['1', '2', '3', '4'].includes(digit)) {
      return;
    }
    // Second digit must be 1-8 (tooth kind)
    if (this.input.length === 1 && !['1', '2', '3', '4', '5', '6', '7', '8'].includes(digit)) {
      return;
    }

    this.input += digit;
    this.notify();

    // Auto check if 2 digits are entered
    if (this.input.length === 2) {
      this.check();
    }
  }

  backspace() {
    if (this.isAnswered) return;
    if (this.input.length > 0) {
      this.input = this.input.slice(0, -1);
      this.notify();
    }
  }

  check() {
    if (this.modelStatus !== 'ready' || this.isAnswered || this.input.length !== 2) return;

    const enteredFdi = parseInt(this.input, 10);
    this.isAnswered = true;
    this.totalAnswered++;

    if (enteredFdi === this.currentTooth.fdi) {
      this.isCorrect = true;
      this.streak++;
      this.score++;
    } else {
      this.isCorrect = false;
      this.streak = 0;
    }

    this.notify();
  }

  notify() {
    if (this.onStateChange) {
      this.onStateChange({
        currentTooth: this.currentTooth,
        modelStatus: this.modelStatus,
        input: this.input,
        isAnswered: this.isAnswered,
        isCorrect: this.isCorrect,
        streak: this.streak,
        score: this.score,
        totalAnswered: this.totalAnswered
      });
    }
  }
}
