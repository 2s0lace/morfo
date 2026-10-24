import { ALL_TEETH, getTooth, QUICK_COMPARISONS, TOOTH_CLASSES, ARCHES, SIDES } from './data.js';
import { t, getLang, setLang } from './i18n.js';
import { ToothViewer } from './viewer3d.js';
import { QuizSession } from './quiz.js';
import { FlashcardsSession } from './flashcards.js';

const MAX_TEETH = 4;
const UPPER_ROW = [18, 17, 16, 15, 14, 13, 12, 11, 21, 22, 23, 24, 25, 26, 27, 28];
const LOWER_ROW = [48, 47, 46, 45, 44, 43, 42, 41, 31, 32, 33, 34, 35, 36, 37, 38];

class MorfoApp {
  constructor() {
    this.screen = 'selector';
    this.stageMode = 'compare';
    this.selectedTeethFdi = [];
    this.inspectedFdi = null;
    this.carabelliEnabled = false;
    this.filters = {
      classes: new Set(),
      arches: new Set(),
      sides: new Set()
    };
    this.loadToken = 0;
    this.toastTimer = null;
    this.resizeTimer = null;
    this.init();
  }

  init() {
    this.viewer = new ToothViewer(document.getElementById('viewport-container'));
    this.quiz = new QuizSession(this.viewer, (state) => this.renderQuizState(state));
    this.flashcards = new FlashcardsSession(this.viewer, (state) => this.renderFlashcardsState(state));

    this.bindEvents();
    this.renderFilterChips();
    this.renderArchChart();
    this.renderQuickComparisons();
    this.renderSelection();
    this.renderToothSpecs();
    this.updateLanguageTexts();
  }

  bindEvents() {
    document.getElementById('start-compare-btn').addEventListener('click', () => this.openCompare());
    document.getElementById('back-to-selector-btn').addEventListener('click', () => this.goSelector());
    document.getElementById('quiz-back-btn').addEventListener('click', () => this.goSelector());
    document.getElementById('fc-back-btn').addEventListener('click', () => this.goSelector());
    document.getElementById('clear-selection-btn').addEventListener('click', () => this.clearSelection());
    document.getElementById('open-quiz-btn').addEventListener('click', () => this.openQuiz());
    document.getElementById('learn-quiz-btn').addEventListener('click', () => this.openQuiz());
    document.getElementById('open-flashcards-btn').addEventListener('click', () => this.openFlashcards());
    document.getElementById('learn-flashcards-btn').addEventListener('click', () => this.openFlashcards());
    document.getElementById('open-about-btn').addEventListener('click', () => this.openSheet('about-sheet'));
    document.getElementById('open-settings-btn').addEventListener('click', () => this.openSheet('settings-sheet'));
    document.getElementById('open-specs-top-btn').addEventListener('click', () => this.openSpecs());
    document.getElementById('lang-toggle-btn').addEventListener('click', () => this.toggleLanguage());
    document.getElementById('select-matching-btn').addEventListener('click', () => this.selectMatching());
    document.getElementById('clear-filters-btn').addEventListener('click', () => this.clearFilters());

    document.getElementById('filter-chips').addEventListener('click', (event) => {
      const chip = event.target.closest('[data-filter-type]');
      if (!chip) return;
      this.toggleFilter(chip.dataset.filterType, chip.dataset.filterKey);
    });

    const archHost = document.getElementById('arch-chart');
    archHost.addEventListener('click', (event) => {
      const chip = event.target.closest('.tooth-chip');
      if (!chip) return;
      this.toggleTooth(Number(chip.dataset.fdi));
    });
    archHost.addEventListener('keydown', (event) => {
      if (event.key !== 'Enter' && event.key !== ' ') return;
      const chip = event.target.closest('.tooth-chip');
      if (!chip) return;
      event.preventDefault();
      this.toggleTooth(Number(chip.dataset.fdi));
    });

    document.getElementById('quick-comparisons-list').addEventListener('click', (event) => {
      const row = event.target.closest('[data-quick]');
      if (!row) return;
      this.openQuick(row.dataset.quick);
    });

    document.getElementById('viewport-teeth-pills').addEventListener('click', (event) => {
      const pill = event.target.closest('[data-pill-fdi]');
      if (!pill) return;
      this.inspectedFdi = Number(pill.dataset.pillFdi);
      this.renderSelection();
    });

    document.getElementById('tooth-actions').addEventListener('click', (event) => {
      const btn = event.target.closest('[data-action]');
      if (!btn) return;
      this.handleToothAction(btn.dataset.action);
    });

    document.querySelectorAll('[data-preset]').forEach((btn) => {
      btn.addEventListener('click', () => {
        const preset = btn.dataset.preset;
        if (preset === 'RESET') this.viewer.resetView();
        else this.viewer.setSurface(preset);
      });
    });

    document.querySelectorAll('.keypad-btn').forEach((btn) => {
      btn.addEventListener('click', () => {
        const key = btn.dataset.key;
        if (key === 'backspace') this.quiz.backspace();
        else if (key) this.quiz.typeDigit(key);
      });
    });
    document.getElementById('quiz-next-btn').addEventListener('click', () => this.quiz.nextQuestion());
    document.getElementById('fc-reveal-btn').addEventListener('click', () => this.flashcards.reveal());
    document.querySelectorAll('.fc-rate-btn').forEach((btn) => {
      btn.addEventListener('click', () => this.flashcards.rate(btn.dataset.diff));
    });

    document.querySelectorAll('[data-close-sheet]').forEach((btn) => {
      btn.addEventListener('click', () => this.closeSheet(btn.closest('.sheet-backdrop')));
    });
    document.querySelectorAll('.sheet-backdrop').forEach((backdrop) => {
      backdrop.addEventListener('click', (event) => {
        if (event.target === backdrop) this.closeSheet(backdrop);
      });
    });

    document.getElementById('lang-segmented').addEventListener('click', (event) => {
      const btn = event.target.closest('[data-lang]');
      if (!btn) return;
      this.changeLanguage(btn.dataset.lang);
    });

    window.addEventListener('keydown', (event) => this.handleKeydown(event));
    window.addEventListener('resize', () => {
      clearTimeout(this.resizeTimer);
      this.resizeTimer = setTimeout(() => {
        this.viewer.onResize();
        if (this.screen === 'stage') {
          this.updateViewerInsets();
          this.viewer.resetView();
        }
      }, 220);
    });
  }

  updateViewerInsets() {
    const stage = document.getElementById('screen-stage');
    const isVisible = (element) => element && element.getBoundingClientRect().height > 0;
    const topBar = [...stage.querySelectorAll('.stage-topbar')].find(isVisible);
    const bottomBar = [...stage.querySelectorAll('.stage-bottombar')].find(isVisible);
    this.viewer.setInsets(
      topBar ? topBar.getBoundingClientRect().height : 0,
      bottomBar ? bottomBar.getBoundingClientRect().height : 0
    );
  }

  handleKeydown(event) {
    const openSheet = document.querySelector('.sheet-backdrop:not(.hidden)');
    if (event.key === 'Escape') {
      if (openSheet) {
        this.closeSheet(openSheet);
        return;
      }
      if (this.screen === 'stage') this.goSelector();
      return;
    }
    if (openSheet) return;
    if (event.target.closest('input, textarea, select')) return;
    if (event.target.closest('button, a') && (event.key === ' ' || event.key === 'Enter')) return;

    if (this.screen === 'stage' && this.stageMode === 'compare') {
      if (['1', '2', '3', '4', '5'].includes(event.key)) {
        const presets = { 1: 'FACIAL', 2: 'LINGUAL', 3: 'MESIAL', 4: 'DISTAL', 5: 'OCCLUSAL' };
        this.viewer.setSurface(presets[event.key]);
      } else if (event.key === ' ' || event.key === 'r' || event.key === 'R') {
        event.preventDefault();
        this.viewer.resetView();
      }
      return;
    }

    if (this.screen === 'stage' && this.stageMode === 'quiz') {
      if (['1', '2', '3', '4', '5', '6', '7', '8'].includes(event.key)) {
        this.quiz.typeDigit(event.key);
      } else if (event.key === 'Backspace') {
        this.quiz.backspace();
      } else if ((event.key === 'Enter' || event.key === ' ') && this.quiz.isAnswered) {
        event.preventDefault();
        this.quiz.nextQuestion();
      }
      return;
    }

    if (this.screen === 'stage' && this.stageMode === 'flashcards') {
      if (event.key === ' ' || event.key === 'Enter') {
        event.preventDefault();
        if (!this.flashcards.isRevealed) this.flashcards.reveal();
      } else if (this.flashcards.isRevealed) {
        if (event.key === '1') this.flashcards.rate('hard');
        else if (event.key === '2') this.flashcards.rate('good');
        else if (event.key === '3') this.flashcards.rate('easy');
      }
    }
  }

  showScreen(name) {
    this.screen = name;
    document.querySelectorAll('.screen').forEach((screen) => {
      screen.classList.toggle('is-active', screen.id === `screen-${name}`);
    });
    if (name === 'stage') {
      setTimeout(() => this.viewer.onResize(), 60);
    }
  }

  setStageMode(mode) {
    this.stageMode = mode;
    const stage = document.getElementById('screen-stage');
    stage.classList.remove('mode-compare', 'mode-quiz', 'mode-flashcards');
    stage.classList.add(`mode-${mode}`);
  }

  goSelector() {
    this.closeAllSheets();
    this.showScreen('selector');
  }

  openCompare() {
    if (this.selectedTeethFdi.length === 0) return;
    this.setStageMode('compare');
    this.showScreen('stage');
    this.updateViewerInsets();
    this.renderSelection();
    this.loadSelectedTeethInViewer();
    setTimeout(() => {
      this.viewer.onResize();
      this.viewer.resetView();
    }, 80);
  }

  openQuiz() {
    this.closeAllSheets();
    this.setStageMode('quiz');
    this.showScreen('stage');
    this.updateViewerInsets();
    this.quiz.start();
  }

  openFlashcards() {
    this.closeAllSheets();
    this.setStageMode('flashcards');
    this.showScreen('stage');
    this.updateViewerInsets();
    this.flashcards.start();
  }

  openSpecs() {
    if (!this.inspectedFdi) return;
    this.renderToothSpecs();
    this.openSheet('specs-sheet');
  }

  openSheet(id) {
    const sheet = document.getElementById(id);
    if (!sheet) return;
    sheet.classList.remove('hidden');
  }

  closeSheet(backdrop) {
    if (!backdrop) return;
    backdrop.classList.add('hidden');
  }

  closeAllSheets() {
    document.querySelectorAll('.sheet-backdrop').forEach((sheet) => sheet.classList.add('hidden'));
  }

  toggleLanguage() {
    this.changeLanguage(getLang() === 'pl' ? 'en' : 'pl');
  }

  changeLanguage(lang) {
    setLang(lang);
    this.updateLanguageTexts();
  }

  updateLanguageTexts() {
    document.documentElement.lang = getLang();
    const langBtn = document.getElementById('lang-toggle-btn');
    if (langBtn) langBtn.textContent = getLang() === 'pl' ? 'EN' : 'PL';

    document.querySelectorAll('[data-i18n]').forEach((el) => {
      el.textContent = t(el.dataset.i18n);
    });

    document.querySelectorAll('#lang-segmented [data-lang]').forEach((btn) => {
      btn.classList.toggle('is-active', btn.dataset.lang === getLang());
    });

    this.renderFilterChips();
    this.renderArchChart();
    this.renderQuickComparisons();
    this.renderSelection();
    this.renderToothSpecs();
    if (this.quiz.currentTooth) this.quiz.notify();
    if (this.flashcards.currentTooth) this.flashcards.notify();
  }

  renderFilterChips() {
    const host = document.getElementById('filter-chips');
    if (!host) return;
    const lang = getLang();

    const groups = [
      ...Object.entries(TOOTH_CLASSES).map(([key, value]) => ({ type: 'classes', key, label: value[lang] })),
      ...Object.entries(ARCHES).map(([key, value]) => ({ type: 'arches', key, label: value[lang] })),
      ...Object.entries(SIDES).map(([key, value]) => ({ type: 'sides', key, label: value[lang] }))
    ];

    host.innerHTML = groups.map((group) => {
      const active = this.filters[group.type].has(group.key);
      return `<button type="button" class="filter-chip${active ? ' is-active' : ''}" data-filter-type="${group.type}" data-filter-key="${group.key}" aria-pressed="${active}">${group.label}</button>`;
    }).join('');

    const active = this.filtersActive();
    document.getElementById('select-matching-btn').disabled = !active;
    document.getElementById('clear-filters-btn').disabled = !active;
  }

  filtersActive() {
    return this.filters.classes.size > 0 || this.filters.arches.size > 0 || this.filters.sides.size > 0;
  }

  matchesFilters(tooth) {
    if (!tooth) return false;
    return (this.filters.classes.size === 0 || this.filters.classes.has(tooth.kind.classKey))
      && (this.filters.arches.size === 0 || this.filters.arches.has(tooth.archKey))
      && (this.filters.sides.size === 0 || this.filters.sides.has(tooth.sideKey));
  }

  toggleFilter(type, key) {
    const set = this.filters[type];
    if (!set) return;
    if (set.has(key)) set.delete(key);
    else set.add(key);
    this.renderFilterChips();
    this.renderArchChart();
  }

  clearFilters() {
    this.filters.classes.clear();
    this.filters.arches.clear();
    this.filters.sides.clear();
    this.renderFilterChips();
    this.renderArchChart();
  }

  selectMatching() {
    const matches = ALL_TEETH.filter((tooth) => this.matchesFilters(tooth)).slice(0, MAX_TEETH);
    if (matches.length === 0) return;
    this.selectedTeethFdi = matches.map((tooth) => tooth.fdi);
    this.inspectedFdi = this.selectedTeethFdi[0];
    this.afterSelectionChange();
  }

  chipSize(tooth) {
    switch (tooth.kind.classKey) {
      case 'MOLAR':
        return 36;
      case 'PREMOLAR':
        return 33;
      case 'CANINE':
        return 30;
      default:
        return 28;
    }
  }

  archPositions(row, upper) {
    const widths = row.map((fdi) => this.chipSize(getTooth(fdi)));
    const total = widths.reduce((sum, width) => sum + width, 0);
    let acc = 0;

    return row.map((fdi, index) => {
      const mid = acc + widths[index] / 2;
      acc += widths[index];
      const angle = Math.PI * (1 - (0.02 + 0.96 * mid / total));
      const x = 180 + 158 * Math.cos(angle);
      const y = upper
        ? 392 * 0.47 - 145 * Math.sin(angle)
        : 392 * 0.53 + 145 * Math.sin(angle);
      return { fdi, x, y, size: widths[index], name: getTooth(fdi)[getLang() === 'pl' ? 'namePl' : 'nameEn'] };
    });
  }

  renderArchChart() {
    const host = document.getElementById('arch-chart');
    if (!host) return;

    const selected = new Set(this.selectedTeethFdi);
    const filterActive = this.filtersActive();

    const chipMarkup = ({ fdi, x, y, size, name }) => {
      const half = size / 2;
      const isSelected = selected.has(fdi);
      const isDimmed = filterActive && !this.matchesFilters(getTooth(fdi));
      const classes = ['tooth-chip'];
      if (isSelected) classes.push('is-selected');
      if (isDimmed) classes.push('is-dimmed');
      return `<g class="${classes.join(' ')}" data-fdi="${fdi}" tabindex="0" role="button" aria-pressed="${isSelected}" aria-label="${name}">
        <rect x="${(x - half - 7).toFixed(1)}" y="${(y - half - 7).toFixed(1)}" width="${size + 14}" height="${size + 14}" rx="14" fill="transparent"/>
        <rect class="chip-body" x="${(x - half).toFixed(1)}" y="${(y - half).toFixed(1)}" width="${size}" height="${size}" rx="${Math.min(11, size * 0.34).toFixed(1)}"/>
        <text class="chip-label" x="${x.toFixed(1)}" y="${y.toFixed(1)}" text-anchor="middle" dominant-baseline="central">${fdi}</text>
      </g>`;
    };

    const upperChips = this.archPositions(UPPER_ROW, true).map(chipMarkup).join('');
    const lowerChips = this.archPositions(LOWER_ROW, false).map(chipMarkup).join('');

    host.innerHTML = `<svg class="arch-chart" viewBox="0 0 360 392" role="group" aria-label="${t('tabCompare')}">
      <text class="arch-caption" x="180" y="132" text-anchor="middle">${t('maxillaryShort')}</text>
      <text class="arch-caption" x="180" y="260" text-anchor="middle">${t('mandibularShort')}</text>
      <text class="arch-caption" x="7" y="196" text-anchor="start" dominant-baseline="central">${t('rightSideShort')}</text>
      <text class="arch-caption" x="353" y="196" text-anchor="end" dominant-baseline="central">${t('leftSideShort')}</text>
      ${upperChips}
      ${lowerChips}
    </svg>`;
  }

  updateArchChartClasses() {
    const host = document.getElementById('arch-chart');
    if (!host) return;
    const selected = new Set(this.selectedTeethFdi);
    const filterActive = this.filtersActive();

    host.querySelectorAll('.tooth-chip').forEach((chip) => {
      const fdi = Number(chip.dataset.fdi);
      const isSelected = selected.has(fdi);
      chip.classList.toggle('is-selected', isSelected);
      chip.classList.toggle('is-dimmed', filterActive && !this.matchesFilters(getTooth(fdi)));
      chip.setAttribute('aria-pressed', String(isSelected));
    });
  }

  renderQuickComparisons() {
    const host = document.getElementById('quick-comparisons-list');
    if (!host) return;
    const lang = getLang();

    host.innerHTML = QUICK_COMPARISONS.map((comparison) => {
      const teeth = comparison.teeth.map((fdi, index) => {
        const suffix = comparison.variantSecond === 'carabelli' && index === 1 ? 'C' : '';
        return `${fdi}${suffix}`;
      }).join(' · ');
      const title = lang === 'pl' ? comparison.titlePl : comparison.titleEn;
      return `<button type="button" class="quick-row" data-quick="${comparison.id}">
        <span class="quick-title">${title}</span>
        <span class="quick-teeth">${teeth}</span>
      </button>`;
    }).join('');
  }

  openQuick(id) {
    const comparison = QUICK_COMPARISONS.find((item) => item.id === id);
    if (!comparison) return;
    this.selectedTeethFdi = [...comparison.teeth];
    this.inspectedFdi = comparison.teeth[0];
    this.carabelliEnabled = comparison.variantSecond === 'carabelli';
    this.afterSelectionChange();
    this.openCompare();
  }

  toggleTooth(fdi) {
    const index = this.selectedTeethFdi.indexOf(fdi);
    if (index >= 0) {
      this.selectedTeethFdi.splice(index, 1);
      if (this.inspectedFdi === fdi) this.inspectedFdi = this.selectedTeethFdi[0] ?? null;
    } else {
      if (this.selectedTeethFdi.length >= MAX_TEETH) {
        this.showToast(t('selectionLimit'));
        return;
      }
      this.selectedTeethFdi.push(fdi);
      this.inspectedFdi = fdi;
    }
    this.afterSelectionChange();
  }

  clearSelection() {
    this.selectedTeethFdi = [];
    this.inspectedFdi = null;
    this.carabelliEnabled = false;
    this.afterSelectionChange();
  }

  afterSelectionChange() {
    this.renderSelection();
    if (this.screen === 'stage' && this.stageMode === 'compare') {
      this.loadSelectedTeethInViewer();
    }
    if (!document.getElementById('specs-sheet').classList.contains('hidden')) {
      this.renderToothSpecs();
    }
  }

  loadSelectedTeethInViewer() {
    const items = this.selectedTeethFdi.map((fdi) => {
      const tooth = getTooth(fdi);
      return {
        toothData: tooth,
        variant: this.carabelliEnabled && tooth.hasCarabelli ? 'carabelli' : 'standard'
      };
    });

    this.loadToken += 1;
    const token = this.loadToken;
    if (this.screen === 'stage') this.setLoading(items.length > 0);
    Promise.resolve(this.viewer.setTeeth(items)).finally(() => {
      if (token === this.loadToken) this.setLoading(false);
    });
  }

  setLoading(active) {
    document.getElementById('stage-loading').classList.toggle('hidden', !active);
  }

  renderSelection() {
    const selectionText = document.getElementById('selection-text');
    const clearBtn = document.getElementById('clear-selection-btn');
    const compareBtn = document.getElementById('start-compare-btn');
    const countBadge = document.getElementById('compare-count');
    const hasSelection = this.selectedTeethFdi.length > 0;

    if (hasSelection) {
      selectionText.textContent = this.selectedTeethFdi.join(' · ');
      selectionText.classList.remove('is-empty');
      clearBtn.classList.remove('hidden');
    } else {
      selectionText.textContent = t('noneSelectedShort');
      selectionText.classList.add('is-empty');
      clearBtn.classList.add('hidden');
    }

    compareBtn.disabled = !hasSelection;
    countBadge.textContent = String(this.selectedTeethFdi.length);
    countBadge.classList.toggle('hidden', !hasSelection);

    const pills = document.getElementById('viewport-teeth-pills');
    pills.innerHTML = this.selectedTeethFdi.map((fdi) => {
      const tooth = getTooth(fdi);
      const suffix = this.carabelliEnabled && tooth.hasCarabelli ? 'C' : '';
      const active = this.inspectedFdi === fdi ? ' is-active' : '';
      return `<button type="button" class="stage-pill${active}" data-pill-fdi="${fdi}">${fdi}${suffix}</button>`;
    }).join('');

    this.renderSelectedToothBar();
    this.updateArchChartClasses();
  }

  renderSelectedToothBar() {
    const bar = document.getElementById('selected-tooth-bar');
    const hasInspected = this.inspectedFdi && this.selectedTeethFdi.includes(this.inspectedFdi);

    if (!hasInspected) {
      bar.classList.add('hidden');
      return;
    }

    const tooth = getTooth(this.inspectedFdi);
    const carabelli = this.carabelliEnabled && tooth.hasCarabelli;
    const name = getLang() === 'pl' ? tooth.namePl : tooth.nameEn;

    bar.classList.remove('hidden');
    document.getElementById('selected-fdi-badge').textContent = `${tooth.fdi}${carabelli ? 'C' : ''}`;
    document.getElementById('selected-tooth-name').textContent = name;

    const actions = [
      { action: 'info', label: t('toothInfo') },
      { action: 'reset', label: t('resetRotation') },
      { action: 'center', label: t('centerView') }
    ];
    if (tooth.hasCarabelli) {
      actions.push({ action: 'carabelli', label: t('carabelliShort'), active: this.carabelliEnabled });
    }
    if (this.selectedTeethFdi.length < MAX_TEETH) {
      actions.push({ action: 'duplicate', label: t('duplicate') });
    }
    actions.push({ action: 'remove', label: t('remove'), danger: true });

    document.getElementById('tooth-actions').innerHTML = actions.map((item) => {
      const classes = ['action-pill'];
      if (item.active) classes.push('is-active');
      if (item.danger) classes.push('danger');
      return `<button type="button" class="${classes.join(' ')}" data-action="${item.action}">${item.label}</button>`;
    }).join('');
  }

  handleToothAction(action) {
    const fdi = this.inspectedFdi;
    if (!fdi) return;
    const tooth = getTooth(fdi);
    const index = this.selectedTeethFdi.indexOf(fdi);

    switch (action) {
      case 'info':
        this.openSpecs();
        break;
      case 'reset':
        this.viewer.resetRotation(fdi);
        break;
      case 'center':
        this.viewer.resetView();
        break;
      case 'carabelli':
        if (!tooth.hasCarabelli) return;
        this.carabelliEnabled = !this.carabelliEnabled;
        this.renderSelection();
        this.loadSelectedTeethInViewer();
        this.renderToothSpecs();
        break;
      case 'duplicate':
        if (this.selectedTeethFdi.length >= MAX_TEETH) return;
        this.selectedTeethFdi.push(fdi);
        this.inspectedFdi = fdi;
        this.afterSelectionChange();
        break;
      case 'remove':
        if (index >= 0) this.selectedTeethFdi.splice(index, 1);
        if (!this.selectedTeethFdi.includes(fdi)) {
          this.inspectedFdi = this.selectedTeethFdi[0] ?? null;
        }
        this.afterSelectionChange();
        break;
      default:
        break;
    }
  }

  renderToothSpecs() {
    const host = document.getElementById('tooth-specs-content');
    if (!host) return;

    const inspected = this.inspectedFdi && this.selectedTeethFdi.includes(this.inspectedFdi)
      ? getTooth(this.inspectedFdi)
      : null;

    if (!inspected) {
      host.innerHTML = `<p class="empty-note">${t('noneSelected')}</p>`;
      return;
    }

    const lang = getLang();
    const name = lang === 'pl' ? inspected.namePl : inspected.nameEn;
    const features = lang === 'pl' ? inspected.featuresPl : inspected.featuresEn;
    const carabelli = this.carabelliEnabled && inspected.hasCarabelli;

    const carabelliMarkup = inspected.hasCarabelli ? `
      <div class="carabelli-row">
        <label class="toggle-switch">
          <input type="checkbox" class="carabelli-input" ${carabelli ? 'checked' : ''}>
          <span class="slider"></span>
        </label>
        <span class="carabelli-label">${t('carabelliToggle')}</span>
      </div>` : '';

    host.innerHTML = `
      <div class="specs-header">
        <span class="fdi-big-badge">${inspected.fdi}${carabelli ? 'C' : ''}</span>
        <div>
          <h4 class="tooth-title">${name}</h4>
          <span class="tooth-subtitle">${inspected.isMirrored ? `${t('mirrorScan')} ${inspected.sourceFdi}` : `${t('sourceModel')} ${inspected.modelId}`}</span>
        </div>
      </div>
      <div class="specs-metric">
        <span class="metric-label">${t('averageLength')}</span>
        <span class="metric-val">${inspected.avgLength} mm <small>(${t('crownAndRoot')})</small></span>
      </div>
      ${carabelliMarkup}
      <div class="specs-features">
        <h5 class="specs-features-title">${t('specsTitle')}</h5>
        <ul class="specs-list">
          ${features.map((feature) => `<li class="spec-item">${feature}</li>`).join('')}
        </ul>
      </div>`;

    const carabelliInput = host.querySelector('.carabelli-input');
    if (carabelliInput) {
      carabelliInput.addEventListener('change', (event) => {
        this.carabelliEnabled = event.target.checked;
        this.renderSelection();
        this.loadSelectedTeethInViewer();
        this.renderToothSpecs();
      });
    }
  }

  renderQuizState(state) {
    if (!state || !state.currentTooth) return;

    const input = document.getElementById('quiz-input-display');
    const feedback = document.getElementById('quiz-feedback-card');
    const nextBtn = document.getElementById('quiz-next-btn');
    const streak = document.getElementById('quiz-streak-count');
    const score = document.getElementById('quiz-score-count');
    const keypad = document.getElementById('quiz-keypad');

    input.textContent = (state.input || '').padEnd(2, '_');
    streak.textContent = String(state.streak);
    score.textContent = `${state.score} / ${state.totalAnswered}`;
    keypad.classList.toggle('hidden', state.isAnswered);

    if (state.isAnswered) {
      const toothName = getLang() === 'pl' ? state.currentTooth.namePl : state.currentTooth.nameEn;
      feedback.classList.remove('hidden');
      nextBtn.classList.remove('hidden');
      feedback.className = `quiz-feedback ${state.isCorrect ? 'correct' : 'incorrect'}`;

      if (state.isCorrect) {
        feedback.innerHTML = `
          <div class="feedback-icon">✓</div>
          <div>
            <div class="feedback-title">${t('correctAnswer')} <b>${state.currentTooth.fdi}</b></div>
            <div class="feedback-desc">${toothName}</div>
          </div>`;
      } else {
        feedback.innerHTML = `
          <div class="feedback-icon">✕</div>
          <div>
            <div class="feedback-title">${t('incorrectAnswer')} <b>${state.currentTooth.fdi}</b></div>
            <div class="feedback-desc">${toothName} (${t('yourAnswer')} ${state.input})</div>
          </div>`;
      }
    } else {
      feedback.classList.add('hidden');
      nextBtn.classList.add('hidden');
    }
  }

  refitFlashcards() {
    requestAnimationFrame(() => {
      if (this.screen === 'stage' && this.stageMode === 'flashcards') {
        this.updateViewerInsets();
        this.viewer.resetView();
      }
    });
  }

  renderFlashcardsState(state) {
    if (!state || !state.currentTooth) return;

    const answerCard = document.getElementById('fc-answer-card');
    const revealBtn = document.getElementById('fc-reveal-btn');
    const ratingGroup = document.getElementById('fc-rating-group');
    document.getElementById('fc-mastered-count').textContent = `${state.masteredCount} / ${state.totalCount}`;

    if (!state.isRevealed) {
      answerCard.classList.add('hidden');
      revealBtn.classList.remove('hidden');
      ratingGroup.classList.add('hidden');
      this.refitFlashcards();
      return;
    }

    const tooth = state.currentTooth;
    const lang = getLang();
    const name = lang === 'pl' ? tooth.namePl : tooth.nameEn;
    const features = lang === 'pl' ? tooth.featuresPl : tooth.featuresEn;

    revealBtn.classList.add('hidden');
    answerCard.classList.remove('hidden');
    ratingGroup.classList.remove('hidden');
    answerCard.innerHTML = `
      <div class="fc-answer-header">
        <span class="fdi-big-badge">${tooth.fdi}</span>
        <div>
          <h4 class="tooth-title">${name}</h4>
          <span class="tooth-subtitle">${tooth.avgLength} mm (${t('crownAndRoot')})</span>
        </div>
      </div>
      <ul class="specs-list" style="margin-top:12px">
        ${features.slice(0, 3).map((feature) => `<li class="spec-item">${feature}</li>`).join('')}
      </ul>`;
    this.refitFlashcards();
  }

  showToast(message) {
    const toast = document.getElementById('toast');
    if (!toast) return;
    toast.textContent = message;
    toast.classList.add('is-visible');
    clearTimeout(this.toastTimer);
    this.toastTimer = setTimeout(() => toast.classList.remove('is-visible'), 2000);
  }
}

window.addEventListener('DOMContentLoaded', () => {
  window.morfoApp = new MorfoApp();
});
