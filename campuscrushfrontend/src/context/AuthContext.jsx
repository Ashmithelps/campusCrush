import { createContext, useState, useContext, useEffect } from 'react';
import api from '../services/api';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [token, setToken] = useState(localStorage.getItem('token'));
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (token) {
            // In a real app, you'd fetch user profile here.
            // For now, we decode or assume logged in.
            // Since backend doesn't have /me endpoint yet, we trust the token until 401.
            setUser({ authenticated: true });
        }
        setLoading(false);
    }, [token]);

    const login = async (email) => {
        // Step 1: Request OTP
        await api.post(`/public/auth/login?email=${email}`);
    };

    const register = async (email) => {
        // Step 1: Request OTP
        await api.post(`/public/auth/register?email=${email}`);
    };

    const verifyOtp = async (email, otp) => {
        // Step 2: Verify OTP and get Token
        const response = await api.post(`/public/auth/verify-otp?email=${email}&otp=${otp}`);
        const jwt = response.data;
        localStorage.setItem('token', jwt);
        setToken(jwt);
        setUser({ authenticated: true });
        return true;
    };

    const logout = () => {
        localStorage.removeItem('token');
        setToken(null);
        setUser(null);
    };

    return (
        <AuthContext.Provider value={{ user, token, login, register, verifyOtp, logout, loading }}>
            {!loading && children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => useContext(AuthContext);
