/**
 * mode-toggle.js
 *
 * Provides keyboard-accessible tablist behaviour for editing mode toggles.
 */

(function () {
  function initModeToggle(tablist) {
    const tabs = Array.from(tablist.querySelectorAll('[role="tab"][data-mode-btn]'));
    if (!tabs.length) return;

    const activate = (tab) => {
      const modeChange = new CustomEvent('modechange', {
        detail: { mode: tab.dataset.modeBtn },
        bubbles: true,
        cancelable: true
      });
      if (!tablist.dispatchEvent(modeChange)) return false;

      tabs.forEach(candidate => {
        const active = candidate === tab;
        candidate.setAttribute('aria-selected', String(active));
        candidate.tabIndex = active ? 0 : -1;

        const panelId = candidate.getAttribute('aria-controls');
        const panel = panelId ? document.getElementById(panelId) : null;
        if (panel) panel.hidden = !active;
      });
      return true;
    };

    tabs.forEach(tab => {
      tab.addEventListener('click', () => {
        if (!activate(tab)) {
          const selected = tabs.find(t => t.getAttribute('aria-selected') === 'true');
          selected?.focus();
        }
      });
    });

    tablist.addEventListener('keydown', event => {
      const currentTab = event.target.closest('[role="tab"][data-mode-btn]');
      if (!currentTab || !tabs.includes(currentTab)) return;

      let nextIndex;
      const currentIndex = tabs.indexOf(currentTab);
      switch (event.key) {
        case 'ArrowLeft':
          nextIndex = (currentIndex - 1 + tabs.length) % tabs.length;
          break;
        case 'ArrowRight':
          nextIndex = (currentIndex + 1) % tabs.length;
          break;
        case 'Home':
          nextIndex = 0;
          break;
        case 'End':
          nextIndex = tabs.length - 1;
          break;
        default:
          return;
      }

      event.preventDefault();
      const nextTab = tabs[nextIndex];
      if (activate(nextTab)) nextTab.focus();
    });

    const selectedTab = tabs.find(tab => tab.getAttribute('aria-selected') === 'true') || tabs[0];
    activate(selectedTab);
  }

  function init() {
    document.querySelectorAll('.mode-toggle[role="tablist"]').forEach(initModeToggle);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
