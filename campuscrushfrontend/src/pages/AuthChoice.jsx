import React, { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme } from '../context/ThemeContext';
import Logo from '../components/Logo';

const AuthChoice = () => {
    const navigate = useNavigate();
    const { theme, toggleTheme } = useTheme();
    const [show, setShow] = useState(false);
    const markRef = useRef(null);

    // Entrance trigger
    useEffect(() => {
        const t = setTimeout(() => setShow(true), 60);
        return () => clearTimeout(t);
    }, []);

    // Cursor parallax at reduced amplitude (≤5px vs landing's ≤8px)
    // The mark feels "settled" — less eager to drift
    useEffect(() => {
        if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
        if ('ontouchstart' in window) return;

        let rafId;
        let tx = 0, ty = 0, cx = 0, cy = 0;

        const onMove = (e) => {
            const dx = (e.clientX / window.innerWidth  - 0.5) * 2;
            const dy = (e.clientY / window.innerHeight - 0.5) * 2;
            tx = dx * 5;
            ty = dy * 5;
        };

        const tick = () => {
            cx += (tx - cx) * 0.05;
            cy += (ty - cy) * 0.05;
            if (markRef.current)
                markRef.current.style.transform = `translate(${cx.toFixed(2)}px,${cy.toFixed(2)}px)`;
            rafId = requestAnimationFrame(tick);
        };

        const timer = setTimeout(() => {
            window.addEventListener('pointermove', onMove, { passive: true });
            rafId = requestAnimationFrame(tick);
        }, 1400);

        return () => {
            clearTimeout(timer);
            window.removeEventListener('pointermove', onMove);
            cancelAnimationFrame(rafId);
        };
    }, []);

    const bodyClass = `ac-body${show ? ' ac-body--in' : ''}`;

    return (
        <div className="ac-page">

            {/* Same atmospheric layers as landing */}
            <svg className="splash-grain" aria-hidden="true" xmlns="http://www.w3.org/2000/svg">
                <filter id="ac-noise">
                    <feTurbulence type="fractalNoise" baseFrequency="0.68" numOctaves="3" stitchTiles="stitch" />
                    <feColorMatrix type="saturate" values="0" />
                </filter>
                <rect width="100%" height="100%" filter="url(#ac-noise)" />
            </svg>
            <div className="splash-glow"    aria-hidden="true" />
            <div className="splash-vignette" aria-hidden="true" />

            {/* Back affordance */}
            <button
                className={`ac-back${show ? ' ac-back--in' : ''}`}
                onClick={() => navigate('/')}
                aria-label="Back to home"
            >
                ←
            </button>

            {/* Theme toggle */}
            <button
                className="splash-theme-toggle"
                onClick={toggleTheme}
                aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}
            >
                {theme === 'dark' ? 'Light' : 'Dark'}
            </button>

            <div className={bodyClass}>

                {/* Settled logo — arrives from landing's larger size */}
                <div className="ac-mark" ref={markRef}>
                    <Logo size={36} settled />
                </div>

                {/* Brand copy */}
                <p className="ac-copy">
                    For the feeling<br />you've been carrying.
                </p>

                {/* Auth options */}
                <div className="ac-actions">
                    <button
                        className="ac-btn-primary"
                        onClick={() => navigate('/register')}
                    >
                        Create account
                    </button>
                    <button
                        className="ac-btn-secondary"
                        onClick={() => navigate('/login')}
                    >
                        Log in
                    </button>
                </div>

                {/* Gate hint */}
                <p className="ac-micro">Students only · @cuchd.in</p>

            </div>
        </div>
    );
};

export default AuthChoice;
