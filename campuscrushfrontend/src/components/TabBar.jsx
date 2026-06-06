import React from 'react';
import { useNavigate } from 'react-router-dom';

const TabBar = ({ active }) => {
    const navigate = useNavigate();

    return (
        <nav className="tab-bar" aria-label="Main navigation">
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
    );
};

export default TabBar;
