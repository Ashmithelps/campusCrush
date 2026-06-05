import React, { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme } from '../context/ThemeContext';
import Logo from '../components/Logo';

// ─── Animation config ─────────────────────────────────────────────
// Tune these values to adjust the entrance choreography.
// They are injected as CSS custom properties on mount so that
// CSS keyframes / transitions and this JS config share one source.
const DELAYS = {
  dot1:  0.20,  // s — first  ellipsis dot
  dot2:  0.40,  // s — second ellipsis dot
  dot3:  0.60,  // s — third  ellipsis dot (also starts the idle pulse)
  word:  0.82,  // s — wordmark  fade + rise + deblur
  hook1: 1.16,  // s — tagline line 1
  hook2: 1.38,  // s — tagline line 2
  btn:   1.54,  // s — BEGIN button
  ghost: 1.74,  // s — "I have an account"
};
// ─────────────────────────────────────────────────────────────────

const Splash = () => {
    const navigate  = useNavigate();
    const { theme, toggleTheme } = useTheme();
    const [show, setShow]       = useState(false);
    const [exiting, setExiting] = useState(false);
    const markRef = useRef(null);
    const wordRef = useRef(null);

    // Inject DELAYS as CSS custom properties — single source of truth
    useEffect(() => {
        const root = document.documentElement;
        Object.entries(DELAYS).forEach(([k, v]) => {
            root.style.setProperty(`--splash-${k}`, `${v}s`);
        });
        return () => {
            Object.keys(DELAYS).forEach(k => root.style.removeProperty(`--splash-${k}`));
        };
    }, []);

    // Trigger entrance (~60ms after mount so transitions have a start state)
    useEffect(() => {
        const t = setTimeout(() => setShow(true), 60);
        return () => clearTimeout(t);
    }, []);

    // Cursor parallax — logo ≤8px, wordmark ≤5px; lerp-damped
    // Disabled on touch + prefers-reduced-motion; activates after entrance (2.4s)
    useEffect(() => {
        if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
        if ('ontouchstart' in window) return;

        let rafId;
        let tx = 0, ty = 0, cx = 0, cy = 0;

        const onMove = (e) => {
            const dx = (e.clientX / window.innerWidth  - 0.5) * 2;
            const dy = (e.clientY / window.innerHeight - 0.5) * 2;
            tx = dx * 8;
            ty = dy * 8;
        };

        const tick = () => {
            cx += (tx - cx) * 0.06;
            cy += (ty - cy) * 0.06;
            if (markRef.current)
                markRef.current.style.transform = `translate(${cx.toFixed(2)}px,${cy.toFixed(2)}px)`;
            if (wordRef.current)
                wordRef.current.style.transform = `translate(${(cx*0.6).toFixed(2)}px,${(cy*0.6).toFixed(2)}px)`;
            rafId = requestAnimationFrame(tick);
        };

        const timer = setTimeout(() => {
            window.addEventListener('pointermove', onMove, { passive: true });
            rafId = requestAnimationFrame(tick);
        }, 2400);

        return () => {
            clearTimeout(timer);
            window.removeEventListener('pointermove', onMove);
            cancelAnimationFrame(rafId);
        };
    }, []);

    // Exit: fade out, then navigate
    const handleBegin = () => {
        setExiting(true);
        setTimeout(() => navigate('/auth'), 340);
    };

    const bodyClass = [
        'splash-body',
        show    ? 'splash-body--in'  : '',
        exiting ? 'splash-body--out' : '',
    ].filter(Boolean).join(' ');

    return (
        <div className="splash-page">

            <svg className="splash-grain" aria-hidden="true" xmlns="http://www.w3.org/2000/svg">
                <filter id="splash-noise">
                    <feTurbulence type="fractalNoise" baseFrequency="0.68" numOctaves="3" stitchTiles="stitch" />
                    <feColorMatrix type="saturate" values="0" />
                </filter>
                <rect width="100%" height="100%" filter="url(#splash-noise)" />
            </svg>

            <div className="splash-glow" aria-hidden="true" />
            <div className="splash-vignette" aria-hidden="true" />

            <button
                className="splash-theme-toggle"
                onClick={toggleTheme}
                aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}
            >
                {theme === 'dark' ? 'Light' : 'Dark'}
            </button>

            <div className={bodyClass}>

                <div className="splash-mark" ref={markRef}>
                    <Logo size={48} />
                </div>

                <div ref={wordRef} style={{ willChange: 'transform' }}>
                    <h1 className="splash-title">Unsaid</h1>
                </div>

                <p className="splash-hook" aria-label="You had the feeling. We gave it somewhere to go.">
                    <span className="splash-hook-1">You had the feeling.</span>
                    <span className="splash-hook-2">We gave it somewhere to go.</span>
                </p>

                <div className="splash-actions">
                    <button className="splash-btn-primary" onClick={handleBegin}>
                        Begin
                    </button>
                    <button className="splash-btn-ghost" onClick={() => navigate('/login')}>
                        I have an account
                    </button>
                </div>

            </div>
        </div>
    );
};

export default Splash;
