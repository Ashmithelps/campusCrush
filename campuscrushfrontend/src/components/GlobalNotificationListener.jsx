import React, { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import socketService from '../services/socket';
import Toast from './common/Toast';
import { useNavigate } from 'react-router-dom';

const GlobalNotificationListener = () => {
    const { user } = useAuth();
    const [notification, setNotification] = useState(null);
    const navigate = useNavigate();

    useEffect(() => {
        if (!user) return;

        const token = localStorage.getItem('token');
        if (token) {
            socketService.connect(token);
        }

        // Subscribe to my notifications
        // We need 'PublicId'. Is it in 'user' object? 
        // AuthContext usually stores what details?
        // Let's assume user object has publicId. If not, we might need to rely on endpoint or JWT parsing.
        // If user is just { email, ... }, we might be stuck.
        // Let's check AuthContext... assuming it has user details from some fetch or decode.

        // Wait, if user doesn't have publicId, we can't subscribe.
        // workaround: AuthContext should decode token or fetch /me.
        // Assuming user.publicId exists for now. If not, we'll debug.

        if (user.publicId) {
            console.log("GlobalListener: Subscribing to", `/topic/user/${user.publicId}/notifications`);
            socketService.subscribe(`/topic/user/${user.publicId}/notifications`, (msg) => {
                console.log("GlobalListener: Received message", msg.body);
                const payload = JSON.parse(msg.body);
                setNotification(payload);
                // Play sound?
                new Audio('/notification.mp3').play().catch(() => { }); // Optional
            });
        } else {
            console.warn("GlobalListener: No publicId found in user object", user);
        }

    }, [user]);

    if (!notification) return null;

    return (
        <Toast
            message={notification}
            onClose={() => setNotification(null)}
        />
    );
};

export default GlobalNotificationListener;
