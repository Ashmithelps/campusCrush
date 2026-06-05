import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import { apiError } from '../services/api';
import Logo from '../components/Logo';
import { isValidEmail } from '../utils/auth';

const Atmosphere = () => (
    <>
        <svg className="splash-grain" aria-hidden="true" xmlns="http://www.w3.org/2000/svg">
            <filter id="lg-noise">
                <feTurbulence type="fractalNoise" baseFrequency="0.68" numOctaves="3" stitchTiles="stitch" />
                <feColorMatrix type="saturate" values="0" />
            </filter>
            <rect width="100%" height="100%" filter="url(#lg-noise)" />
        </svg>
        <div className="splash-glow"     aria-hidden="true" />
        <div className="splash-vignette" aria-hidden="true" />
    </>
);

const Login = () => {
    const navigate = useNavigate();
    const { login } = useAuth();
    const { theme, toggleTheme } = useTheme();

    const [email,       setEmail]       = useState('');
    const [emailStatus, setEmailStatus] = useState('idle'); // idle|valid|error
    const [error,       setError]       = useState('');
    const [loading,     setLoading]     = useState(false);

    const handleEmailChange = (e) => {
        const val = e.target.value;
        setEmail(val);
        setError('');
        if (isValidEmail(val)) {
            setEmailStatus('valid');
        } else {
            setEmailStatus(prev => prev === 'error' && val.trim() ? 'error' : 'idle');
        }
    };

    const handleEmailBlur = () => {
        if (email.trim()) setEmailStatus(isValidEmail(email) ? 'valid' : 'error');
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!isValidEmail(email)) { setEmailStatus('error'); return; }
        setError('');
        setLoading(true);
        try {
            await login(email.toLowerCase().trim());
            navigate('/verify', { state: { email: email.toLowerCase().trim(), flow: 'login' } });
        } catch (err) {
            setError(apiError(err, "Couldn't send a code — try signing up if you're new."));
        } finally {
            setLoading(false);
        }
    };

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

            <div className="rg-body rg-body--login">
                <div className="rg-mark">
                    <Logo size={36} settled />
                </div>

                <p className="rg-copy">You're back.</p>

                <form className="rg-form" onSubmit={handleSubmit} noValidate>
                    <div className={fieldClass}>
                        <input
                            id="lg-email"
                            type="email"
                            inputMode="email"
                            autoComplete="email"
                            placeholder=" "
                            value={email}
                            onChange={handleEmailChange}
                            onBlur={handleEmailBlur}
                            aria-describedby={emailStatus === 'error' ? 'lg-email-err' : 'lg-micro'}
                            aria-invalid={emailStatus === 'error' ? 'true' : undefined}
                            autoFocus
                        />
                        <label htmlFor="lg-email">College email</label>

                        {emailStatus === 'valid' && (
                            <span className="rg-field-icon rg-field-ok" aria-hidden="true">✓</span>
                        )}
                    </div>

                    {emailStatus === 'error' && (
                        <p className="rg-field-err" id="lg-email-err" role="alert">
                            Only @cuchd.in emails are accepted
                        </p>
                    )}

                    {error && <p className="rg-error" role="alert">{error}</p>}

                    <p className="rg-micro" id="lg-micro">@cuchd.in · students only</p>

                    <button
                        className="rg-btn-primary"
                        type="submit"
                        disabled={loading || emailStatus !== 'valid'}
                    >
                        {loading ? 'Sending code…' : 'Continue'}
                    </button>
                </form>

                <p className="rg-link">
                    New to Unsaid?{' '}
                    <button type="button" onClick={() => navigate('/register')}>Sign up</button>
                </p>
            </div>
        </div>
    );
};

export default Login;
