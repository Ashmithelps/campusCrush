import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import Logo from '../components/Logo';
import ThemeToggle from '../components/ThemeToggle';
import { useAuth } from '../context/AuthContext';
import { apiError } from '../services/api';

const AppShell = ({ children, onConfess, confessBreath }) => {
    const { logout, user } = useAuth();
    const navigate = useNavigate();
    const { pathname } = useLocation();
    const [logoutSheetOpen, setLogoutSheetOpen] = useState(false);

    const active = pathname === '/dashboard' ? 'inbox'
                 : pathname === '/feed'      ? 'feed'
                 : null;

    return (
        <div className="app-shell">

            {/* ── Desktop sidebar ── */}
            <nav className="app-sidebar" aria-label="Main navigation">
                <div className="app-sidebar-brand">
                    <Logo size={26} settled />
                    {user?.rollNumber && (
                        <span className="app-sidebar-roll data">{user.rollNumber}</span>
                    )}
                </div>

                <div className="app-sidebar-nav">
                    <button
                        className={`app-sidebar-item${active === 'inbox' ? ' app-sidebar-item--active' : ''}`}
                        onClick={() => navigate('/dashboard')}
                        aria-label="Inbox"
                        aria-current={active === 'inbox' ? 'page' : undefined}
                    >
                        <span className="app-sidebar-icon" aria-hidden="true">✉</span>
                        Inbox
                    </button>
                    <button
                        className={`app-sidebar-item${active === 'feed' ? ' app-sidebar-item--active' : ''}`}
                        onClick={() => navigate('/feed')}
                        aria-label="Feed"
                        aria-current={active === 'feed' ? 'page' : undefined}
                    >
                        <span className="app-sidebar-icon" aria-hidden="true">✦</span>
                        Feed
                    </button>
                </div>

                {onConfess && (
                    <button
                        className={`app-sidebar-confess${confessBreath ? ' app-sidebar-confess--breathe' : ''}`}
                        onClick={onConfess}
                        aria-label="New confession"
                    >
                        <span aria-hidden="true" style={{ fontSize: '0.9rem' }}>✦</span>
                        Confess
                    </button>
                )}

                <div className="app-sidebar-bottom">
                    <ThemeToggle className="btn-theme" />
                    <button
                        className="btn-ghost dash-logout"
                        onClick={() => setLogoutSheetOpen(true)}
                        aria-label="Sign out"
                    >
                        ↪
                    </button>
                </div>
            </nav>

            {/* ── Mobile header ── */}
            <header className="app-header">
                <div className="dash-header-left">
                    <Logo size={26} settled />
                    {user?.rollNumber && (
                        <div className="dash-header-roll data">{user.rollNumber}</div>
                    )}
                </div>
                <div className="dash-header-right">
                    <ThemeToggle className="btn-theme" />
                    <button
                        className="btn-ghost dash-logout"
                        onClick={() => setLogoutSheetOpen(true)}
                        aria-label="Sign out"
                    >
                        ↪
                    </button>
                </div>
            </header>

            {/* ── Page content ── */}
            <div className="app-content">
                {children}
            </div>

            {/* ── Mobile tab bar ── */}
            <nav className="tab-bar app-tab-bar" aria-label="Main navigation">
                <button
                    className={`tab-bar-item${active === 'inbox' ? ' tab-bar-item--active' : ''}`}
                    onClick={() => navigate('/dashboard')}
                    aria-label="Inbox"
                    aria-current={active === 'inbox' ? 'page' : undefined}
                >
                    <span className="tab-bar-icon" aria-hidden="true">✉</span>
                    <span className="tab-bar-label">Inbox</span>
                </button>
                <button
                    className={`tab-bar-item${active === 'feed' ? ' tab-bar-item--active' : ''}`}
                    onClick={() => navigate('/feed')}
                    aria-label="Feed"
                    aria-current={active === 'feed' ? 'page' : undefined}
                >
                    <span className="tab-bar-icon" aria-hidden="true">✦</span>
                    <span className="tab-bar-label">Feed</span>
                </button>
            </nav>

            {/* ── Logout confirmation sheet ── */}
            {logoutSheetOpen && (
                <div className="sheet-overlay" onClick={() => setLogoutSheetOpen(false)}>
                    <div className="sheet" onClick={e => e.stopPropagation()}>
                        <div className="sheet-handle" />
                        <div className="sheet-title">Sign out?</div>
                        <p style={{ color: 'var(--text-2)', fontSize: '0.9rem', marginBottom: '1.25rem' }}>
                            You'll need your college email to get back in.
                        </p>
                        <button className="btn-full btn-danger" onClick={logout}>
                            Sign out
                        </button>
                        <button className="btn-full btn-surface" style={{ marginTop: 10 }} onClick={() => setLogoutSheetOpen(false)}>
                            Cancel
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
};

export default AppShell;
