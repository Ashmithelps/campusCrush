import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate, Link } from 'react-router-dom';
import Input from '../components/common/Input';
import Button from '../components/common/Button';

const Register = () => {
    const [email, setEmail] = useState('');
    const [otp, setOtp] = useState('');
    const [step, setStep] = useState(1); // 1=Email, 2=OTP
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const { register, verifyOtp } = useAuth();
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
                await register(email);
                setStep(2);
            } else {
                await verifyOtp(email, otp);
                // After success, go to dashboard directly (auto-login)
                navigate('/dashboard');
            }
        } catch (err) {
            console.error("Registration Error:", err);
            const errorMsg = err.response?.data?.message || err.response?.data?.error || 'Registration failed.';
            setError(errorMsg);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="container" style={{ marginTop: '10vh' }}>
            <div className="card">
                <h2 style={{ textAlign: 'center', color: 'hsl(var(--secondary))', marginBottom: '1.5rem' }}>
                    {step === 1 ? 'Join CampusCrush' : 'Verify Email'}
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
                        />
                    ) : (
                        <Input
                            label="OTP Code from Email"
                            type="text"
                            placeholder="123456"
                            value={otp}
                            onChange={(e) => setOtp(e.target.value)}
                            required
                            autoFocus
                        />
                    )}

                    {error && <p style={{ color: 'hsl(var(--danger))', textAlign: 'center', marginBottom: '1rem' }}>{error}</p>}

                    <Button variant="secondary" type="submit" loading={loading} style={{ width: '100%' }}>
                        {step === 1 ? 'Register' : 'Verify & Join'}
                    </Button>
                </form>
                <div style={{ textAlign: 'center', marginTop: '1rem', color: 'hsl(var(--text-muted))' }}>
                    Already have an account? <Link to="/login" style={{ color: 'hsl(var(--primary))', fontWeight: 'bold' }}>Login</Link>
                </div>
            </div>
        </div>
    );
};

export default Register;
