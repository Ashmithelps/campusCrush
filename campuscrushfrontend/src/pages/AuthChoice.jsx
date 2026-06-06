import React, { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Logo from '../components/Logo';
import AuthShell from '../layouts/AuthShell';

const AuthChoice = () => {
    const navigate = useNavigate();
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
        <AuthShell pageClass="ac-page">
            <button
                className={`ac-back${show ? ' ac-back--in' : ''}`}
                onClick={() => navigate('/')}
                aria-label="Back to home"
            >
                ←
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
        </AuthShell>
    );
};

export default AuthChoice;
