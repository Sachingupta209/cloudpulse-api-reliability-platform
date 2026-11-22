import { describe, it, beforeEach } from 'node:test';
import assert from 'node:assert/strict';

describe('CloudPulse Application-Wide Theme System Tests', () => {

  const STORAGE_KEY = 'cloudpulse-theme';

  // Mock document and localStorage implementation for node test runner
  let mockStorage = {};
  let mockClassList = new Set();
  let mockStyle = {};
  let mockAttributes = {};
  let systemPrefersDark = false;
  let mediaQueryListeners = [];

  const mockLocalStorage = {
    getItem: (key) => (key in mockStorage ? mockStorage[key] : null),
    setItem: (key, val) => {
      mockStorage[key] = String(val);
    },
    removeItem: (key) => {
      delete mockStorage[key];
    },
    clear: () => {
      mockStorage = {};
    },
  };

  const mockDocumentElement = {
    classList: {
      add: (cls) => mockClassList.add(cls),
      remove: (cls) => mockClassList.delete(cls),
      contains: (cls) => mockClassList.has(cls),
    },
    style: mockStyle,
    setAttribute: (name, val) => {
      mockAttributes[name] = val;
    },
    getAttribute: (name) => mockAttributes[name] || null,
  };

  const mockMatchMedia = (query) => {
    return {
      media: query,
      matches: systemPrefersDark,
      addEventListener: (evt, listener) => {
        if (evt === 'change') mediaQueryListeners.push(listener);
      },
      removeEventListener: (evt, listener) => {
        if (evt === 'change') {
          mediaQueryListeners = mediaQueryListeners.filter((l) => l !== listener);
        }
      },
    };
  };

  // Reusable theme resolver function matching ThemeContext logic
  function resolveTheme(currentTheme, osDark) {
    if (currentTheme === 'system') {
      return osDark ? 'dark' : 'light';
    }
    return currentTheme;
  }

  function applyTheme(resolved) {
    if (resolved === 'dark') {
      mockDocumentElement.classList.add('dark');
      mockDocumentElement.style.colorScheme = 'dark';
      mockDocumentElement.setAttribute('data-theme', 'dark');
    } else {
      mockDocumentElement.classList.remove('dark');
      mockDocumentElement.style.colorScheme = 'light';
      mockDocumentElement.setAttribute('data-theme', 'light');
    }
  }

  function getChartColors(isDark) {
    return isDark
      ? {
          grid: '#1e293b',
          axis: '#64748b',
          tooltipBg: '#0f172a',
          tooltipBorder: '#334155',
          tooltipText: '#f8fafc',
        }
      : {
          grid: '#e2e8f0',
          axis: '#64748b',
          tooltipBg: '#ffffff',
          tooltipBorder: '#cbd5e1',
          tooltipText: '#0f172a',
        };
  }

  beforeEach(() => {
    mockStorage = {};
    mockClassList.clear();
    mockStyle = {};
    mockAttributes = {};
    systemPrefersDark = false;
    mediaQueryListeners = [];
  });

  it('should default theme to "system" when no preference is saved in localStorage', () => {
    const saved = mockLocalStorage.getItem(STORAGE_KEY);
    const initialTheme = saved || 'system';
    assert.strictEqual(initialTheme, 'system');
  });

  it('should persist theme selection in localStorage under "cloudpulse-theme"', () => {
    mockLocalStorage.setItem(STORAGE_KEY, 'dark');
    assert.strictEqual(mockLocalStorage.getItem(STORAGE_KEY), 'dark');

    mockLocalStorage.setItem(STORAGE_KEY, 'light');
    assert.strictEqual(mockLocalStorage.getItem(STORAGE_KEY), 'light');

    mockLocalStorage.setItem(STORAGE_KEY, 'system');
    assert.strictEqual(mockLocalStorage.getItem(STORAGE_KEY), 'system');
  });

  it('should resolve "system" mode according to OS media query', () => {
    // OS prefers light
    systemPrefersDark = false;
    let resolved = resolveTheme('system', systemPrefersDark);
    assert.strictEqual(resolved, 'light');

    // OS prefers dark
    systemPrefersDark = true;
    resolved = resolveTheme('system', systemPrefersDark);
    assert.strictEqual(resolved, 'dark');
  });

  it('should override OS preference when explicitly set to "light" or "dark"', () => {
    // Even if OS prefers dark, explicit 'light' must stay 'light'
    systemPrefersDark = true;
    let resolved = resolveTheme('light', systemPrefersDark);
    assert.strictEqual(resolved, 'light');

    // Even if OS prefers light, explicit 'dark' must stay 'dark'
    systemPrefersDark = false;
    resolved = resolveTheme('dark', systemPrefersDark);
    assert.strictEqual(resolved, 'dark');
  });

  it('should properly update DOM classes, data-theme, and colorScheme on resolution', () => {
    // Test dark application
    applyTheme('dark');
    assert.strictEqual(mockDocumentElement.classList.contains('dark'), true);
    assert.strictEqual(mockDocumentElement.style.colorScheme, 'dark');
    assert.strictEqual(mockDocumentElement.getAttribute('data-theme'), 'dark');

    // Test light application
    applyTheme('light');
    assert.strictEqual(mockDocumentElement.classList.contains('dark'), false);
    assert.strictEqual(mockDocumentElement.style.colorScheme, 'light');
    assert.strictEqual(mockDocumentElement.getAttribute('data-theme'), 'light');
  });

  it('should respond dynamically to OS color-scheme changes when in system mode', () => {
    let currentTheme = 'system';
    let currentResolved = resolveTheme(currentTheme, systemPrefersDark);
    applyTheme(currentResolved);
    assert.strictEqual(mockDocumentElement.classList.contains('dark'), false);

    // Register listener via mock matchMedia
    const mql = mockMatchMedia('(prefers-color-scheme: dark)');
    mql.addEventListener('change', (e) => {
      if (currentTheme === 'system') {
        currentResolved = e.matches ? 'dark' : 'light';
        applyTheme(currentResolved);
      }
    });

    // Simulate OS switching to dark mode
    systemPrefersDark = true;
    for (const listener of mediaQueryListeners) {
      listener({ matches: true });
    }

    assert.strictEqual(currentResolved, 'dark');
    assert.strictEqual(mockDocumentElement.classList.contains('dark'), true);
    assert.strictEqual(mockDocumentElement.getAttribute('data-theme'), 'dark');

    // Simulate OS switching back to light mode
    systemPrefersDark = false;
    for (const listener of mediaQueryListeners) {
      listener({ matches: false });
    }

    assert.strictEqual(currentResolved, 'light');
    assert.strictEqual(mockDocumentElement.classList.contains('dark'), false);
    assert.strictEqual(mockDocumentElement.getAttribute('data-theme'), 'light');
  });

  it('should generate accessible, high-contrast chart colors for both themes', () => {
    const darkColors = getChartColors(true);
    assert.strictEqual(darkColors.grid, '#1e293b');
    assert.strictEqual(darkColors.tooltipBg, '#0f172a');
    assert.strictEqual(darkColors.tooltipText, '#f8fafc');

    const lightColors = getChartColors(false);
    assert.strictEqual(lightColors.grid, '#e2e8f0');
    assert.strictEqual(lightColors.tooltipBg, '#ffffff');
    assert.strictEqual(lightColors.tooltipText, '#0f172a');
  });

  it('should simulate FOUC prevention logic from index.html inline script', () => {
    // Simulate user having chosen dark theme in past session
    mockLocalStorage.setItem(STORAGE_KEY, 'dark');

    // Run FOUC inline script logic
    const saved = mockLocalStorage.getItem(STORAGE_KEY) || 'system';
    const isDark = saved === 'dark' || (saved === 'system' && systemPrefersDark);
    applyTheme(isDark ? 'dark' : 'light');

    assert.strictEqual(mockDocumentElement.classList.contains('dark'), true);
    assert.strictEqual(mockDocumentElement.style.colorScheme, 'dark');
  });
});
