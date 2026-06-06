import React from 'react';
import { useTheme } from '../context/ThemeContext';

const ThemeToggle = ({ className = 'btn-theme' }) => {
    const { theme, toggleTheme } = useTheme();
    return (
        <button
            className={className}
            onClick={toggleTheme}
            aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}
        >
            {theme === 'dark' ? 'Light' : 'Dark'}
        </button>
    );
};

export default ThemeToggle;
