import React, { useEffect, useState, useCallback, useRef } from 'react';
import { motion, useMotionValue, useTransform, animate } from 'framer-motion';
import AppShell from '../layouts/AppShell';
import api, { apiError } from '../services/api';

const SWIPE_THRESHOLD = 100;
const ROTATION_RANGE  = 18;

const FeedSkeleton = () => (
    <div className="feed-deck-area">
        <div className="feed-card feed-card--skel">
            <div className="skel-line skel-pulse" style={{ width: '80%', height: 18 }} />
            <div className="skel-line skel-pulse" style={{ width: '65%', height: 18, marginTop: 12 }} />
            <div className="skel-line skel-pulse" style={{ width: '50%', height: 18, marginTop: 12 }} />
        </div>
    </div>
);

const EndState = ({ onRefresh }) => (
    <div className="feed-deck-area">
        <div className="feed-end">
            <svg width="28" height="28" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true" style={{ opacity: 0.35 }}>
                <circle cx="8"  cy="16" r="4" fill="var(--accent)" />
                <circle cx="16" cy="16" r="4" fill="var(--accent)" />
                <circle cx="24" cy="16" r="4" fill="var(--accent)" />
            </svg>
            <p className="feed-end-title">Nothing here yet</p>
            <p className="feed-end-sub">Be the first to post something.</p>
            <button className="btn-full btn-surface feed-end-btn" onClick={onRefresh}>
                Refresh
            </button>
        </div>
    </div>
);

const SwipeCard = ({ item, onDismiss, isTop, stackIndex }) => {
    const x       = useMotionValue(0);
    const rotate  = useTransform(x, [-200, 200], [-ROTATION_RANGE, ROTATION_RANGE]);
    const opacity = useTransform(x, [-160, -80, 0, 80, 160], [0, 1, 1, 1, 0]);

    const [reportOpen, setReportOpen] = useState(false);
    const [reported,   setReported]   = useState(false);

    const handleDragEnd = (_, info) => {
        if (Math.abs(info.offset.x) >= SWIPE_THRESHOLD) {
            const dir = info.offset.x > 0 ? 1 : -1;
            animate(x, dir * 500, { duration: 0.3 });
            onDismiss(item.id);
        } else {
            animate(x, 0, { type: 'spring', stiffness: 300, damping: 20 });
        }
    };

    const handleReport = async (reason) => {
        try {
            await api.post(`/feed/${item.id}/report`, { reason });
        } catch {
            // swallow
        }
        setReported(true);
        setReportOpen(false);
        setTimeout(() => onDismiss(item.id), 600);
    };

    const stackOffset = stackIndex * 6;

    return (
        <>
            <motion.div
                className={`feed-card${isTop ? ' feed-card--top' : ''}`}
                style={{
                    x: isTop ? x : 0,
                    rotate: isTop ? rotate : 0,
                    opacity: isTop ? opacity : 1,
                    zIndex: 10 - stackIndex,
                    top: stackOffset,
                    scale: 1 - stackIndex * 0.04,
                }}
                drag={isTop ? 'x' : false}
                dragConstraints={{ left: -400, right: 400 }}
                dragElastic={0.8}
                onDragEnd={isTop ? handleDragEnd : undefined}
                whileTap={isTop ? { cursor: 'grabbing' } : undefined}
                initial={{ opacity: 0, y: 30, scale: 0.94 }}
                animate={{ opacity: 1, y: 0, scale: 1 - stackIndex * 0.04 }}
                transition={{ type: 'spring', stiffness: 280, damping: 22 }}
            >
                {reported ? (
                    <p className="feed-card-reported">Reported. Thanks for keeping Unsaid safe.</p>
                ) : (
                    <>
                        <p className="feed-card-content">{item.content}</p>
                        <div className="feed-card-footer">
                            <span className="feed-card-views data">{item.viewCount} seen</span>
                            {isTop && (
                                <button
                                    className="feed-card-report-btn"
                                    onClick={() => setReportOpen(true)}
                                    aria-label="Report this confession"
                                >
                                    ⚑
                                </button>
                            )}
                        </div>
                    </>
                )}
            </motion.div>

            {reportOpen && (
                <div className="sheet-overlay" onClick={() => setReportOpen(false)}>
                    <div className="sheet" onClick={e => e.stopPropagation()}>
                        <div className="sheet-handle" />
                        <div className="sheet-title">Report confession</div>
                        {['Hate speech', 'Harassment', 'Spam', 'Other'].map(reason => (
                            <button
                                key={reason}
                                className="chat-sheet-item chat-sheet-item--danger"
                                onClick={() => handleReport(reason)}
                            >
                                {reason}
                            </button>
                        ))}
                        <button
                            className="chat-sheet-item"
                            style={{ marginTop: 8 }}
                            onClick={() => setReportOpen(false)}
                        >
                            Cancel
                        </button>
                    </div>
                </div>
            )}
        </>
    );
};

const Feed = () => {
    const [cards,       setCards]       = useState([]);
    const [loading,     setLoading]     = useState(true);
    const [fetchError,  setFetchError]  = useState(false);
    const [empty,       setEmpty]       = useState(false);
    const [composeOpen, setComposeOpen] = useState(false);
    const [draft,       setDraft]       = useState('');
    const [posting,     setPosting]     = useState(false);
    const [postError,   setPostError]   = useState('');
    const [postSuccess, setPostSuccess] = useState(false);

    const seenThisRound = useRef(new Set());
    const fetching      = useRef(false);

    const fetchFeed = useCallback(async () => {
        if (fetching.current) return;
        fetching.current = true;
        try {
            const res = await api.get('/feed');
            const items = res.data;
            if (items.length === 0) {
                setEmpty(true);
                setCards([]);
            } else {
                setEmpty(false);
                setCards(items);
                seenThisRound.current = new Set();
            }
            setFetchError(false);
        } catch {
            setFetchError(true);
        } finally {
            setLoading(false);
            fetching.current = false;
        }
    }, []);

    useEffect(() => { fetchFeed(); }, [fetchFeed]);

    const handleDismiss = useCallback((id) => {
        api.post(`/feed/${id}/view`).catch(() => {});

        seenThisRound.current.add(id);

        setCards(prev => {
            if (prev.length === 0) return prev;
            const [head, ...tail] = prev;
            const next = [...tail, head];

            if (seenThisRound.current.size >= prev.length) {
                seenThisRound.current = new Set();
                fetchFeed();
            }

            return next;
        });
    }, [fetchFeed]);

    const handleRefresh = () => {
        setCards([]);
        setEmpty(false);
        setLoading(true);
        seenThisRound.current = new Set();
        fetchFeed();
    };

    const handlePost = async (e) => {
        e.preventDefault();
        setPostError('');
        if (!draft.trim()) { setPostError('Write something first.'); return; }
        setPosting(true);
        try {
            const res = await api.post('/feed', { content: draft.trim() });
            setDraft('');
            setPostSuccess(true);
            setCards(prev => [res.data, ...prev]);
            setTimeout(() => {
                setPostSuccess(false);
                setComposeOpen(false);
            }, 1200);
        } catch (err) {
            setPostError(apiError(err, 'Could not post. Try again.'));
        } finally {
            setPosting(false);
        }
    };

    const closeCompose = () => {
        setComposeOpen(false);
        setDraft('');
        setPostError('');
        setPostSuccess(false);
    };

    const visibleCards = cards.slice(0, 3);

    return (
        <AppShell onConfess={() => setComposeOpen(true)}>
            <div className="feed-page">

                {/* Deck area */}
                {loading ? (
                    <FeedSkeleton />
                ) : fetchError ? (
                    <div className="feed-deck-area">
                        <div className="feed-end">
                            <p className="feed-end-title" style={{ fontSize: '1rem' }}>Could not load</p>
                            <p className="feed-end-sub">Check your connection.</p>
                            <button className="btn-full btn-surface feed-end-btn" onClick={handleRefresh}>
                                Retry
                            </button>
                        </div>
                    </div>
                ) : empty ? (
                    <EndState onRefresh={handleRefresh} />
                ) : (
                    <div className="feed-deck-area">
                        <div className="feed-deck">
                            {[...visibleCards].reverse().map((item, revIdx) => {
                                const stackIndex = visibleCards.length - 1 - revIdx;
                                return (
                                    <SwipeCard
                                        key={item.id}
                                        item={item}
                                        isTop={stackIndex === 0}
                                        stackIndex={stackIndex}
                                        onDismiss={handleDismiss}
                                    />
                                );
                            })}
                        </div>
                        <p className="feed-hint">Swipe to read the next one</p>
                    </div>
                )}

                {/* FAB (mobile only — hidden on desktop via CSS) */}
                <button
                    className="fab"
                    onClick={() => setComposeOpen(true)}
                    aria-label="Post a confession"
                >
                    <span style={{ fontSize: '1rem', lineHeight: 1 }}>✦</span>
                    Confess
                </button>

                {/* Compose sheet */}
                {composeOpen && (
                    <div className="sheet-overlay" onClick={closeCompose}>
                        <div className="sheet" onClick={e => e.stopPropagation()}>
                            <div className="sheet-handle" />
                            <div className="sheet-title">Post anonymously</div>

                            {postSuccess ? (
                                <p className="invite-sent" style={{ textAlign: 'center', padding: '24px 0' }}>
                                    ✓ Posted
                                </p>
                            ) : (
                                <>
                                    <div className="field">
                                        <textarea
                                            className="field-input feed-compose-textarea"
                                            placeholder="Something you've been holding in…"
                                            value={draft}
                                            onChange={e => setDraft(e.target.value.slice(0, 300))}
                                            rows={4}
                                            autoFocus
                                        />
                                        <span className="feed-compose-count data">{draft.length}/300</span>
                                    </div>

                                    {postError && (
                                        <p className="error-text" style={{ marginBottom: 14 }}>{postError}</p>
                                    )}

                                    <button
                                        className="btn-full btn-accent"
                                        onClick={handlePost}
                                        disabled={posting || !draft.trim()}
                                    >
                                        {posting ? 'Posting…' : 'Post anonymously'}
                                    </button>

                                    <p className="feed-compose-limit">3 posts per day · no identity is ever shared</p>
                                </>
                            )}
                        </div>
                    </div>
                )}
            </div>
        </AppShell>
    );
};

export default Feed;
