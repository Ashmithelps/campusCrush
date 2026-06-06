import React from 'react';
import ThemeToggle from '../components/ThemeToggle';

const Atmosphere = () => (
    <>
        <svg className="splash-grain" aria-hidden="true" xmlns="http://www.w3.org/2000/svg">
            <filter id="auth-noise">
                <feTurbulence type="fractalNoise" baseFrequency="0.68" numOctaves="3" stitchTiles="stitch" />
                <feColorMatrix type="saturate" values="0" />
            </filter>
            <rect width="100%" height="100%" filter="url(#auth-noise)" />
        </svg>
        <div className="splash-glow"     aria-hidden="true" />
        <div className="splash-vignette" aria-hidden="true" />
    </>
);

const AuthShell = ({ children, pageClass = 'rg-page' }) => (
    <div className={pageClass}>
        <Atmosphere />
        <ThemeToggle className="splash-theme-toggle" />
        {children}
    </div>
);

export default AuthShell;
