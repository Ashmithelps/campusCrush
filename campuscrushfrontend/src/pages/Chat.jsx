import React, { useEffect, useState, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../services/api';
import socketService from '../services/socket';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import MutualCrushOverlay from '../components/MutualCrushOverlay';

// ── Helpers ────────────────────────────────────────────────────────────────

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

function formatTime(ts) {
    return new Date(ts).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

function formatDate(ts) {
    const d = new Date(ts);
    const today = new Date();
    const yesterday = new Date(today);
    yesterday.setDate(today.getDate() - 1);
    if (d.toDateString() === today.toDateString()) return 'Today';
    if (d.toDateString() === yesterday.toDateString()) return 'Yesterday';
    return d.toLocaleDateString([], { month: 'short', day: 'numeric' });
}

// Group consecutive TEXT messages from the same sender within 5 minutes
function groupMessages(msgs) {
    return msgs.map((msg, i) => {
        if (msg.type !== 'TEXT' || msg.id === -1) {
            return { ...msg, isFirst: true, isLast: true, showDate: false };
        }
        const prev = msgs[i - 1];
        const next = msgs[i + 1];

        const gapBefore = !prev ||
            prev.type !== 'TEXT' || prev.id === -1 ||
            prev.from !== msg.from ||
            Math.abs(new Date(msg.sentAt) - new Date(prev.sentAt)) >= 5 * 60 * 1000;

        const gapAfter = !next ||
            next.type !== 'TEXT' || next.id === -1 ||
            next.from !== msg.from ||
            Math.abs(new Date(next.sentAt) - new Date(msg.sentAt)) >= 5 * 60 * 1000;

        // Show date divider if this is the first message or date changed from previous
        const showDate = !prev ||
            new Date(msg.sentAt).toDateString() !== new Date(prev.sentAt).toDateString();

        return { ...msg, isFirst: gapBefore, isLast: gapAfter, showDate };
    });
}

// ── Sub-components ─────────────────────────────────────────────────────────

const ChatAvatar = ({ alias, size = 36 }) => (
    <div
        className="chat-header-avatar"
        style={{ background: avatarColor(alias), width: size, height: size, fontSize: size * 0.38 }}
        aria-hidden="true"
    >
        {avatarLetter(alias)}
    </div>
);

const DateDivider = ({ ts }) => (
    <div className="chat-date-divider" aria-label={formatDate(ts)}>
        <span>{formatDate(ts)}</span>
    </div>
);

const IcebreakerCard = ({ msg, alias }) => (
    <div className="chat-icebreaker-wrap" role="article" aria-label="Opening confession">
        <p className="chat-icebreaker-label">The note that started this</p>
        <blockquote className="chat-icebreaker">{msg.content}</blockquote>
        <p className="chat-icebreaker-time">{formatDate(msg.sentAt)}</p>
    </div>
);

const SystemPill = ({ msg }) => {
    const cls = msg.type === 'MUTUAL'
        ? 'chat-system-pill chat-system-pill--mutual'
        : 'chat-system-pill chat-system-pill--reveal';
    return (
        <div className="chat-system-msg" role="status" aria-live="polite">
            <span className={cls}>{msg.content}</span>
        </div>
    );
};

const MessageBubble = ({ msg }) => {
    const self = msg.from === 'SELF';
    const bubbleCls = [
        'chat-bubble',
        self ? 'self' : 'other',
        !msg.isLast ? 'chat-bubble--mid' : '',
    ].filter(Boolean).join(' ');

    return (
        <>
            {msg.showDate && <DateDivider ts={msg.sentAt} />}
            {msg.isFirst && <div className="chat-group-gap" />}
            <div className={`chat-bubble-wrap ${self ? 'self' : 'other'}`}>
                <div className={bubbleCls}>{msg.content}</div>
                {msg.isLast && (
                    <div className="chat-time">{formatTime(msg.sentAt)}</div>
                )}
            </div>
        </>
    );
};

const TypingIndicator = () => (
    <div className="chat-bubble-wrap other">
        <div className="chat-bubble other chat-typing">
            <span /><span /><span />
        </div>
    </div>
);

// ── Main component ─────────────────────────────────────────────────────────

const Chat = () => {
    const { confessionId } = useParams();
    const navigate          = useNavigate();
    const { user }          = useAuth();
    const { theme, toggleTheme } = useTheme();

    const [messages,    setMessages]    = useState([]);
    const [confession,  setConfession]  = useState(null);
    const [inputText,   setInputText]   = useState('');
    const [loading,     setLoading]     = useState(true);
    const [error,       setError]       = useState('');
    const [sheetOpen,   setSheetOpen]   = useState(false);
    const [confirmAction, setConfirmAction] = useState(null); // 'block' | 'decline' | 'reveal' | 'unblock'
    const [actionLoading, setActionLoading] = useState(false);
    const [showMutual,  setShowMutual]  = useState(false);
    const [atBottom,    setAtBottom]    = useState(true);

    const messagesListRef = useRef(null);
    const messagesEndRef  = useRef(null);
    const inputRef        = useRef(null);
    const subscriptionRef = useRef(null);

    // ── Scroll tracking ───────────────────────────────────────────────────

    const handleScroll = useCallback(() => {
        const el = messagesListRef.current;
        if (!el) return;
        const nearBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 100;
        setAtBottom(nearBottom);
    }, []);

    const scrollToBottom = useCallback((behavior = 'smooth') => {
        messagesEndRef.current?.scrollIntoView({ behavior });
    }, []);

    useEffect(() => {
        if (atBottom) scrollToBottom();
    }, [messages, atBottom, scrollToBottom]);

    // ── Data fetching ─────────────────────────────────────────────────────

    const fetchConfession = useCallback(async () => {
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
        } catch {
            setError('Could not load this conversation.');
        }
    }, [confessionId]);

    const fetchMessages = useCallback(async () => {
        try {
            const res = await api.get(`/messages/${confessionId}`);
            setMessages(res.data);
        } catch {
            // swallow — socket will retry
        }
    }, [confessionId]);

    const fetchAll = useCallback(async () => {
        await Promise.all([fetchConfession(), fetchMessages()]);
        setLoading(false);
    }, [fetchConfession, fetchMessages]);

    useEffect(() => {
        fetchAll();
        api.post(`/confessions/${confessionId}/read`).catch(() => {});
        scrollToBottom('instant');

        const token = localStorage.getItem('token');
        if (token) {
            socketService.connect(token);
            socketService.subscribe(`/user/queue/confession/${confessionId}`, (frame) => {
                const event = frame?.body;
                if (event === 'MUTUAL') setShowMutual(true);
                fetchMessages();
                fetchConfession();
                api.post(`/confessions/${confessionId}/read`).catch(() => {});
            }).then(sub => { subscriptionRef.current = sub; });
        }

        return () => { if (subscriptionRef.current) subscriptionRef.current.unsubscribe(); };
    }, [confessionId]); // eslint-disable-line react-hooks/exhaustive-deps

    // ── Actions ───────────────────────────────────────────────────────────

    const handleSend = (e) => {
        e.preventDefault();
        const text = inputText.trim();
        if (!text || actionLoading) return;

        // Optimistic
        setMessages(prev => [...prev, {
            id: `temp-${Date.now()}`,
            from: 'SELF',
            content: text,
            type: 'TEXT',
            sentAt: new Date().toISOString(),
        }]);
        socketService.sendMessage(confessionId, text);
        setInputText('');
        inputRef.current?.focus();
    };

    const runAction = async (fn) => {
        setActionLoading(true);
        try {
            await fn();
            setConfirmAction(null);
            await fetchAll();
        } catch {
            // action failed silently; could surface a toast
        } finally {
            setActionLoading(false);
        }
    };

    const handleReply   = () => runAction(() => api.post(`/confessions/${confessionId}/reply`));
    const handleReveal  = () => runAction(() => api.post(`/confessions/${confessionId}/reveal`));
    const handleBlock   = () => runAction(() => api.post(`/confessions/${confessionId}/block`));
    const handleUnblock = () => runAction(() => api.post(`/confessions/${confessionId}/unblock`));

    // ── Derived state ─────────────────────────────────────────────────────

    const state      = confession?.state;
    const isSender   = confession?.isSender;
    const isBlocked  = state === 'BLOCKED';
    const isPending  = state === 'CREATED';
    const isActive   = state === 'UNLOCKED' || state === 'REVEALED';
    const canReveal  = isSender && !confession?.isRevealed && isActive;

    const alias   = loading ? '…' : (confession?.otherUserAlias ?? 'Chat');
    const grouped = groupMessages(messages);

    // ── Status badge text ─────────────────────────────────────────────────

    const statusLabel = {
        CREATED:  'Pending',
        UNLOCKED: 'Active',
        BLOCKED:  'Blocked',
        REVEALED: 'Revealed',
    }[state] ?? null;

    // ── Error state ───────────────────────────────────────────────────────

    if (!loading && error) {
        return (
            <div className="chat-page">
                <header className="chat-header">
                    <button className="chat-back-btn" onClick={() => navigate('/dashboard')} aria-label="Back to inbox">←</button>
                    <div className="chat-header-info"><div className="chat-header-name">Chat</div></div>
                </header>
                <div className="chat-error-state">
                    <p className="chat-error-copy">{error}</p>
                    <button className="btn-full btn-surface" style={{ maxWidth: 180 }} onClick={() => navigate('/dashboard')}>
                        Go back
                    </button>
                </div>
            </div>
        );
    }

    // ── Render ────────────────────────────────────────────────────────────

    return (
        <div className="chat-page">

            {/* ── Header ── */}
            <header className="chat-header">
                <button
                    className="chat-back-btn"
                    onClick={() => navigate('/dashboard')}
                    aria-label="Back to inbox"
                >
                    ←
                </button>

                {!loading && <ChatAvatar alias={alias} />}

                <div className="chat-header-info">
                    <div className={`chat-header-name${isSender ? ' data' : ''}`}>{alias}</div>
                    {statusLabel && (
                        <div className="chat-header-sub">{statusLabel}</div>
                    )}
                </div>

                {/* ⋮ overflow */}
                {!loading && (
                    <button
                        className="chat-menu-btn"
                        onClick={() => setSheetOpen(true)}
                        aria-label="More options"
                        aria-haspopup="dialog"
                    >
                        ⋮
                    </button>
                )}
            </header>

            {/* ── Message list ── */}
            <div
                className="chat-messages"
                ref={messagesListRef}
                onScroll={handleScroll}
                role="log"
                aria-label="Messages"
                aria-live="polite"
            >
                {loading ? (
                    <div className="chat-loading">
                        <div className="skel-row" style={{ '--ri': 0, padding: '10px 0' }}>
                            <div className="skel-lines" style={{ flex: 1 }}>
                                <div className="skel-line skel-pulse" style={{ width: '55%', marginLeft: 'auto' }} />
                                <div className="skel-line skel-pulse" style={{ width: '40%', marginTop: 8 }} />
                                <div className="skel-line skel-pulse" style={{ width: '65%', marginLeft: 'auto', marginTop: 14 }} />
                                <div className="skel-line skel-pulse" style={{ width: '50%', marginTop: 8 }} />
                            </div>
                        </div>
                    </div>
                ) : (
                    grouped.map(msg => {
                        if (msg.id === -1) {
                            return <IcebreakerCard key={msg.id} msg={msg} alias={alias} />;
                        }
                        if (msg.type === 'REVEAL' || msg.type === 'MUTUAL') {
                            return <SystemPill key={msg.id} msg={msg} />;
                        }
                        return <MessageBubble key={msg.id} msg={msg} />;
                    })
                )}
                <div ref={messagesEndRef} />
            </div>

            {/* ── Scroll-to-bottom pill ── */}
            {!atBottom && !loading && (
                <button
                    className="chat-scroll-btn"
                    onClick={() => scrollToBottom()}
                    aria-label="Scroll to latest message"
                >
                    ↓ new
                </button>
            )}

            {/* ── Action bar ── */}
            <div className="chat-action-bar">

                {/* CREATED — receiver: show accept / decline */}
                {isPending && !isSender && (
                    <div className="chat-invited-bar">
                        <p className="chat-state-banner">Someone sent you a note ✦</p>
                        <div className="chat-secondary-actions">
                            <button
                                className="chat-action-btn btn-success"
                                onClick={handleReply}
                                disabled={actionLoading}
                                aria-busy={actionLoading}
                            >
                                {actionLoading ? '…' : 'Accept & reply'}
                            </button>
                            <button
                                className="chat-action-btn btn-danger"
                                onClick={() => setConfirmAction('decline')}
                            >
                                Decline
                            </button>
                        </div>
                    </div>
                )}

                {/* CREATED — sender: waiting banner */}
                {isPending && isSender && (
                    <p className="chat-state-banner chat-state-banner--waiting">
                        Waiting for them to open your note…
                    </p>
                )}

                {/* BLOCKED */}
                {isBlocked && (
                    <div className="chat-blocked-bar">
                        <p className="chat-state-banner">This conversation is blocked.</p>
                        {/* Only the person who blocked can unblock */}
                        <button
                            className="btn-full btn-surface"
                            style={{ padding: '11px', marginTop: 2 }}
                            onClick={() => setConfirmAction('unblock')}
                            disabled={actionLoading}
                        >
                            Unblock
                        </button>
                    </div>
                )}

                {/* ACTIVE — composer */}
                {isActive && (
                    <form className="chat-input-row" onSubmit={handleSend}>
                        <input
                            ref={inputRef}
                            className="chat-input"
                            value={inputText}
                            onChange={e => setInputText(e.target.value)}
                            placeholder="Say something…"
                            autoComplete="off"
                            aria-label="Message input"
                        />
                        <button
                            type="submit"
                            className="chat-send-btn"
                            disabled={!inputText.trim()}
                            aria-label="Send message"
                        >
                            ↑
                        </button>
                    </form>
                )}
            </div>

            {/* ── ⋮ overflow sheet ── */}
            {sheetOpen && (
                <div className="sheet-overlay" onClick={() => setSheetOpen(false)}>
                    <div
                        className="sheet"
                        role="dialog"
                        aria-label="Conversation options"
                        onClick={e => e.stopPropagation()}
                    >
                        <div className="sheet-handle" />

                        <button
                            className="chat-sheet-item"
                            onClick={() => { setSheetOpen(false); toggleTheme(); }}
                        >
                            <span className="chat-sheet-icon">◐</span>
                            {theme === 'dark' ? 'Switch to light' : 'Switch to dark'}
                        </button>

                        {canReveal && (
                            <button
                                className="chat-sheet-item chat-sheet-item--gold"
                                onClick={() => { setSheetOpen(false); setConfirmAction('reveal'); }}
                            >
                                <span className="chat-sheet-icon">✦</span>
                                Reveal yourself
                            </button>
                        )}

                        {!isBlocked && (
                            <button
                                className="chat-sheet-item chat-sheet-item--danger"
                                onClick={() => { setSheetOpen(false); setConfirmAction('block'); }}
                            >
                                <span className="chat-sheet-icon">⊘</span>
                                Block conversation
                            </button>
                        )}
                    </div>
                </div>
            )}

            {/* ── Confirm sheet ── */}
            {confirmAction && (
                <div className="sheet-overlay" onClick={() => !actionLoading && setConfirmAction(null)}>
                    <div
                        className="sheet"
                        role="alertdialog"
                        aria-modal="true"
                        aria-live="assertive"
                        onClick={e => e.stopPropagation()}
                    >
                        <div className="sheet-handle" />

                        {confirmAction === 'reveal' && (
                            <>
                                <p className="chat-confirm-title">Reveal yourself?</p>
                                <p className="chat-confirm-body">
                                    Your roll number will be shared with them. This cannot be undone.
                                </p>
                                <div className="chat-confirm-btns">
                                    <button
                                        className="chat-action-btn btn-accent"
                                        onClick={handleReveal}
                                        disabled={actionLoading}
                                        aria-busy={actionLoading}
                                    >
                                        {actionLoading ? '…' : 'Yes, reveal'}
                                    </button>
                                    <button
                                        className="chat-action-btn btn-surface"
                                        onClick={() => setConfirmAction(null)}
                                        disabled={actionLoading}
                                    >
                                        Cancel
                                    </button>
                                </div>
                            </>
                        )}

                        {(confirmAction === 'block' || confirmAction === 'decline') && (
                            <>
                                <p className="chat-confirm-title">
                                    {confirmAction === 'decline' ? 'Decline this confession?' : 'Block this conversation?'}
                                </p>
                                <p className="chat-confirm-body">
                                    {confirmAction === 'decline'
                                        ? 'They won\'t know who you are. You can always change your mind later.'
                                        : 'The other person won\'t be able to send you messages. Only you can unblock.'}
                                </p>
                                <div className="chat-confirm-btns">
                                    <button
                                        className="chat-action-btn btn-danger"
                                        onClick={handleBlock}
                                        disabled={actionLoading}
                                        aria-busy={actionLoading}
                                    >
                                        {actionLoading ? '…' : confirmAction === 'decline' ? 'Decline' : 'Block'}
                                    </button>
                                    <button
                                        className="chat-action-btn btn-surface"
                                        onClick={() => setConfirmAction(null)}
                                        disabled={actionLoading}
                                    >
                                        Cancel
                                    </button>
                                </div>
                            </>
                        )}

                        {confirmAction === 'unblock' && (
                            <>
                                <p className="chat-confirm-title">Unblock this conversation?</p>
                                <p className="chat-confirm-body">
                                    Messages can be sent again. You can block again at any time.
                                </p>
                                <div className="chat-confirm-btns">
                                    <button
                                        className="chat-action-btn btn-surface"
                                        onClick={handleUnblock}
                                        disabled={actionLoading}
                                        aria-busy={actionLoading}
                                    >
                                        {actionLoading ? '…' : 'Unblock'}
                                    </button>
                                    <button
                                        className="chat-action-btn btn-danger-ghost"
                                        onClick={() => setConfirmAction(null)}
                                        disabled={actionLoading}
                                    >
                                        Cancel
                                    </button>
                                </div>
                            </>
                        )}
                    </div>
                </div>
            )}

            {/* ── Mutual crush overlay ── */}
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
