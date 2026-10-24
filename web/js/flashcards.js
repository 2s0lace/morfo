// Morfo Spaced Repetition Flashcards Engine
import { ALL_TEETH } from './data.js';

const STORAGE_KEY = 'morfo_flashcard_progress';

export class FlashcardsSession {
  constructor(viewer, onStateChange) {
    this.viewer = viewer;
    this.onStateChange = onStateChange;

    this.progress = this.loadProgress();
    this.currentTooth = null;
    this.isRevealed = false;
  }

  loadProgress() {
    try {
      const data = localStorage.getItem(STORAGE_KEY);
      return data ? JSON.parse(data) : {};
    } catch {
      return {};
    }
  }

  saveProgress() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(this.progress));
    } catch (e) {
      console.warn('Could not save flashcards progress', e);
    }
  }

  start() {
    this.nextCard();
  }

  nextCard() {
    // Weighted selection: teeth with low mastery score or not yet seen appear first
    const pool = [...ALL_TEETH].sort((a, b) => {
      const scoreA = this.progress[a.fdi]?.score || 0;
      const scoreB = this.progress[b.fdi]?.score || 0;
      return (scoreA + Math.random() * 2) - (scoreB + Math.random() * 2);
    });

    this.currentTooth = pool[0];
    this.isRevealed = false;

    this.viewer.setTeeth([{ toothData: this.currentTooth, variant: 'standard' }]).then(() => {
      this.viewer.setRandomRotation();
      setTimeout(() => this.viewer.resetView(), 400);
    });

    this.notify();
  }

  reveal() {
    this.isRevealed = true;
    this.notify();
  }

  rate(difficulty) {
    // difficulty: 'hard' (-1), 'good' (+1), 'easy' (+2)
    const fdi = this.currentTooth.fdi;
    const current = this.progress[fdi] || { score: 0, reviews: 0 };

    let delta = 1;
    if (difficulty === 'hard') delta = -1;
    else if (difficulty === 'easy') delta = 2;

    current.score = Math.max(0, current.score + delta);
    current.reviews = (current.reviews || 0) + 1;
    current.lastReviewed = Date.now();

    this.progress[fdi] = current;
    this.saveProgress();

    this.nextCard();
  }

  getMasteredCount() {
    return Object.values(this.progress).filter(p => p.score >= 3).length;
  }

  notify() {
    if (this.onStateChange) {
      this.onStateChange({
        currentTooth: this.currentTooth,
        isRevealed: this.isRevealed,
        masteredCount: this.getMasteredCount(),
        totalCount: ALL_TEETH.length
      });
    }
  }
}
