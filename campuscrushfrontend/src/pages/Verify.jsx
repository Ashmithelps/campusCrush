import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { apiError } from '../services/api';
import Logo from '../components/Logo';
import SegmentedInput from '../components/SegmentedInput';

const RESEND_COOLDOWN  = 60;
const AUTO_SUBMIT_MS   = 400;
const SUCCESS_HOLD_MS  = 820;
const EXIT_MS          = 340;

const Atmosphere = () => (
    <>
        <svg className="splash-grain" aria-hidden="true" xmlns="http://www.w3.org/2000/svg">
            <filter id="vf-noise">
                <feTurbulence type="fractalNoise" baseFrequency="0.68" numOctaves="3" stitchTiles="stitch" />
                <feColorMatrix type="saturate" values="0" />
            </filter>
            <rect width="100%" height="100%" filter="url(#vf-noise)" />
        </svg>
        <div className="splash-glow"     aria-hidden="true" />
        <div className="splash-vignette" aria-hidden="true" />
    </>
);

const Verify = () => {
    const navigate  = useNavigate();
    const location  = useLocation();
    const { verifyOtp, login, register } = useAuth();

    const { email, flow } = location.state || {};

    useEffect(() => {
        if (!email || !flow) navigate('/auth', { replace: true });
    }, [email, flow, navigate]);

    const [digits,        setDigits]        = useState(Array(6).fill(''));
    const [status,        setStatus]        = useState('idle'); // idle|verifying|error|success
    const [errorMsg,      setErrorMsg]      = useState('');
    const [exiting,       setExiting]       = useState(false);
    const [resendCooldown, setResendCooldown] = useState(RESEND_COOLDOWN);
    const [resendPhase,   setResendPhase]   = useState('cooling'); // cooling|ready|sending

    const inputRef      = useRef(null);
    const autoSubmitRef = useRef(null);
    const cooldownRef   = useRef(null);

    // ── Countdown ──────────────────────────────────────────

    useEffect(() => {
        if (resendPhase !== 'cooling') return;
        if (resendCooldown <= 0) { setResendPhase('ready'); return; }
        cooldownRef.current = setTimeout(() => setResendCooldown(c => c - 1), 1000);
        return () => clearTimeout(cooldownRef.current);
    }, [resendCooldown, resendPhase]);

    // ── Verify ─────────────────────────────────────────────

    const doVerify = useCallback(async (code) => {
        if (code.length < 6 || status === 'verifying' || status === 'success') return;
        clearTimeout(autoSubmitRef.current);
        setStatus('verifying');
        setErrorMsg('');
        try {
            await verifyOtp(email.toLowerCase().trim(), code);
            setStatus('success');
            setTimeout(() => {
                setExiting(true);
                setTimeout(() => navigate('/dashboard', { replace: true }), EXIT_MS);
            }, SUCCESS_HOLD_MS);
        } catch (err) {
            setStatus('error');
            setErrorMsg(apiError(err, 'Wrong or expired code — try again.'));
            // Shake plays for ~420ms; then clear boxes and refocus
            setTimeout(() => {
                setDigits(Array(6).fill(''));
                setStatus('idle');
                inputRef.current?.focus(0);
            }, 620);
        }
    }, [email, navigate, status, verifyOtp]);

    // ── Auto-submit ────────────────────────────────────────

    const handleComplete = useCallback((code) => {
        clearTimeout(autoSubmitRef.current);
        autoSubmitRef.current = setTimeout(() => doVerify(code), AUTO_SUBMIT_MS);
    }, [doVerify]);

    const handleDigitsChange = (next) => {
        clearTimeout(autoSubmitRef.current);
        setDigits(next);
        // Clear visible error as soon as the user starts re-entering
        if (errorMsg) setErrorMsg('');
    };

    // ── Resend ─────────────────────────────────────────────

    const handleResend = async () => {
        if (resendPhase !== 'ready') return;
        setResendPhase('sending');
        try {
            if (flow === 'register') await register(email.toLowerCase().trim());
            else await login(email.toLowerCase().trim());
        } catch {
            // Server-side 60s guard will surface; reset the local timer regardless
        } finally {
            setResendCooldown(RESEND_COOLDOWN);
            setResendPhase('cooling');
        }
    };

    const handleManualSubmit = () => {
        clearTimeout(autoSubmitRef.current);
        doVerify(digits.join(''));
    };

    const handleBack = () => navigate(flow === 'register' ? '/register' : '/login');

    const otp = digits.join('');

    if (!email || !flow) return null;

    return (
        <div className={`rg-page${exiting ? ' vf-page--out' : ''}`}>
            <Atmosphere />

            <button
                className="ac-back ac-back--in"
                onClick={handleBack}
                aria-label="Back to email"
            >
                ←
            </button>

            <div className="rg-body vf-body">
                {/* Logo — persists settled; pulses on success */}
                <div className={`rg-mark${status === 'success' ? ' vf-mark--pulse' : ''}`}>
                    <Logo size={36} settled />
                </div>

                <div className="vf-content">
                    <p className="vf-heading">Check your inbox</p>
                    <p className="vf-sub">
                        Sent to{' '}
                        <button
                            className="vf-email-btn"
                            onClick={handleBack}
                            aria-label={`Change email — currently ${email}`}
                        >
                            {email}
                        </button>
                    </p>

                    {/* Screen-reader live region for state changes */}
                    <span className="vf-sr" aria-live="assertive" aria-atomic="true">
                        {status === 'verifying' ? 'Verifying your code…' : ''}
                        {status === 'error'     ? errorMsg             : ''}
                        {status === 'success'   ? 'Code accepted. Welcome.' : ''}
                    </span>

                    <SegmentedInput
                        ref={inputRef}
                        value={digits}
                        onChange={handleDigitsChange}
                        onComplete={handleComplete}
                        status={status}
                        disabled={status === 'verifying' || status === 'success'}
                    />

                    {/* Reserve space so error text causes no layout shift */}
                    <p className="vf-error-slot" role="alert" aria-live="polite">
                        {errorMsg || ' '}
                    </p>

                    <button
                        className="rg-btn-primary"
                        onClick={handleManualSubmit}
                        disabled={otp.length < 6 || status === 'verifying' || status === 'success'}
                        aria-busy={status === 'verifying' ? 'true' : undefined}
                    >
                        {status === 'verifying' ? (
                            <span className="vf-dots" aria-hidden="true">
                                <span /><span /><span />
                            </span>
                        ) : status === 'success' ? (
                            flow === 'register' ? 'Welcome' : "You’re in"
                        ) : (
                            flow === 'register' ? 'Verify & join' : 'Sign in'
                        )}
                    </button>

                    <p className="vf-resend">
                        {resendPhase === 'cooling' && (
                            <span>Resend in {resendCooldown}s</span>
                        )}
                        {resendPhase === 'sending' && (
                            <span>Sending…</span>
                        )}
                        {resendPhase === 'ready' && (
                            <button type="button" onClick={handleResend}>
                                Resend code
                            </button>
                        )}
                    </p>
                </div>
            </div>
        </div>
    );
};

export default Verify;
