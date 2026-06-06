import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { apiError } from '../services/api';
import Logo from '../components/Logo';
import { isValidEmail } from '../utils/auth';
import AuthShell from '../layouts/AuthShell';

const Login = () => {
    const navigate = useNavigate();
    const { login } = useAuth();

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
        <AuthShell>
            <button
                className="ac-back ac-back--in"
                onClick={() => navigate('/auth')}
                aria-label="Back to sign-in options"
            >
                ←
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
        </AuthShell>
    );
};

export default Login;
