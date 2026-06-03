import React, { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import api, { apiError } from '../services/api';
import socket from '../services/socket';
import { useNavigate } from 'react-router-dom';

const STATE_BADGE = {
    CREATED:  { label: 'Pending',  cls: 'badge-created'  },
    UNLOCKED: { label: 'Active',   cls: 'badge-unlocked' },
    BLOCKED:  { label: 'Blocked',  cls: 'badge-blocked'  },
    REVEALED: { label: 'Revealed', cls: 'badge-revealed' },
};

const AVATAR_COLORS = [
    '#FF2D55', '#FF6B00', '#FFB800', '#1DB954',
    '#00C7BE', '#0A84FF', '#5E5CE6', '#BF5AF2',
    '#FF375F', '#30B0C7',
];

function avatarColor(alias = '') {
    let h = 0;
    for (let i = 0; i < alias.length; i++) h = alias.charCodeAt(i) + ((h << 5) - h);
    return AVATAR_COLORS[Math.abs(h) % AVATAR_COLORS.length];
}

function avatarLetter(alias = '') {
    return alias.trim()[0]?.toUpperCase() ?? '?';
}

function timeAgo(dateStr) {
    const diff = Math.floor((Date.now() - new Date(dateStr)) / 1000);
    if (diff < 60)    return 'now';
    if (diff < 3600)  return `${Math.floor(diff / 60)}m`;
    if (diff < 86400) return `${Math.floor(diff / 3600)}h`;
    return `${Math.floor(diff / 86400)}d`;
}

const Dashboard = () => {
    const { logout, user }   = useAuth();
    const [confessions, setConfessions] = useState([]);
    const [loading, setLoading]         = useState(true);
    const [fetchError, setFetchError]   = useState(false);
    const [sheetOpen, setSheetOpen]     = useState(false);
    const [targetId, setTargetId]       = useState('');
    const [message, setMessage]         = useState('');
    const [sending, setSending]         = useState(false);
    const [sendError, setSendError]     = useState('');
    const navigate = useNavigate();

    useEffect(() => {
        const unread = confessions.filter(c => c.hasUnread).length;
        document.title = unread > 0 ? `(${unread}) CampusCrush` : 'CampusCrush';
        return () => { document.title = 'CampusCrush'; };
    }, [confessions]);

    const fetchConfessions = async () => {
        try {
            const res = await api.get('/confessions');
            setConfessions(res.data);
            setFetchError(false);
        } catch (err) {
            console.error('Failed to fetch confessions', err);
            setFetchError(true);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchConfessions();

        let subscription;
        const token = localStorage.getItem('token');
        if (token) {
            socket.connect(token);
            socket.subscribe('/user/queue/confessions', () => {
                fetchConfessions();
            }).then(sub => { subscription = sub; });
        }

        return () => { if (subscription) subscription.unsubscribe(); };
    }, []);

    const handleSendConfession = async (e) => {
        e.preventDefault();
        setSendError('');
        if (!targetId.trim() || !message.trim()) {
            setSendError('Both fields are required.');
            return;
        }
        setSending(true);
        try {
            await api.post(`/confessions/${targetId.trim().toUpperCase()}`, message.trim());
            setSheetOpen(false);
            setTargetId('');
            setMessage('');
            fetchConfessions();
        } catch (err) {
            setSendError(apiError(err, 'Failed to send confession.'));
        } finally {
            setSending(false);
        }
    };

    const closeSheet = () => {
        setSheetOpen(false);
        setSendError('');
        setTargetId('');
        setMessage('');
    };

    return (
        <div className="dashboard">
            {/* Header */}
            <header className="dash-header">
                <div className="dash-header-left">
                    <div className="dash-header-logo">campuscrush</div>
                    <div className="dash-header-alias">{user?.displayAlias ?? ''}</div>
                </div>
                <button className="btn-ghost" onClick={logout}>
                    Sign out
                </button>
            </header>

            {/* Section label */}
            {!loading && confessions.length > 0 && (
                <div className="dash-section-label">Messages</div>
            )}

            {/* List */}
            <div className="dash-list">
                {loading ? (
                    <div className="dash-empty">
                        <div className="dash-empty-title">Loading...</div>
                    </div>
                ) : fetchError ? (
                    <div className="dash-empty">
                        <div className="dash-empty-icon">⚠️</div>
                        <div className="dash-empty-title">Could not load messages</div>
                        <div className="dash-empty-sub">Check your connection and try again</div>
                        <button className="btn-full btn-surface" style={{ marginTop: 16, maxWidth: 200 }} onClick={fetchConfessions}>Retry</button>
                    </div>
                ) : confessions.length === 0 ? (
                    <div className="dash-empty">
                        <div className="dash-empty-icon">💌</div>
                        <div className="dash-empty-title">No confessions yet</div>
                        <div className="dash-empty-sub">Tap the button below to send your first one</div>
                    </div>
                ) : (
                    confessions.map(c => {
                        const badge  = STATE_BADGE[c.state] ?? { label: c.state, cls: '' };
                        const alias  = c.otherUserAlias ?? '?';
                        const color  = avatarColor(alias);
                        const letter = avatarLetter(alias);
                        return (
                            <div
                                key={c.id}
                                className={`conf-card${c.hasUnread ? ' conf-card--unread' : ''}`}
                                onClick={() => navigate(`/chat/${c.id}`)}
                            >
                                {/* Avatar */}
                                <div
                                    className={`conf-avatar${c.hasUnread ? ' conf-avatar-unread' : ''}`}
                                    style={{ background: color }}
                                >
                                    {letter}
                                </div>

                                {/* Body */}
                                <div className="conf-card-body">
                                    <div className="conf-card-top">
                                        <span className={`conf-card-alias${c.hasUnread ? ' conf-card-alias--unread' : ''}`}>{alias}</span>
                                        <div className="conf-card-meta">
                                            <span className={`badge ${badge.cls}`}>{badge.label}</span>
                                            <span className="conf-card-time">{timeAgo(c.createdAt)}</span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        );
                    })
                )}
            </div>

            {/* FAB */}
            <button className="fab" onClick={() => setSheetOpen(true)} aria-label="New confession">
                <span style={{ fontSize: '1rem', lineHeight: 1 }}>✦</span>
                Confess
            </button>

            {/* Bottom Sheet */}
            {sheetOpen && (
                <div className="sheet-overlay" onClick={closeSheet}>
                    <div className="sheet" onClick={e => e.stopPropagation()}>
                        <div className="sheet-handle" />
                        <div className="sheet-title">Send a confession</div>

                        <div className="field">
                            <label>Their Roll Number</label>
                            <input
                                className="field-input"
                                placeholder="e.g. 23BAI70503"
                                value={targetId}
                                onChange={e => setTargetId(e.target.value)}
                                autoFocus
                            />
                        </div>

                        <div className="field">
                            <label>Icebreaker Message</label>
                            <input
                                className="field-input"
                                placeholder="Hey, I noticed you in the library..."
                                value={message}
                                onChange={e => setMessage(e.target.value)}
                                onKeyDown={e => e.key === 'Enter' && handleSendConfession(e)}
                            />
                        </div>

                        {sendError && (
                            <p className="error-text" style={{ marginBottom: 14 }}>{sendError}</p>
                        )}

                        <button
                            className="btn-full btn-accent"
                            onClick={handleSendConfession}
                            disabled={sending}
                        >
                            {sending ? 'Sending...' : 'Send Anonymously'}
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Dashboard;
