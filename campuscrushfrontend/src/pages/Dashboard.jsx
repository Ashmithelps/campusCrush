import React, { useEffect, useState } from 'react';
import MutualCrushOverlay from '../components/MutualCrushOverlay';
import AppShell from '../layouts/AppShell';
import { useAuth } from '../context/AuthContext';
import api, { apiError } from '../services/api';
import socket from '../services/socket';
import { useNavigate } from 'react-router-dom';

const STATE_BADGE = {
    INVITED:  { label: 'Invited',  cls: 'badge-invited'  },
    CREATED:  { label: 'Pending',  cls: 'badge-created'  },
    UNLOCKED: { label: 'Active',   cls: 'badge-unlocked' },
    BLOCKED:  { label: 'Blocked',  cls: 'badge-blocked'  },
    REVEALED: { label: 'Revealed', cls: 'badge-revealed' },
};

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

function formatPreview(c) {
    return c.icebreakerMessage || null;
}

function groupConfessions(list) {
    return {
        forYou:  list.filter(c => c.state === 'CREATED'  && !c.isSender),
        convos:  list.filter(c => c.state === 'UNLOCKED' || c.state === 'REVEALED'),
        waiting: list.filter(c => (c.state === 'CREATED' && c.isSender) || c.state === 'INVITED'),
        blocked: list.filter(c => c.state === 'BLOCKED'),
    };
}

function buildFlatList(confessions) {
    const { forYou, convos, waiting, blocked } = groupConfessions(confessions);
    const sections = [
        { key: 'forYou',  label: 'For you',       items: forYou  },
        { key: 'convos',  label: 'Conversations',  items: convos  },
        { key: 'waiting', label: 'Waiting',        items: waiting },
        { key: 'blocked', label: 'Blocked',        items: blocked },
    ].filter(s => s.items.length > 0);

    let rowIndex = 0;
    return sections.flatMap(s => [
        { type: 'header', label: s.label, key: `hdr-${s.key}` },
        ...s.items.map(c => ({ type: 'row', c, index: rowIndex++, key: c.id })),
    ]);
}

const SkeletonRows = () => (
    <>
        {[0, 1, 2, 3].map(i => (
            <div key={i} className="skel-row" style={{ '--ri': i }}>
                <div className="skel-avatar skel-pulse" />
                <div className="skel-lines">
                    <div className="skel-line skel-pulse" style={{ width: '40%' }} />
                    <div className="skel-line skel-pulse" style={{ width: '62%', marginTop: 8 }} />
                </div>
            </div>
        ))}
    </>
);

const EmptyState = () => (
    <div className="dash-empty dash-empty--brand">
        <div className="dash-empty-logo">
            <svg width="32" height="32" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true" style={{ opacity: 0.28 }}>
                <circle cx="8"  cy="16" r="4" fill="var(--accent)" />
                <circle cx="16" cy="16" r="4" fill="var(--accent)" />
                <circle cx="24" cy="16" r="4" fill="var(--accent)" />
            </svg>
        </div>
        <p className="dash-empty-title">
            The feeling
            <br />
            you haven't sent yet.
        </p>
        <p className="dash-empty-sub">
            Tap Confess — let someone know.
        </p>
    </div>
);

const ConfessionRow = ({ c, index, onClick }) => {
    const badge   = STATE_BADGE[c.state] ?? { label: c.state, cls: '' };
    const alias   = c.otherUserAlias ?? '?';
    const color   = avatarColor(alias);
    const letter  = avatarLetter(alias);
    const preview = formatPreview(c);

    const a11yLabel = [
        alias,
        badge.label,
        c.hasUnread ? 'unread' : null,
        preview ? `— ${preview}` : null,
    ].filter(Boolean).join(', ');

    return (
        <div
            className={`conf-card${c.hasUnread ? ' conf-card--unread' : ''}${c.state === 'INVITED' ? ' conf-card--invited' : ''}`}
            style={{ '--ri': index }}
            onClick={onClick}
            role={c.state !== 'INVITED' ? 'button' : undefined}
            tabIndex={c.state !== 'INVITED' ? 0 : undefined}
            aria-label={a11yLabel}
            onKeyDown={c.state !== 'INVITED' ? (e => (e.key === 'Enter' || e.key === ' ') && onClick(e)) : undefined}
        >
            <div
                className={`conf-avatar${c.hasUnread ? ' conf-avatar-unread' : ''}`}
                style={{ background: color }}
            >
                {letter}
            </div>

            <div className="conf-card-body">
                <div className="conf-card-top">
                    <span className={`conf-card-alias${c.hasUnread ? ' conf-card-alias--unread' : ''}${c.isSender ? ' data' : ''}`}>
                        {alias}
                    </span>
                    <div className="conf-card-meta">
                        <span className={`badge ${badge.cls}`}>{badge.label}</span>
                        <span className="conf-card-time">{timeAgo(c.createdAt)}</span>
                    </div>
                </div>
                {preview && (
                    <p className="conf-card-preview">{preview}</p>
                )}
            </div>
        </div>
    );
};

const Dashboard = () => {
    const { user }               = useAuth();
    const [confessions, setConfessions] = useState([]);
    const [loading, setLoading]         = useState(true);
    const [fetchError, setFetchError]   = useState(false);
    const [sheetOpen, setSheetOpen]     = useState(false);
    const [targetId, setTargetId]       = useState('');
    const [message, setMessage]         = useState('');
    const [mutualConfessionId, setMutualConfessionId] = useState(null);
    const [sending, setSending]           = useState(false);
    const [sendError, setSendError]       = useState('');
    const [inviteResult, setInviteResult] = useState(null);
    const [inviting, setInviting]         = useState(false);
    const [inviteSent, setInviteSent]     = useState(false);
    const navigate = useNavigate();

    useEffect(() => {
        const unread = confessions.filter(c => c.hasUnread).length;
        document.title = unread > 0 ? `(${unread}) Unsaid` : 'Unsaid';
        return () => { document.title = 'Unsaid'; };
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
            const res = await api.post(`/confessions/${targetId.trim().toUpperCase()}`, message.trim());
            if (res.data?.status === 'MUTUAL') {
                closeSheet();
                setMutualConfessionId(res.data.confessionId);
            } else if (res.data?.status === 'INVITED') {
                setInviteResult({ confessionId: res.data.confessionId, email: res.data.invitedEmail });
                fetchConfessions();
            } else {
                setSheetOpen(false);
                setTargetId('');
                setMessage('');
                fetchConfessions();
            }
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
        setInviteResult(null);
        setInviteSent(false);
    };

    const handleSendInvite = async () => {
        setInviting(true);
        try {
            await api.post(`/confessions/${inviteResult.confessionId}/invite`);
            setInviteSent(true);
        } catch {
            setInviteSent(true);
        } finally {
            setInviting(false);
        }
    };

    const flatList = buildFlatList(confessions);
    const confessBreath = confessions.length === 0 && !loading;

    return (
        <AppShell
            onConfess={() => setSheetOpen(true)}
            confessBreath={confessBreath}
        >
            <div className="dashboard">

                {/* List */}
                <div className="dash-list">
                    {loading ? (
                        <SkeletonRows />
                    ) : fetchError ? (
                        <div className="dash-empty">
                            <p className="dash-empty-title" style={{ fontSize: '1rem' }}>Could not load</p>
                            <p className="dash-empty-sub">Check your connection and try again.</p>
                            <button className="btn-full btn-surface" style={{ marginTop: 20, maxWidth: 180 }} onClick={fetchConfessions}>
                                Retry
                            </button>
                        </div>
                    ) : confessions.length === 0 ? (
                        <EmptyState />
                    ) : (
                        flatList.map(item =>
                            item.type === 'header' ? (
                                <div key={item.key} className="dash-section-label">{item.label}</div>
                            ) : (
                                <ConfessionRow
                                    key={item.key}
                                    c={item.c}
                                    index={item.index}
                                    onClick={() => item.c.state !== 'INVITED' && navigate(`/chat/${item.c.id}`)}
                                />
                            )
                        )
                    )}
                </div>

                {/* FAB (mobile only — hidden on desktop via CSS) */}
                <button
                    className={`fab${confessBreath ? ' fab--breathe' : ''}`}
                    onClick={() => setSheetOpen(true)}
                    aria-label="New confession"
                >
                    <span style={{ fontSize: '1rem', lineHeight: 1 }}>✦</span>
                    Confess
                </button>

                {/* Mutual Crush Overlay */}
                {mutualConfessionId && (
                    <MutualCrushOverlay
                        onDone={() => {
                            api.post(`/confessions/${mutualConfessionId}/mutual-seen`).catch(() => {});
                            setMutualConfessionId(null);
                            navigate(`/chat/${mutualConfessionId}`);
                        }}
                    />
                )}

                {/* Confession sheet */}
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

                            {inviteResult ? (
                                <div className="invite-box">
                                    <p className="invite-sent" style={{ marginBottom: 6 }}>
                                        ✓ Confession saved
                                    </p>
                                    <p className="invite-msg">
                                        They're out there living life, completely unaware someone has a crush on them. Want to send them an invite to join Unsaid?
                                    </p>
                                    {inviteSent ? (
                                        <>
                                            <p className="invite-sent" style={{ marginTop: 10 }}>
                                                💌 Invite sent to {inviteResult.email}
                                            </p>
                                            <button className="btn-full btn-surface" style={{ marginTop: 12 }} onClick={closeSheet}>
                                                Done
                                            </button>
                                        </>
                                    ) : (
                                        <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
                                            <button
                                                className="btn-full btn-accent"
                                                onClick={handleSendInvite}
                                                disabled={inviting}
                                            >
                                                {inviting ? 'Sending…' : `Invite — ${inviteResult.email}`}
                                            </button>
                                            <button className="btn-full btn-surface" onClick={closeSheet}>
                                                Skip
                                            </button>
                                        </div>
                                    )}
                                </div>
                            ) : (
                                <button
                                    className="btn-full btn-accent"
                                    onClick={handleSendConfession}
                                    disabled={sending}
                                >
                                    {sending ? 'Sending…' : 'Send Anonymously'}
                                </button>
                            )}
                        </div>
                    </div>
                )}
            </div>
        </AppShell>
    );
};

export default Dashboard;
