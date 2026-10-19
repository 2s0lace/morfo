// Morfo Web Application Main Controller
import { ALL_TEETH, getTooth, QUICK_COMPARISONS } from './data.js';
import { t, getLang, setLang } from './i18n.js';
import { ToothViewer } from './viewer3d.js';
import { QuizSession } from './quiz.js';
import { FlashcardsSession } from './flashcards.js';

class MorfoApp {
  constructor() {
    this.viewer = null;
    this.quiz = null;
    this.flashcards = null;

    this.activeTab = 'compare'; // 'compare', 'quiz', 'flashcards', 'about'
    this.selectedTeethFdi = [11, 21]; // Default: central incisors
    this.inspectedFdi = 11; // Tooth whose morphology specs are displayed
    this.carabelliEnabled = false;

    this.init();
  }

  init() {
    const container = document.getElementById('viewport-container');
    this.viewer = new ToothViewer(container);

    this.quiz = new QuizSession(this.viewer, (state) => this.renderQuizState(state));
    this.flashcards = new FlashcardsSession(this.viewer, (state) => this.renderFlashcardsState(state));

    this.bindEvents();
    this.updateLanguageTexts();
    this.renderOdontogram();
    this.renderQuickComparisons();
    this.loadSelectedTeethInViewer();
    this.renderToothSpecs();
  }

  bindEvents() {
    // Navigation tabs
    document.querySelectorAll('.nav-tab').forEach((tab) => {
      tab.addEventListener('click', (e) => {
        const target = e.currentTarget.dataset.tab;
        this.switchTab(target);
      });
    });

    // Language switcher
    const langBtn = document.getElementById('lang-toggle-btn');
    if (langBtn) {
      langBtn.addEventListener('click', () => {
        const nextLang = getLang() === 'pl' ? 'en' : 'pl';
        setLang(nextLang);
        this.updateLanguageTexts();
        this.renderOdontogram();
        this.renderQuickComparisons();
        this.renderToothSpecs();
        if (this.activeTab === 'quiz') this.quiz.notify();
        if (this.activeTab === 'flashcards') this.flashcards.notify();
      });
    }

    // Surface preset buttons
    document.querySelectorAll('.surface-btn').forEach((btn) => {
      btn.addEventListener('click', (e) => {
        const preset = e.currentTarget.dataset.preset;
        if (preset === 'RESET') {
          this.viewer.resetView();
        } else {
          this.viewer.setSurface(preset);
        }
      });
    });

    // Clear selection button
    const clearBtn = document.getElementById('clear-selection-btn');
    if (clearBtn) {
      clearBtn.addEventListener('click', () => {
        this.selectedTeethFdi = [];
        this.renderSelectedPills();
        this.renderOdontogram();
        this.loadSelectedTeethInViewer();
        this.renderToothSpecs();
      });
    }

    // Carabelli toggle
    const carabelliCheckbox = document.getElementById('carabelli-toggle');
    if (carabelliCheckbox) {
      carabelliCheckbox.addEventListener('change', (e) => {
        this.carabelliEnabled = e.target.checked;
        this.loadSelectedTeethInViewer();
      });
    }

    // Quiz on-screen keypad
    document.querySelectorAll('.keypad-btn').forEach((btn) => {
      btn.addEventListener('click', (e) => {
        const val = e.currentTarget.dataset.key;
        if (val === 'backspace') {
          this.quiz.backspace();
        } else if (val) {
          this.quiz.typeDigit(val);
        }
      });
    });

    const nextQuizBtn = document.getElementById('quiz-next-btn');
    if (nextQuizBtn) {
      nextQuizBtn.addEventListener('click', () => this.quiz.nextQuestion());
    }

    // Flashcard buttons
    const revealBtn = document.getElementById('fc-reveal-btn');
    if (revealBtn) {
      revealBtn.addEventListener('click', () => this.flashcards.reveal());
    }
    document.querySelectorAll('.fc-rate-btn').forEach((btn) => {
      btn.addEventListener('click', (e) => {
        const diff = e.currentTarget.dataset.diff;
        this.flashcards.rate(diff);
      });
    });

    // Physical Keyboard Shortcuts for Laptop/PC users
    window.addEventListener('keydown', (e) => {
      // Ignore if user is inside an input field
      if (['INPUT', 'TEXTAREA'].includes(e.target.tagName)) return;

      if (this.activeTab === 'compare') {
        if (e.key === '1') this.viewer.setSurface('FACIAL');
        else if (e.key === '2') this.viewer.setSurface('LINGUAL');
        else if (e.key === '3') this.viewer.setSurface('MESIAL');
        else if (e.key === '4') this.viewer.setSurface('DISTAL');
        else if (e.key === '5') this.viewer.setSurface('OCCLUSAL');
        else if (e.key === ' ' || e.key === 'r' || e.key === 'R') {
          e.preventDefault();
          this.viewer.resetView();
        }
      } else if (this.activeTab === 'quiz') {
        if (['1', '2', '3', '4', '5', '6', '7', '8'].includes(e.key)) {
          this.quiz.typeDigit(e.key);
        } else if (e.key === 'Backspace') {
          this.quiz.backspace();
        } else if ((e.key === 'Enter' || e.key === ' ') && this.quiz.isAnswered) {
          e.preventDefault();
          this.quiz.nextQuestion();
        }
      } else if (this.activeTab === 'flashcards') {
        if (e.key === ' ' || e.key === 'Enter') {
          e.preventDefault();
          if (!this.flashcards.isRevealed) {
            this.flashcards.reveal();
          }
        } else if (this.flashcards.isRevealed) {
          if (e.key === '1') this.flashcards.rate('hard');
          else if (e.key === '2') this.flashcards.rate('good');
          else if (e.key === '3') this.flashcards.rate('easy');
        }
      }
    });
  }

  switchTab(tabId) {
    this.activeTab = tabId;

    document.querySelectorAll('.nav-tab').forEach((tab) => {
      tab.classList.toggle('active', tab.dataset.tab === tabId);
    });

    // Toggle panels
    document.querySelectorAll('.view-panel').forEach((p) => p.classList.add('hidden'));
    const targetPanel = document.getElementById(`panel-${tabId}`);
    if (targetPanel) targetPanel.classList.remove('hidden');

    // Controls visibility
    const compareControls = document.getElementById('compare-floating-controls');
    const compareSidebar = document.getElementById('compare-sidebar');
    if (compareControls) compareControls.classList.toggle('hidden', tabId !== 'compare');
    if (compareSidebar) compareSidebar.classList.toggle('hidden', tabId !== 'compare');

    // Activate tab-specific logic
    if (tabId === 'compare') {
      this.loadSelectedTeethInViewer();
    } else if (tabId === 'quiz') {
      this.quiz.start();
    } else if (tabId === 'flashcards') {
      this.flashcards.start();
    }

    setTimeout(() => this.viewer.onResize(), 50);
  }

  updateLanguageTexts() {
    const lang = getLang();
    document.getElementById('lang-toggle-btn').textContent = lang === 'pl' ? 'EN' : 'PL';

    // Update data-i18n elements
    document.querySelectorAll('[data-i18n]').forEach((el) => {
      const key = el.dataset.i18n;
      el.textContent = t(key);
    });
  }

  renderOdontogram() {
    const upperContainer = document.getElementById('odontogram-upper');
    const lowerContainer = document.getElementById('odontogram-lower');
    if (!upperContainer || !lowerContainer) return;

    upperContainer.innerHTML = '';
    lowerContainer.innerHTML = '';

    // Upper arch: 18..11 then 21..28
    const q1 = [18, 17, 16, 15, 14, 13, 12, 11];
    const q2 = [21, 22, 23, 24, 25, 26, 27, 28];

    // Lower arch: 48..41 then 31..38
    const q4 = [48, 47, 46, 45, 44, 43, 42, 41];
    const q3 = [31, 32, 33, 34, 35, 36, 37, 38];

    const createBtn = (fdi) => {
      const btn = document.createElement('button');
      btn.className = `tooth-chip ${this.selectedTeethFdi.includes(fdi) ? 'active' : ''}`;
      btn.textContent = fdi;
      btn.dataset.fdi = fdi;
      btn.title = getTooth(fdi)[getLang() === 'pl' ? 'namePl' : 'nameEn'];

      btn.addEventListener('click', () => {
        this.toggleToothSelection(fdi);
      });
      return btn;
    };

    const q1Group = document.createElement('div');
    q1Group.className = 'quadrant-group';
    q1.forEach((fdi) => q1Group.appendChild(createBtn(fdi)));

    const q2Group = document.createElement('div');
    q2Group.className = 'quadrant-group';
    q2.forEach((fdi) => q2Group.appendChild(createBtn(fdi)));

    upperContainer.appendChild(q1Group);
    upperContainer.appendChild(q2Group);

    const q4Group = document.createElement('div');
    q4Group.className = 'quadrant-group';
    q4.forEach((fdi) => q4Group.appendChild(createBtn(fdi)));

    const q3Group = document.createElement('div');
    q3Group.className = 'quadrant-group';
    q3.forEach((fdi) => q3Group.appendChild(createBtn(fdi)));

    lowerContainer.appendChild(q4Group);
    lowerContainer.appendChild(q3Group);

    this.renderSelectedPills();
  }

  toggleToothSelection(fdi) {
    const idx = this.selectedTeethFdi.indexOf(fdi);
    if (idx >= 0) {
      this.selectedTeethFdi.splice(idx, 1);
    } else {
      if (this.selectedTeethFdi.length >= 4) {
        this.selectedTeethFdi.shift(); // Keep max 4 teeth for clean side-by-side view
      }
      this.selectedTeethFdi.push(fdi);
      this.inspectedFdi = fdi;
    }

    this.renderSelectedPills();
    this.renderOdontogram();
    this.loadSelectedTeethInViewer();
    this.renderToothSpecs();
  }

  renderSelectedPills() {
    const container = document.getElementById('selected-pills-container');
    if (!container) return;

    container.innerHTML = '';
    if (this.selectedTeethFdi.length === 0) {
      container.innerHTML = `<span class="text-muted text-sm">${t('noneSelected')}</span>`;
      return;
    }

    this.selectedTeethFdi.forEach((fdi) => {
      const pill = document.createElement('button');
      pill.className = `pill-badge ${this.inspectedFdi === fdi ? 'active-inspected' : ''}`;
      pill.textContent = fdi;
      pill.addEventListener('click', () => {
        this.inspectedFdi = fdi;
        this.renderSelectedPills();
        this.renderToothSpecs();
      });
      container.appendChild(pill);
    });
  }

  renderQuickComparisons() {
    const container = document.getElementById('quick-comparisons-list');
    if (!container) return;

    container.innerHTML = '';
    const lang = getLang();

    QUICK_COMPARISONS.forEach((qc) => {
      const chip = document.createElement('button');
      chip.className = 'quick-chip';
      chip.textContent = lang === 'pl' ? qc.titlePl : qc.titleEn;

      chip.addEventListener('click', () => {
        this.selectedTeethFdi = [...qc.teeth];
        this.inspectedFdi = qc.teeth[0];
        if (qc.variantSecond === 'carabelli') {
          this.carabelliEnabled = true;
          const cb = document.getElementById('carabelli-toggle');
          if (cb) cb.checked = true;
        }
        this.renderSelectedPills();
        this.renderOdontogram();
        this.loadSelectedTeethInViewer();
        this.renderToothSpecs();
      });

      container.appendChild(chip);
    });
  }

  loadSelectedTeethInViewer() {
    if (this.activeTab !== 'compare') return;

    const items = this.selectedTeethFdi.map((fdi) => {
      const tooth = getTooth(fdi);
      const isCarabelli = this.carabelliEnabled && tooth.hasCarabelli;
      return {
        toothData: tooth,
        variant: isCarabelli ? 'carabelli' : 'standard'
      };
    });

    this.viewer.setTeeth(items);
  }

  renderToothSpecs() {
    const container = document.getElementById('tooth-specs-card');
    if (!container) return;

    if (!this.inspectedFdi || this.selectedTeethFdi.length === 0) {
      container.innerHTML = `<p class="text-muted text-sm">${t('noneSelected')}</p>`;
      return;
    }

    const tooth = getTooth(this.inspectedFdi);
    const lang = getLang();
    const name = lang === 'pl' ? tooth.namePl : tooth.nameEn;
    const features = lang === 'pl' ? tooth.featuresPl : tooth.featuresEn;

    let carabelliHtml = '';
    if (tooth.hasCarabelli) {
      carabelliHtml = `
        <div class="carabelli-row">
          <label class="toggle-switch">
            <input type="checkbox" id="carabelli-toggle" ${this.carabelliEnabled ? 'checked' : ''}>
            <span class="slider"></span>
          </label>
          <span class="text-sm font-medium">${t('carabelliToggle')}</span>
        </div>
      `;
    }

    let featuresListHtml = features
      .map((f) => `<li class="spec-item">${f}</li>`)
      .join('');

    container.innerHTML = `
      <div class="specs-header">
        <span class="fdi-big-badge">${tooth.fdi}</span>
        <div>
          <h4 class="tooth-title">${name}</h4>
          <span class="tooth-subtitle text-muted text-xs">
            ${tooth.isMirrored ? t('mirrorScan') + ' ' + tooth.sourceFdi : t('sourceModel') + ' ' + tooth.modelId}
          </span>
        </div>
      </div>

      <div class="specs-metric">
        <span class="metric-label text-muted text-xs">${t('averageLength')}</span>
        <span class="metric-val">${tooth.avgLength} mm <span class="text-xs text-muted">(${t('crownAndRoot')})</span></span>
      </div>

      ${carabelliHtml}

      <div class="specs-features">
        <h5 class="specs-features-title text-xs font-semibold text-muted">${t('specsTitle')}</h5>
        <ul class="specs-list">
          ${featuresListHtml}
        </ul>
      </div>
    `;

    // Re-bind carabelli toggle
    const cb = document.getElementById('carabelli-toggle');
    if (cb) {
      cb.addEventListener('change', (e) => {
        this.carabelliEnabled = e.target.checked;
        this.loadSelectedTeethInViewer();
      });
    }
  }

  renderQuizState(state) {
    const inputEl = document.getElementById('quiz-input-display');
    const feedbackCard = document.getElementById('quiz-feedback-card');
    const nextBtn = document.getElementById('quiz-next-btn');
    const streakEl = document.getElementById('quiz-streak-count');
    const scoreEl = document.getElementById('quiz-score-count');

    if (inputEl) {
      inputEl.textContent = state.input ? state.input : '__';
    }
    if (streakEl) streakEl.textContent = state.streak;
    if (scoreEl) scoreEl.textContent = `${state.score} / ${state.totalAnswered}`;

    if (feedbackCard && nextBtn) {
      if (state.isAnswered) {
        feedbackCard.classList.remove('hidden');
        nextBtn.classList.remove('hidden');

        const lang = getLang();
        const toothName = lang === 'pl' ? state.currentTooth.namePl : state.currentTooth.nameEn;

        if (state.isCorrect) {
          feedbackCard.className = 'quiz-feedback correct';
          feedbackCard.innerHTML = `
            <div class="feedback-icon">✓</div>
            <div>
              <div class="feedback-title">${t('correctAnswer')} <b>${state.currentTooth.fdi}</b></div>
              <div class="feedback-desc text-sm text-muted">${toothName}</div>
            </div>
          `;
        } else {
          feedbackCard.className = 'quiz-feedback incorrect';
          feedbackCard.innerHTML = `
            <div class="feedback-icon">✕</div>
            <div>
              <div class="feedback-title">${t('incorrectAnswer')} <b>${state.currentTooth.fdi}</b></div>
              <div class="feedback-desc text-sm text-muted">${toothName} (${t('yourAnswer')} ${state.input})</div>
            </div>
          `;
        }
      } else {
        feedbackCard.classList.add('hidden');
        nextBtn.classList.add('hidden');
      }
    }
  }

  renderFlashcardsState(state) {
    const answerCard = document.getElementById('fc-answer-card');
    const revealBtn = document.getElementById('fc-reveal-btn');
    const ratingGroup = document.getElementById('fc-rating-group');
    const masteredEl = document.getElementById('fc-mastered-count');

    if (masteredEl) {
      masteredEl.textContent = `${state.masteredCount} / ${state.totalCount}`;
    }

    if (answerCard && revealBtn && ratingGroup) {
      if (state.isRevealed) {
        answerCard.classList.remove('hidden');
        revealBtn.classList.add('hidden');
        ratingGroup.classList.remove('hidden');

        const lang = getLang();
        const tooth = state.currentTooth;
        const name = lang === 'pl' ? tooth.namePl : tooth.nameEn;
        const features = lang === 'pl' ? tooth.featuresPl : tooth.featuresEn;

        answerCard.innerHTML = `
          <div class="fc-answer-header">
            <span class="fdi-big-badge">${tooth.fdi}</span>
            <div>
              <h4 class="tooth-title">${name}</h4>
              <span class="text-xs text-muted">${tooth.avgLength} mm (${t('crownAndRoot')})</span>
            </div>
          </div>
          <ul class="specs-list text-sm mt-3">
            ${features.slice(0, 3).map((f) => `<li>${f}</li>`).join('')}
          </ul>
        `;
      } else {
        answerCard.classList.add('hidden');
        revealBtn.classList.remove('hidden');
        ratingGroup.classList.add('hidden');
      }
    }
  }
}

// Start app on DOMContentLoaded
window.addEventListener('DOMContentLoaded', () => {
  window.morfoApp = new MorfoApp();
});
