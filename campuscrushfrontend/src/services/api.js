import axios from 'axios';

const api = axios.create({
    baseURL: '/api', // Proxy handles localhost:8080
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
