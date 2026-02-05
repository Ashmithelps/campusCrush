import React, { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import Button from '../components/common/Button';
import Input from '../components/common/Input';
import Modal from '../components/common/Modal';
import api from '../services/api';
import socket from '../services/socket';
import { useNavigate } from 'react-router-dom';

const Dashboard = () => {
    const { logout, user } = useAuth();
    const [confessions, setConfessions] = useState([]);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();

    // Modal State
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [targetId, setTargetId] = useState('');
    const [message, setMessage] = useState('');
    const [sending, setSending] = useState(false);

    const fetchConfessions = async () => {
        try {
            const response = await api.get('/confessions');
            setConfessions(response.data);
        } catch (error) {
            console.error("Failed to fetch confessions", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchConfessions();

        let subscription;

        const setupSocket = async () => {
            const token = localStorage.getItem('token');
            if (token) {
                socket.connect(token);
            }

            // Subscribe to Real-Time Updates
            subscription = await socket.subscribe('/user/queue/confessions', (msg) => {
                console.log("New confession received!", msg);
                fetchConfessions(); // Instant refresh
            });
        };

        setupSocket();

        return () => {
            if (subscription) subscription.unsubscribe();
        };
    }, []);

    const handleSendConfession = async (e) => {
        e.preventDefault();
        setSending(true);
        try {
            await api.post(`/confessions/${targetId}`, message);
            alert("Confession Sent! 💘"); // Keeping this simple alert for success is okay, or could be a toast
            setIsModalOpen(false);
            setTargetId('');
            setMessage('');
            fetchConfessions();
        } catch (err) {
            alert("Failed: " + (err.response?.data?.message || err.response?.status));
        } finally {
            setSending(false);
        }
    };

    return (
        <div className="container" style={{ marginTop: '2rem' }}>
            <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
                <h1 style={{ color: 'hsl(var(--primary))' }}>Dashboard</h1>
                <Button variant="outline" onClick={logout} style={{ background: 'transparent', border: '1px solid hsl(var(--secondary))', color: 'hsl(var(--secondary))' }}>
                    Logout
                </Button>
            </header>

            <div className="card" style={{ marginBottom: '2rem' }}>
                <h3 style={{ marginBottom: '1rem' }}>Your Confessions</h3>
                {loading ? (
                    <p>Loading...</p>
                ) : confessions.length === 0 ? (
                    <p style={{ color: 'hsl(var(--text-muted))' }}>
                        No confessions yet. Send an icebreaker to get started!
                    </p>
                ) : (
                    <div style={{ display: 'grid', gap: '1rem' }}>
                        {confessions.map((c) => (
                            <div
                                key={c.id}
                                style={{
                                    padding: '1rem',
                                    border: '1px solid #eee',
                                    borderRadius: 'var(--radius-md)',
                                    backgroundColor: 'white',
                                    cursor: 'pointer',
                                    transition: 'transform 0.1s',
                                    boxShadow: '0 2px 5px rgba(0,0,0,0.05)'
                                }}
                                onClick={() => navigate(`/chat/${c.id}`)}
                                onMouseOver={(e) => e.currentTarget.style.transform = 'translateY(-2px)'}
                                onMouseOut={(e) => e.currentTarget.style.transform = 'translateY(0)'}
                            >
                                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                                    <span style={{ fontWeight: 'bold', color: 'hsl(var(--secondary))' }}>
                                        {c.sender ? `To: ${c.otherUserAlias}` : `From: ${c.otherUserAlias}`}
                                    </span>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                        {c.hasUnread && (
                                            <span style={{
                                                height: '10px',
                                                width: '10px',
                                                borderRadius: '50%',
                                                backgroundColor: '#ff4444',
                                                display: 'inline-block',
                                                boxShadow: '0 0 5px rgba(255, 68, 68, 0.5)'
                                            }} title="New Messages" />
                                        )}
                                        <span style={{ fontSize: '0.8rem', color: '#999' }}>
                                            {new Date(c.createdAt).toLocaleDateString()}
                                        </span>
                                    </div>
                                </div>
                                <p style={{ color: 'hsl(var(--text-main))', margin: 0 }}>
                                    {c.icebreakerMessage}
                                </p>
                                <div style={{ marginTop: '0.5rem', fontSize: '0.8rem' }}>
                                    Status: <span style={{ fontWeight: 600, color: c.state === 'ACCEPTED' ? 'green' : c.state === 'REJECTED' ? 'red' : 'orange' }}>{c.state}</span>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>

            <div className='card' style={{ textAlign: 'center', padding: '2rem' }}>
                <h3 style={{ color: 'hsl(var(--secondary))' }}>Ready to confess?</h3>
                <p style={{ marginBottom: '1.5rem', color: 'hsl(var(--text-muted))' }}>
                    Send an anonymous message to someone by their Roll Number.
                </p>
                <Button onClick={() => setIsModalOpen(true)} style={{ fontSize: '1.1rem', padding: '0.8rem 2rem' }}>
                    Search & Confess 💌
                </Button>
            </div>

            {/* New Confession Modal */}
            <Modal
                isOpen={isModalOpen}
                onClose={() => setIsModalOpen(false)}
                title="Send New Confession"
                footer={
                    <>
                        <Button variant="outline" onClick={() => setIsModalOpen(false)} style={{ marginRight: '0.5rem' }}>
                            Cancel
                        </Button>
                        <Button onClick={handleSendConfession} loading={sending}>
                            Send Message
                        </Button>
                    </>
                }
            >
                <form onSubmit={handleSendConfession}>
                    <Input
                        label="Recipient Roll Number"
                        placeholder="e.g. 23BAI70503"
                        value={targetId}
                        onChange={(e) => setTargetId(e.target.value)}
                        required
                        autoFocus
                    />
                    <Input
                        label="Icebreaker Message"
                        placeholder="Hey... I noticed you in the library..."
                        value={message}
                        onChange={(e) => setMessage(e.target.value)}
                        required
                    />
                </form>
            </Modal>
        </div>
    );
};

export default Dashboard;
