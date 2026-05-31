import axios from 'axios';

const api = axios.create({
    baseURL: (import.meta.env.VITE_API_URL || '') + '/api',
    headers: {
        'Content-Type': 'application/json',
    },
});

// Add a request interceptor to include JWT token
api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`; // Corrected backticks
        }
        return config;
    },
    (error) => Promise.reject(error)
);

export default api;

// Safely extract a string message from any Axios error.
// Spring Boot can return { message }, { detail }, a plain string, or nothing.
export function apiError(err, fallback = 'Something went wrong.') {
    const data = err?.response?.data;
    if (!data) return fallback;
    if (typeof data === 'string') return data || fallback;
    return data.message || data.detail || data.error || fallback;
}
