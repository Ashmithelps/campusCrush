import React, { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../services/api';
import socketService from '../services/socket';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import HeartLoader from '../components/HeartLoader';
import MutualCrushOverlay from '../components/MutualCrushOverlay';

const AVATAR_COLORS = [
    '#C75D3F', '#B5600F', '#5B8A5A', '#5B7FA3',
    '#7A6B9A', '#C47080', '#8A8A5A', '#A0624A',
    '#5A7A8A', '#4A7A7A',
];
function avatarColor(alias = '') {
    let h = 0;
    for (let i = 0; i < alias.length; i++) h = alias.charCodeAt(i) + ((h << 5) - h);
    return AVATAR_COLORS[Math.abs(h) % AVATAR_COLORS.length];
}
function avatarLetter(alias = '') { return alias.trim()[0]?.toUpperCase() ?? '?'; }

const Chat = () => {
    const { confessionId } = useParams();
    const navigate         = useNavigate();
    const { user }         = useAuth();
    const { theme, toggleTheme } = useTheme();

    const [messages, setMessages]           = useState([]);
    const [confession, setConfession]       = useState(null);
    const [inputText, setInputText]         = useState('');
    const [loading, setLoading]             = useState(true);
    const [error, setError]                 = useState('');
    const [confirmReveal, setConfirmReveal] = useState(false);
    const [confirmBlock, setConfirmBlock]   = useState(false);
    const [actionLoading, setActionLoading] = useState(false);
    const [menuOpen, setMenuOpen]           = useState(false);
    const [showMutual, setShowMutual]       = useState(false);

    const messagesEndRef  = useRef(null);
    const subscriptionRef = useRef(null);

    const scrollToBottom = () => messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });

    const fetchConfession = async () => {
        try {
            let confData = null;
            try {
                const res = await api.get(`/confessions/${confessionId}`);
                confData = res.data;
            } catch {
                const all = await api.get('/confessions');
                confData = all.data.find(c => c.id.toString() === confessionId) ?? null;
            }
            setConfession(confData);
            if (confData?.showMutualAnimation) setShowMutual(true);
            if (!confData) setError('Could not load this conversation.');
        } catch (err) {
            console.error('Failed to fetch confession', err);
            setError('Could not load this conversation.');
        }
    };

    const fetchMessages = async () => {
        try {
            const res = await api.get(`/messages/${confessionId}`);
            setMessages(res.data);
        } catch (err) {
            console.error('Failed to fetch messages', err);
        }
    };

    const fetchAll = async () => {
        await Promise.all([fetchConfession(), fetchMessages()]);
        setLoading(false);
    };

    useEffect(() => {
        fetchAll();
        api.post(`/confessions/${confessionId}/read`).catch(() => {});

        const token = localStorage.getItem('token');
        if (token) {
            socketService.connect(token);
            socketService.subscribe(`/user/queue/confession/${confessionId}`, (frame) => {
                const event = frame?.body;
                if (event === 'MUTUAL') {
                    setShowMutual(true);
                }
                fetchMessages();
                api.post(`/confessions/${confessionId}/read`).catch(() => {});
            }).then(sub => { subscriptionRef.current = sub; });
        }

        return () => { if (subscriptionRef.current) subscriptionRef.current.unsubscribe(); };
    }, [confessionId]);

    useEffect(() => { scrollToBottom(); }, [messages]);

    const handleSend = (e) => {
        e.preventDefault();
        const text = inputText.trim();
        if (!text) return;

        // Optimistic update — show message immediately, server confirms via WS echo
        setMessages(prev => [...prev, {
            id: `temp-${Date.now()}`,
            from: 'SELF',
            content: text,
            type: 'TEXT',
            sentAt: new Date().toISOString(),
        }]);

        socketService.sendMessage(confessionId, text);
        setInputText('');
    };

    const handleReply = async () => {
        setActionLoading(true);
        try {
            await api.post(`/confessions/${confessionId}/reply`);
            await fetchAll();
        } catch (err) {
            console.error('Reply failed', err);
        } finally {
            setActionLoading(false);
        }
    };

    const handleReveal = async () => {
        setActionLoading(true);
        try {
            await api.post(`/confessions/${confessionId}/reveal`);
            setConfirmReveal(false);
            await fetchAll();
        } catch (err) {
            console.error('Reveal failed', err);
        } finally {
            setActionLoading(false);
        }
    };

    const handleBlock = async () => {
        setActionLoading(true);
        try {
            await api.post(`/confessions/${confessionId}/block`);
            setConfirmBlock(false);
            await fetchAll();
        } catch (err) {
            console.error('Block failed', err);
        } finally {
            setActionLoading(false);
        }
    };

    const handleUnblock = async () => {
        setActionLoading(true);
        try {
            await api.post(`/confessions/${confessionId}/unblock`);
            await fetchAll();
        } catch (err) {
            console.error('Unblock failed', err);
        } finally {
            setActionLoading(false);
        }
    };

    const state     = confession?.state;
    const isSender  = confession?.isSender;
    const isBlocked = state === 'BLOCKED';
    const isPending = state === 'CREATED';
    const isActive  = state === 'UNLOCKED' || state === 'REVEALED';

    const stateBadge = {
        CREATED:  { label: 'Pending',  cls: 'badge-created'  },
        UNLOCKED: { label: 'Active',   cls: 'badge-unlocked' },
        BLOCKED:  { label: 'Blocked',  cls: 'badge-blocked'  },
        REVEALED: { label: 'Revealed', cls: 'badge-revealed' },
    }[state] ?? null;

    // ── Error state ──────────────────────────────────────────
    if (!loading && error) {
        return (
            <div className="chat-page">
                <header className="chat-header">
                    <button className="chat-back-btn" onClick={() => navigate('/dashboard')}>←</button>
                    <div className="chat-header-info">
                        <div className="chat-header-name">Chat</div>
                    </div>
                </header>
                <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 12, padding: 24 }}>
                    <span style={{ fontSize: '2rem', opacity: 0.3 }}>💬</span>
                    <p style={{ color: 'var(--text-2)', fontSize: '0.9rem', textAlign: 'center' }}>{error}</p>
                    <button className="btn-full btn-surface" style={{ maxWidth: 200 }} onClick={() => navigate('/dashboard')}>
                        Go back
                    </button>
                </div>
            </div>
        );
    }

    const alias  = loading ? '...' : (confession?.otherUserAlias ?? 'Chat');
    const aColor = avatarColor(alias);
    const aLetter = avatarLetter(alias);

    return (
        <div className="chat-page">
            {/* Header */}
            <header className="chat-header">
                <button className="chat-back-btn" onClick={() => navigate('/dashboard')}>←</button>

                {/* Avatar */}
                {!loading && (
                    <div style={{
                        width: 34, height: 34, borderRadius: '50%',
                        background: aColor, color: 'white',
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        fontSize: '0.8rem', fontWeight: 800, flexShrink: 0,
                        letterSpacing: '-0.02em',
                    }}>
                        {aLetter}
                    </div>
                )}

                <div className="chat-header-info">
                    <div className="chat-header-name">{alias}</div>
                </div>

                {stateBadge && (
                    <span className={`badge ${stateBadge.cls}`}>{stateBadge.label}</span>
                )}

                <button className="btn-theme" onClick={toggleTheme} style={{ marginLeft: 2 }}>
                    {theme === 'dark' ? 'Light' : 'Dark'}
                </button>

                {/* ⋮ menu */}
                {(isActive || isPending) && !loading && (
                    <div style={{ position: 'relative' }}>
                        <button
                            className="chat-back-btn"
                            onClick={() => setMenuOpen(o => !o)}
                            style={{ fontSize: '1.4rem', color: 'var(--text-2)' }}
                        >
                            ⋮
                        </button>
                        {menuOpen && (
                            <div style={{
                                position: 'absolute', top: '110%', right: 0,
                                background: 'var(--surface-2)',
                                border: '1px solid var(--border-2)',
                                borderRadius: 'var(--radius-sm)',
                                overflow: 'hidden',
                                minWidth: 170, zIndex: 30,
                                boxShadow: '0 12px 32px rgba(0,0,0,0.6)',
                            }}>
                                {isSender && !confession?.isRevealed && isActive && (
                                    <button
                                        onClick={() => { setMenuOpen(false); setConfirmReveal(true); }}
                                        style={{
                                            display: 'block', width: '100%', padding: '13px 16px',
                                            background: 'none', border: 'none', color: 'var(--gold)',
                                            fontSize: '0.85rem', fontWeight: 600, textAlign: 'left',
                                            cursor: 'pointer', fontFamily: 'var(--font)',
                                            borderBottom: '1px solid var(--border)',
                                        }}
                                    >
                                        Reveal Identity 🎭
                                    </button>
                                )}
                                {state !== 'BLOCKED' && (
                                    <button
                                        onClick={() => { setMenuOpen(false); setConfirmBlock(true); }}
                                        style={{
                                            display: 'block', width: '100%', padding: '13px 16px',
                                            background: 'none', border: 'none', color: 'var(--danger)',
                                            fontSize: '0.85rem', fontWeight: 600, textAlign: 'left',
                                            cursor: 'pointer', fontFamily: 'var(--font)',
                                        }}
                                    >
                                        Block conversation
                                    </button>
                                )}
                            </div>
                        )}
                    </div>
                )}
            </header>
            {/* Dismiss menu on outside click */}
            {menuOpen && (
                <div
                    style={{ position: 'fixed', inset: 0, zIndex: 29 }}
                    onClick={() => setMenuOpen(false)}
                />
            )}

            {/* Messages */}
            <div className="chat-messages">
                {loading && (
                    <div className="loader" style={{ minHeight: '40dvh' }}>
                        <HeartLoader size={70} />
                    </div>
                )}

                {!loading && messages.length === 0 && !error && (
                    <p style={{ textAlign: 'center', color: 'var(--text-3)', fontSize: '0.85rem', marginTop: 24 }}>
                        No messages yet.
                    </p>
                )}

                {messages.map(msg => {
                    if (msg.type === 'REVEAL') {
                        return (
                            <div key={msg.id} className="chat-system-msg">
                                <span className="chat-system-pill">{msg.content}</span>
                            </div>
                        );
                    }
                    const self = msg.from === 'SELF';
                    return (
                        <div key={msg.id} className={`chat-bubble-wrap ${self ? 'self' : 'other'}`}>
                            <div className={`chat-bubble ${self ? 'self' : 'other'}`}>
                                {msg.content}
                            </div>
                            <div className="chat-time">
                                {new Date(msg.sentAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                            </div>
                        </div>
                    );
                })}
                <div ref={messagesEndRef} />
            </div>

            {/* Action Bar — pinned at bottom, state-driven */}
            <div className="chat-action-bar">

                {/* ── Confirm overlays (work for any state) ── */}
                {confirmBlock && (
                    <div className="confirm-overlay">
                        <p>Block this conversation? The other person won't be able to message you.</p>
                        <div className="confirm-btns">
                            <button
                                className="chat-action-btn btn-danger"
                                onClick={handleBlock}
                                disabled={actionLoading}
                            >
                                {actionLoading ? '...' : 'Block'}
                            </button>
                            <button
                                className="chat-action-btn btn-surface"
                                onClick={() => setConfirmBlock(false)}
                            >
                                Cancel
                            </button>
                        </div>
                    </div>
                )}

                {confirmReveal && (
                    <div className="confirm-overlay">
                        <p>Reveal your identity? This cannot be undone.</p>
                        <div className="confirm-btns">
                            <button
                                className="chat-action-btn"
                                style={{ background: 'var(--accent)', color: 'white', border: 'none' }}
                                onClick={handleReveal}
                                disabled={actionLoading}
                            >
                                {actionLoading ? '...' : 'Yes, reveal'}
                            </button>
                            <button
                                className="chat-action-btn btn-surface"
                                onClick={() => setConfirmReveal(false)}
                            >
                                Cancel
                            </button>
                        </div>
                    </div>
                )}

                {/* ── State content (hidden while a confirm is open) ── */}
                {!confirmBlock && !confirmReveal && (
                    <>
                        {/* CREATED — receiver */}
                        {isPending && !isSender && (
                            <div>
                                <div className="chat-state-banner">Someone has a crush on you ✨</div>
                                <div className="chat-secondary-actions">
                                    <button
                                        className="chat-action-btn btn-success"
                                        onClick={handleReply}
                                        disabled={actionLoading}
                                    >
                                        {actionLoading ? '...' : 'Accept & Reply'}
                                    </button>
                                    <button
                                        className="chat-action-btn btn-danger"
                                        onClick={() => setConfirmBlock(true)}
                                    >
                                        Decline
                                    </button>
                                </div>
                            </div>
                        )}

                        {/* CREATED — sender */}
                        {isPending && isSender && (
                            <div className="chat-state-banner" style={{ marginBottom: 0 }}>
                                ⏳ Waiting for them to accept...
                            </div>
                        )}

                        {/* BLOCKED */}
                        {isBlocked && (
                            <div>
                                <div className="chat-state-banner" style={{ marginBottom: 10 }}>
                                    ⛔ This conversation is blocked
                                </div>
                                <button
                                    className="btn-full btn-surface"
                                    style={{ padding: '10px' }}
                                    onClick={handleUnblock}
                                    disabled={actionLoading}
                                >
                                    {actionLoading ? '...' : 'Unblock'}
                                </button>
                            </div>
                        )}

                        {/* ACTIVE — input only, Block/Reveal moved to ⋮ header menu */}
                        {isActive && (
                            <form className="chat-input-row" onSubmit={handleSend}>
                                <input
                                    className="chat-input"
                                    value={inputText}
                                    onChange={e => setInputText(e.target.value)}
                                    placeholder="Type a message..."
                                    autoComplete="off"
                                    autoFocus
                                />
                                <button
                                    type="submit"
                                    className="chat-send-btn"
                                    disabled={!inputText.trim()}
                                >
                                    →
                                </button>
                            </form>
                        )}

                    </>
                )}
            </div>
            {showMutual && (
                <MutualCrushOverlay
                    onDone={() => {
                        api.post(`/confessions/${confessionId}/mutual-seen`).catch(() => {});
                        setShowMutual(false);
                        fetchAll();
                    }}
                />
            )}
        </div>
    );
};

export default Chat;
