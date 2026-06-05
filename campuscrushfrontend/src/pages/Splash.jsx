import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme } from '../context/ThemeContext';

const UnsaidLogo = ({ size = 72 }) => (
    <svg width={size} height={size} viewBox="0 0 72 72" fill="none" aria-hidden="true">
        <circle cx="36" cy="36" r="31" stroke="#C75D3F" strokeWidth="1.2" />
        <text
            x="36"
            y="51"
            textAnchor="middle"
            fontFamily="'Cormorant Garamond', Georgia, serif"
            fontSize="40"
            fontStyle="italic"
            fontWeight="600"
            fill="#C75D3F"
        >
            U
        </text>
    </svg>
);

const Splash = () => {
    const navigate = useNavigate();
    const { theme, toggleTheme } = useTheme();
    const [show, setShow] = useState(false);

    useEffect(() => {
        const t = setTimeout(() => setShow(true), 60);
        return () => clearTimeout(t);
    }, []);

    return (
        <div className="splash-page">
            <button className="splash-theme-toggle" onClick={toggleTheme}>
                {theme === 'dark' ? 'Light' : 'Dark'}
            </button>

            <div className={`splash-body${show ? ' splash-body--in' : ''}`}>

                <div className="splash-mark">
                    <UnsaidLogo size={72} />
                </div>

                <h1 className="splash-title">Unsaid</h1>

                <p className="splash-hook">
                    You had the feeling.<br />
                    We gave it somewhere to go.
                </p>

                <div className="splash-actions">
                    <button
                        className="splash-btn-primary"
                        onClick={() => navigate('/register')}
                    >
                        Begin
                    </button>
                    <button
                        className="splash-btn-ghost"
                        onClick={() => navigate('/login')}
                    >
                        I have an account
                    </button>
                </div>

            </div>
        </div>
    );
};

export default Splash;
