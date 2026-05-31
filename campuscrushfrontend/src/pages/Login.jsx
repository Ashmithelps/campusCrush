import React, { useState, useRef } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate, Link } from 'react-router-dom';
import { apiError } from '../services/api';

const Login = () => {
    const [email, setEmail]         = useState('');
    const [step, setStep]           = useState(1);
    const [otpDigits, setOtpDigits] = useState(['', '', '', '', '', '']);
    const [error, setError]         = useState('');
    const [loading, setLoading]     = useState(false);
    const { login, verifyOtp }      = useAuth();
    const navigate                  = useNavigate();
    const otpRefs                   = useRef([]);

    const otp = otpDigits.join('');

    const handleEmailSubmit = async (e) => {
        e.preventDefault();
        setError('');
        if (!email.toLowerCase().trim().endsWith('@cuchd.in')) {
            setError('Only @cuchd.in emails are allowed.');
            return;
        }
        setLoading(true);
        try {
            await login(email.toLowerCase().trim());
            setStep(2);
        } catch (err) {
            setError(apiError(err, 'Failed to send OTP.'));
        } finally {
            setLoading(false);
        }
    };

    const handleOtpSubmit = async (e) => {
        e.preventDefault();
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

    if (step === 2) {
        return (
            <div className="auth-page">
                <div className="auth-top">
                    <button
                        className="btn-ghost"
                        style={{ alignSelf: 'flex-start', paddingLeft: 0, marginBottom: 32 }}
                        onClick={() => { setStep(1); setOtpDigits(['','','','','','']); setError(''); }}
                    >
                        ← Back
                    </button>
                    <div className="auth-heading" style={{ fontSize: '2rem' }}>Check your inbox</div>
                    <div className="auth-sub">
                        We sent a 6-digit code to{' '}
                        <span style={{ color: 'var(--text)', fontWeight: 600 }}>{email}</span>
                    </div>

                    <div className="otp-row" onPaste={handleOtpPaste}>
                        {otpDigits.map((digit, i) => (
                            <input
                                key={i}
                                ref={el => otpRefs.current[i] = el}
                                className="otp-box"
                                type="text"
                                inputMode="numeric"
                                maxLength={1}
                                value={digit}
                                autoFocus={i === 0}
                                onChange={e => handleOtpChange(i, e.target.value)}
                                onKeyDown={e => handleOtpKeyDown(i, e)}
                            />
                        ))}
                    </div>

                    {error && <p className="error-text" style={{ marginTop: 14, textAlign: 'center' }}>{error}</p>}
                </div>

                <div className="auth-bottom">
                    <button
                        className="btn-full btn-accent"
                        onClick={handleOtpSubmit}
                        disabled={loading || otp.length < 6}
                    >
                        {loading ? 'Verifying...' : 'Verify & Login'}
                    </button>
                    <p className="link-text">
                        Didn't get it?{' '}
                        <button onClick={handleEmailSubmit} disabled={loading}>Resend code</button>
                    </p>
                </div>
            </div>
        );
    }

    return (
        <div className="auth-page">
            <div className="auth-top">
                <div className="auth-logo">campuscrush</div>
                <div className="auth-heading">Welcome back.</div>
                <div className="auth-sub">Your campus, your secrets. Sign in anonymously.</div>

                <div className="field">
                    <label>College Email</label>
                    <input
                        className="field-input"
                        type="email"
                        placeholder="you@cuchd.in"
                        value={email}
                        onChange={e => setEmail(e.target.value)}
                        autoFocus
                        onKeyDown={e => e.key === 'Enter' && handleEmailSubmit(e)}
                    />
                </div>

                {error && <p className="error-text">{error}</p>}
            </div>

            <div className="auth-bottom">
                <button
                    className="btn-full btn-accent"
                    onClick={handleEmailSubmit}
                    disabled={loading || !email}
                >
                    {loading ? 'Sending code...' : 'Continue'}
                </button>
                <p className="link-text">
                    New here? <Link to="/register">Create account</Link>
                </p>
            </div>
        </div>
    );
};

export default Login;
