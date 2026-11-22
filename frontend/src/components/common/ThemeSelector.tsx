import React from 'react';
import { Laptop, Moon, Sun } from 'lucide-react';
import { useTheme, type ThemeMode } from '../../context/ThemeContext';

interface ThemeSelectorProps {
  variant?: 'compact' | 'detailed';
  className?: string;
}

export const ThemeSelector: React.FC<ThemeSelectorProps> = ({ variant = 'compact', className = '' }) => {
  const { theme, resolvedTheme, setTheme } = useTheme();

  const options: Array<{
    mode: ThemeMode;
    label: string;
    description: string;
    icon: React.ComponentType<{ className?: string }>;
  }> = [
    {
      mode: 'light',
      label: 'Light',
      description: 'Clean bright layout with high contrast dark text',
      icon: Sun,
    },
    {
      mode: 'dark',
      label: 'Dark',
      description: 'Deep slate background with reduced glare for low-light environments',
      icon: Moon,
    },
    {
      mode: 'system',
      label: 'System',
      description: 'Automatically synchronizes with your operating system preferences',
      icon: Laptop,
    },
  ];

  if (variant === 'detailed') {
    return (
      <div className={`space-y-3 ${className}`}>
        <div
          role="radiogroup"
          aria-label="Theme mode selection"
          className="grid grid-cols-1 sm:grid-cols-3 gap-3"
        >
          {options.map((opt) => {
            const Icon = opt.icon;
            const isSelected = theme === opt.mode;

            return (
              <button
                key={opt.mode}
                type="button"
                role="radio"
                aria-checked={isSelected}
                onClick={() => setTheme(opt.mode)}
                className={`p-4 rounded-xl border text-left transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-500 ${
                  isSelected
                    ? 'bg-sky-50 dark:bg-sky-950/40 border-sky-500 text-sky-900 dark:text-sky-200 shadow-md shadow-sky-500/10'
                    : 'bg-white dark:bg-slate-950/60 border-slate-200 dark:border-slate-800 text-slate-700 dark:text-slate-300 hover:border-slate-300 dark:hover:border-slate-700'
                }`}
              >
                <div className="flex items-center justify-between mb-2">
                  <div
                    className={`p-2 rounded-lg ${
                      isSelected
                        ? 'bg-sky-500 text-white'
                        : 'bg-slate-100 dark:bg-slate-800 text-slate-500 dark:text-slate-400'
                    }`}
                  >
                    <Icon className="w-4 h-4" />
                  </div>
                  {isSelected && (
                    <span className="text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full bg-sky-500/20 text-sky-600 dark:text-sky-400 border border-sky-500/30">
                      Active
                    </span>
                  )}
                </div>
                <h4 className="text-sm font-bold">{opt.label}</h4>
                <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 leading-relaxed">
                  {opt.description}
                </p>
                {opt.mode === 'system' && isSelected && (
                  <p className="text-[11px] font-medium text-sky-600 dark:text-sky-400 mt-2">
                    Resolved: {resolvedTheme === 'dark' ? 'Dark Mode' : 'Light Mode'}
                  </p>
                )}
              </button>
            );
          })}
        </div>
      </div>
    );
  }

  // Compact variant for Navbar header
  return (
    <div
      role="radiogroup"
      aria-label="Theme mode selection"
      className={`inline-flex items-center p-1 bg-slate-100 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700/80 rounded-lg ${className}`}
    >
      {options.map((opt) => {
        const Icon = opt.icon;
        const isSelected = theme === opt.mode;

        return (
          <button
            key={opt.mode}
            type="button"
            role="radio"
            aria-checked={isSelected}
            onClick={() => setTheme(opt.mode)}
            title={`Switch to ${opt.label} theme${opt.mode === 'system' ? ` (${resolvedTheme} active)` : ''}`}
            aria-label={`${opt.label} theme`}
            className={`p-1.5 rounded-md text-xs font-medium transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-500 ${
              isSelected
                ? 'bg-white dark:bg-slate-900 text-sky-600 dark:text-sky-400 shadow-sm'
                : 'text-slate-500 dark:text-slate-400 hover:text-slate-800 dark:hover:text-slate-200'
            }`}
          >
            <Icon className="w-3.5 h-3.5" />
          </button>
        );
      })}
    </div>
  );
};
