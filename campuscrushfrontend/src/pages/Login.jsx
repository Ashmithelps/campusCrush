import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate, Link } from 'react-router-dom';
import Input from '../components/common/Input';
import Button from '../components/common/Button';

const Login = () => {
    const [email, setEmail] = useState('');
    const [otp, setOtp] = useState('');
    const [step, setStep] = useState(1); // 1 = Email, 2 = OTP
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const { login, verifyOtp } = useAuth();
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            if (step === 1) {
                if (!email.toLowerCase().endsWith('@cuchd.in')) {
                    setError('Only @cuchd.in emails are allowed.');
                    setLoading(false);
                    return;
                }
                await login(email);
                setStep(2); // Move to OTP step
            } else {
                await verifyOtp(email, otp);
                navigate('/dashboard');
            }
        } catch (err) {
            console.error("Login Error:", err);
            let errorMsg = 'Login failed.';
            if (err.response && err.response.data) {
                if (typeof err.response.data === 'string') {
                    errorMsg = err.response.data;
                } else if (err.response.data.message) {
                    errorMsg = err.response.data.message;
                } else if (err.response.data.error) {
                    errorMsg = err.response.data.error;
                }
            }
            setError(String(errorMsg)); // Force string to prevent crash
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="container" style={{ marginTop: '10vh' }}>
            <div className="card">
                <h2 style={{ textAlign: 'center', color: 'hsl(var(--primary))', marginBottom: '1.5rem' }}>
                    {step === 1 ? 'Welcome Back' : 'Enter OTP'}
                </h2>
                <form onSubmit={handleSubmit}>
                    {step === 1 ? (
                        <Input
                            label="College Email"
                            type="email"
                            placeholder="you@cuchd.in"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                            autoFocus
                        />
                    ) : (
                        <Input
                            label="OTP Code"
                            type="text"
                            placeholder="123456"
                            value={otp}
                            onChange={(e) => setOtp(e.target.value)}
                            required
                            autoFocus
                        />
                    )}

                    {error && <p style={{ color: 'hsl(var(--danger))', textAlign: 'center', marginBottom: '1rem' }}>{error}</p>}

                    <Button type="submit" loading={loading} style={{ width: '100%' }}>
                        {step === 1 ? 'Get OTP' : 'Verify & Login'}
                    </Button>
                </form>
                {step === 2 && (
                    <div style={{ textAlign: 'center', marginTop: '1rem' }}>
                        <button onClick={() => setStep(1)} style={{ background: 'none', border: 'none', color: 'hsl(var(--text-muted))', cursor: 'pointer', textDecoration: 'underline' }}>
                            Wrong Email?
                        </button>
                    </div>
                )}
                <div style={{ textAlign: 'center', marginTop: '1rem', color: 'hsl(var(--text-muted))' }}>
                    New here? <Link to="/register" style={{ color: 'hsl(var(--secondary))', fontWeight: 'bold' }}>Register</Link>
                </div>
            </div>
        </div>
    );
};

export default Login;
