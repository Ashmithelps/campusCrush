import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { apiError } from '../services/api';
import Logo from '../components/Logo';
import { isValidEmail } from '../utils/auth';
import AuthShell from '../layouts/AuthShell';

const Register = () => {
    const navigate = useNavigate();
    const { register } = useAuth();

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
            await register(email.toLowerCase().trim());
            navigate('/verify', { state: { email: email.toLowerCase().trim(), flow: 'register' } });
        } catch (err) {
            setError(apiError(err, 'Failed to send OTP.'));
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

            <div className="rg-body">
                <div className="rg-mark">
                    <Logo size={36} settled />
                </div>

                <p className="rg-copy">
                    You're nearly<br />in.
                </p>

                <form className="rg-form" onSubmit={handleSubmit} noValidate>
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
                            aria-describedby={emailStatus === 'error' ? 'rg-email-err' : 'rg-micro'}
                            aria-invalid={emailStatus === 'error' ? 'true' : undefined}
                            autoFocus
                        />
                        <label htmlFor="rg-email">College email</label>

                        {emailStatus === 'valid' && (
                            <span className="rg-field-icon rg-field-ok" aria-hidden="true">✓</span>
                        )}
                    </div>

                    {emailStatus === 'error' && (
                        <p className="rg-field-err" id="rg-email-err" role="alert">
                            Only @cuchd.in emails are accepted
                        </p>
                    )}

                    {error && <p className="rg-error" role="alert">{error}</p>}

                    <p className="rg-micro" id="rg-micro">Students only · @cuchd.in</p>

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
                    <button type="button" onClick={() => navigate('/login')}>Log in</button>
                </p>
            </div>
        </AuthShell>
    );
};

export default Register;
