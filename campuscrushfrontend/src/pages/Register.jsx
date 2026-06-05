import React, { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import { apiError } from '../services/api';
import Logo from '../components/Logo';
import { isValidEmail } from '../utils/auth';

// Atmosphere layers — shared between both steps of this page
const Atmosphere = () => (
    <>
        <svg className="splash-grain" aria-hidden="true" xmlns="http://www.w3.org/2000/svg">
            <filter id="rg-noise">
                <feTurbulence type="fractalNoise" baseFrequency="0.68" numOctaves="3" stitchTiles="stitch" />
                <feColorMatrix type="saturate" values="0" />
            </filter>
            <rect width="100%" height="100%" filter="url(#rg-noise)" />
        </svg>
        <div className="splash-glow" aria-hidden="true" />
        <div className="splash-vignette" aria-hidden="true" />
    </>
);

const Register = () => {
    const navigate = useNavigate();
    const { register, verifyOtp } = useAuth();
    const { theme, toggleTheme } = useTheme();

    const [email, setEmail]           = useState('');
    const [emailStatus, setEmailStatus] = useState('idle'); // 'idle' | 'valid' | 'error'
    const [step, setStep]             = useState(1);
    const [otpDigits, setOtpDigits]   = useState(['', '', '', '', '', '']);
    const [error, setError]           = useState('');
    const [loading, setLoading]       = useState(false);
    const otpRefs = useRef([]);

    const otp = otpDigits.join('');

    // ── Email validation ──────────────────────────────────

    const handleEmailChange = (e) => {
        const val = e.target.value;
        setEmail(val);
        setError('');
        if (isValidEmail(val)) {
            setEmailStatus('valid');
        } else {
            // Keep showing error live only if user already blurred; otherwise wait
            setEmailStatus(prev => prev === 'error' && val.trim() ? 'error' : 'idle');
        }
    };

    const handleEmailBlur = () => {
        if (email.trim()) setEmailStatus(isValidEmail(email) ? 'valid' : 'error');
    };

    // ── OTP send ──────────────────────────────────────────

    const sendOtp = async () => {
        setError('');
        setLoading(true);
        try {
            await register(email.toLowerCase().trim());
            setStep(2);
        } catch (err) {
            setError(apiError(err, 'Failed to send OTP.'));
        } finally {
            setLoading(false);
        }
    };

    const handleEmailSubmit = (e) => {
        e.preventDefault();
        if (!isValidEmail(email)) { setEmailStatus('error'); return; }
        sendOtp();
    };

    // ── OTP verify ────────────────────────────────────────

    const handleOtpSubmit = async (e) => {
        e?.preventDefault();
        if (otp.length < 6) { setError('Enter the 6-digit code.'); return; }
        setError('');
        setLoading(true);
        try {
            await verifyOtp(email.toLowerCase().trim(), otp);
            navigate('/dashboard');
        } catch (err) {
            setError(apiError(err, 'Invalid or expired OTP.'));
            setOtpDigits(['', '', '', '', '', '']);
            otpRefs.current[0]?.focus();
        } finally {
            setLoading(false);
        }
    };

    const handleOtpChange = (index, value) => {
        if (!/^\d*$/.test(value)) return;
        const next = [...otpDigits];
        next[index] = value.slice(-1);
        setOtpDigits(next);
        if (value && index < 5) otpRefs.current[index + 1]?.focus();
    };

    const handleOtpKeyDown = (index, e) => {
        if (e.key === 'Backspace' && !otpDigits[index] && index > 0) {
            otpRefs.current[index - 1]?.focus();
        }
    };

    const handleOtpPaste = (e) => {
        const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6);
        if (pasted.length === 6) {
            setOtpDigits(pasted.split(''));
            otpRefs.current[5]?.focus();
        }
        e.preventDefault();
    };

    // ── Step 2 — OTP entry ────────────────────────────────

    if (step === 2) {
        return (
            <div className="rg-page">
                <Atmosphere />

                <button
                    className="ac-back ac-back--in"
                    onClick={() => { setStep(1); setOtpDigits(['','','','','','']); setError(''); }}
                    aria-label="Back to email"
                >
                    ←
                </button>

                <button
                    className="splash-theme-toggle"
                    onClick={toggleTheme}
                    aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}
                >
                    {theme === 'dark' ? 'Light' : 'Dark'}
                </button>

                <div className="rg-body">
                    {/* Logo persists — no re-entrance, dot-3 still breathes */}
                    <div className="rg-mark">
                        <Logo size={36} settled />
                    </div>

                    {/* OTP content fades in as a new element */}
                    <div className="rg-otp-content">
                        <p className="rg-otp-heading">Check your inbox</p>
                        <p className="rg-otp-sub">
                            Code sent to{' '}
                            <span className="rg-otp-email">{email}</span>
                        </p>

                        <div
                            className="rg-otp-row"
                            onPaste={handleOtpPaste}
                            role="group"
                            aria-label="6-digit verification code"
                        >
                            {otpDigits.map((digit, i) => (
                                <input
                                    key={i}
                                    ref={el => otpRefs.current[i] = el}
                                    className="rg-otp-box"
                                    type="text"
                                    inputMode="numeric"
                                    maxLength={1}
                                    value={digit}
                                    autoFocus={i === 0}
                                    onChange={e => handleOtpChange(i, e.target.value)}
                                    onKeyDown={e => handleOtpKeyDown(i, e)}
                                    aria-label={`Digit ${i + 1}`}
                                />
                            ))}
                        </div>

                        {error && <p className="rg-error" role="alert">{error}</p>}

                        <button
                            className="rg-btn-primary"
                            onClick={handleOtpSubmit}
                            disabled={loading || otp.length < 6}
                        >
                            {loading ? 'Verifying…' : 'Verify & Join'}
                        </button>

                        <p className="rg-link">
                            Didn't get it?{' '}
                            <button type="button" onClick={sendOtp} disabled={loading}>
                                Resend code
                            </button>
                        </p>
                    </div>
                </div>
            </div>
        );
    }

    // ── Step 1 — Email entry ──────────────────────────────

    const fieldClass = [
        'rg-field',
        emailStatus === 'valid' ? 'rg-field--valid' : '',
        emailStatus === 'error' ? 'rg-field--error' : '',
    ].filter(Boolean).join(' ');

    return (
        <div className="rg-page">
            <Atmosphere />

            <button
                className="ac-back ac-back--in"
                onClick={() => navigate('/auth')}
                aria-label="Back to sign-in options"
            >
                ←
            </button>

            <button
                className="splash-theme-toggle"
                onClick={toggleTheme}
                aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}
            >
                {theme === 'dark' ? 'Light' : 'Dark'}
            </button>

            <div className="rg-body">
                <div className="rg-mark">
                    <Logo size={36} settled />
                </div>

                <p className="rg-copy">
                    You're nearly<br />in.
                </p>

                <form className="rg-form" onSubmit={handleEmailSubmit} noValidate>
                    <div className={fieldClass}>
                        <input
                            id="rg-email"
                            type="email"
                            inputMode="email"
                            autoComplete="email"
                            placeholder=" "
                            value={email}
                            onChange={handleEmailChange}
                            onBlur={handleEmailBlur}
                            aria-describedby={
                                emailStatus === 'error' ? 'rg-email-err' : 'rg-micro'
                            }
                            aria-invalid={emailStatus === 'error' ? 'true' : undefined}
                            autoFocus
                        />
                        <label htmlFor="rg-email">College email</label>

                        {emailStatus === 'valid' && (
                            <span className="rg-field-icon rg-field-ok" aria-hidden="true">
                                ✓
                            </span>
                        )}
                    </div>

                    {emailStatus === 'error' && (
                        <p className="rg-field-err" id="rg-email-err" role="alert">
                            Only @cuchd.in emails are accepted
                        </p>
                    )}

                    {error && <p className="rg-error" role="alert">{error}</p>}

                    <p className="rg-micro" id="rg-micro">
                        Students only · @cuchd.in
                    </p>

                    <button
                        className="rg-btn-primary"
                        type="submit"
                        disabled={loading || emailStatus !== 'valid'}
                    >
                        {loading ? 'Sending code…' : 'Continue'}
                    </button>
                </form>

                <p className="rg-link">
                    Already registered?{' '}
                    <button type="button" onClick={() => navigate('/login')}>
                        Log in
                    </button>
                </p>
            </div>
        </div>
    );
};

export default Register;
