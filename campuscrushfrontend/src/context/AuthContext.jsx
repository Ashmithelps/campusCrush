import { createContext, useState, useContext, useEffect } from 'react';
import api from '../services/api';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [token, setToken] = useState(localStorage.getItem('token'));
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (token) {
            api.get('/me')
                .then(res => setUser(res.data))
                .catch(() => {
                    localStorage.removeItem('token');
                    setToken(null);
                    setUser(null);
                })
                .finally(() => setLoading(false));
        } else {
            setLoading(false);
        }
    }, [token]);

    const login = async (email) => {
        await api.post(`/public/auth/login?email=${email}`);
    };

    const register = async (email) => {
        await api.post(`/public/auth/register?email=${email}`);
    };

    const verifyOtp = async (email, otp) => {
        const response = await api.post(`/public/auth/verify-otp?email=${email}&otp=${otp}`);
        const jwt = response.data;
        localStorage.setItem('token', jwt);
        setToken(jwt);

        const meResponse = await api.get('/me', {
            headers: { Authorization: `Bearer ${jwt}` },
        });
        setUser(meResponse.data);
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
