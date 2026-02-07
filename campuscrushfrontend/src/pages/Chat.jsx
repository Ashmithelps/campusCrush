import React, { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../services/api';
import socketService from '../services/socket';
import Button from '../components/common/Button';
import Input from '../components/common/Input';
import { useAuth } from '../context/AuthContext';

const Chat = () => {
    const { confessionId } = useParams();
    const navigate = useNavigate();
    const { user } = useAuth();
    const [messages, setMessages] = useState([]);
    const [inputText, setInputText] = useState('');
    const [loading, setLoading] = useState(true);
    const [isSender, setIsSender] = useState(false);
    const [confessionState, setConfessionState] = useState(null);
    const [isRevealed, setIsRevealed] = useState(false);
    const messagesEndRef = useRef(null);
    const subscriptionRef = useRef(null);

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    };

    const fetchMessages = async () => {
        try {
            const confResponse = await api.get('/confessions');
            const myConf = confResponse.data.find(c => c.id.toString() === confessionId);

            if (myConf) {
                setConfessionState(myConf.state);
                setIsSender(myConf.isSender);
                setIsRevealed(myConf.isRevealed);
            }

            const response = await api.get(`/messages/${confessionId}`);
            setMessages(response.data);
            scrollToBottom();
        } catch (error) {
            console.error("Failed to fetch messages", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchMessages();
        api.post(`/confessions/${confessionId}/read`);

        const token = localStorage.getItem('token');
        if (token) {
            socketService.connect(token);
            socketService.subscribe(`/topic/confession/${confessionId}`, (message) => {
                // If it's a REVEAL type, we should re-fetch to update isRevealed state too
                fetchMessages();
                api.post(`/confessions/${confessionId}/read`);
            }).then(sub => {
                subscriptionRef.current = sub;
            });
        }

        return () => {
            if (subscriptionRef.current) {
                subscriptionRef.current.unsubscribe();
            }
        };
    }, [confessionId]);

    useEffect(() => {
        scrollToBottom();
    }, [messages]);

    const handleSend = async (e) => {
        e.preventDefault();
        if (!inputText.trim()) return;

        try {
            socketService.sendMessage(confessionId, inputText);
            setInputText('');
        } catch (error) {
            console.error("Failed to send", error);
        }
    };

    const handleReply = async () => {
        try {
            await api.post(`/confessions/${confessionId}/reply`);
            fetchMessages();
        } catch (e) {
            alert("Error: " + (e.response?.data?.message || e.message));
        }
    };

    const handleReveal = async () => {
        if (!window.confirm("Are you sure you want to reveal your identity? This cannot be undone.")) return;
        try {
            await api.post(`/confessions/${confessionId}/reveal`);
            setIsRevealed(true); // Optimistic update
        } catch (e) {
            alert("Error: " + (e.response?.data?.message || e.message));
        }
    };

    return (
        <div className="container" style={{ height: '100vh', display: 'flex', flexDirection: 'column' }}>
            <header style={{ padding: '1rem', borderBottom: '1px solid #eee', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div style={{ display: 'flex', alignItems: 'center' }}>
                    <Button variant="text" onClick={() => navigate('/dashboard')} style={{ marginRight: '1rem' }}>
                        &larr; Back
                    </Button>
                    <h3 style={{ margin: 0, color: 'hsl(var(--primary))' }}>Chat</h3>
                </div>
                {/* Reveal Button for Sender */}
                {isSender && !isRevealed && confessionState !== 'CREATED' && (
                    <Button
                        variant="secondary"
                        onClick={handleReveal}
                        style={{ fontSize: '0.8rem', padding: '0.25rem 0.75rem', border: '1px solid hsl(var(--primary))', color: 'hsl(var(--primary))' }}
                    >
                        Reveal Identity 🎭
                    </Button>
                )}
            </header>

            <div style={{ flex: 1, overflowY: 'auto', padding: '1rem', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                {loading && <p>Loading chat...</p>}

                {confessionState === 'CREATED' && !loading && (
                    <div style={{ textAlign: 'center', color: '#888', margin: '1rem 0', padding: '1rem', background: '#fff0f5', borderRadius: '8px', border: '1px solid #ffd1dc' }}>
                        {!isSender ? (
                            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '0.5rem' }}>
                                <p style={{ margin: 0, fontWeight: '500', color: 'hsl(var(--primary))' }}>This confession is pending your reply.</p>
                                <Button onClick={handleReply} style={{ marginTop: '0.5rem' }}>Reply & Unlock</Button>
                            </div>
                        ) : (
                            <p style={{ fontSize: '0.9rem', fontStyle: 'italic', margin: 0 }}>Waiting for them to accept...</p>
                        )}
                    </div>
                )}

                {messages.map((msg) => {
                    const isSelf = msg.from === 'SELF';

                    if (msg.type === 'REVEAL') {
                        return (
                            <div key={msg.id} style={{ textAlign: 'center', margin: '1rem 0' }}>
                                <div style={{
                                    display: 'inline-block',
                                    padding: '0.5rem 1rem',
                                    borderRadius: '2rem',
                                    background: 'linear-gradient(135deg, #FFD700 0%, #FDB931 100%)',
                                    color: '#5c4000',
                                    fontSize: '0.85rem',
                                    fontWeight: 'bold',
                                    boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
                                }}>
                                    {msg.content}
                                </div>
                            </div>
                        );
                    }

                    return (
                        <div
                            key={msg.id}
                            style={{
                                alignSelf: isSelf ? 'flex-end' : 'flex-start',
                                maxWidth: '75%',
                                padding: '1rem 1.25rem',
                                borderRadius: isSelf ? '1.25rem 1.25rem 0 1.25rem' : '1.25rem 1.25rem 1.25rem 0',
                                backgroundColor: isSelf ? 'hsl(var(--primary))' : 'white',
                                color: isSelf ? 'white' : 'hsl(var(--text-main))',
                                boxShadow: '0 2px 4px rgba(0,0,0,0.05)',
                                fontSize: '0.95rem',
                                position: 'relative',
                                border: isSelf ? 'none' : '1px solid rgba(0,0,0,0.05)',
                                marginBottom: '0.5rem',
                            }}
                        >
                            {msg.content}
                            <div style={{
                                fontSize: '0.65rem',
                                opacity: 0.8,
                                marginTop: '0.25rem',
                                textAlign: 'right',
                                color: isSelf ? 'rgba(255,255,255,0.9)' : 'hsl(var(--text-muted))'
                            }}>
                                {new Date(msg.sentAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                            </div>
                        </div>
                    );
                })}
                <div ref={messagesEndRef} />
            </div>

            <form onSubmit={handleSend} style={{ padding: '1rem', borderTop: '1px solid #eee', backgroundColor: 'white', display: 'flex', gap: '0.5rem' }}>
                <input
                    className="input"
                    style={{ marginBottom: 0 }}
                    value={inputText}
                    onChange={(e) => setInputText(e.target.value)}
                    placeholder={confessionState === 'CREATED' ? "Waiting for reply..." : "Type a message..."}
                    disabled={confessionState === 'CREATED'}
                />
                <Button type="submit" disabled={confessionState === 'CREATED'}>Send</Button>
            </form>
        </div>
    );
};

export default Chat;
