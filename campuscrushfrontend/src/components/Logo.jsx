import React from 'react';

// Shared animated ellipsis logo mark for Unsaid.
//
// size    — rendered width in px; height is always size/4
// settled — false (default): full entrance spring on each dot, used on the landing page
//           true: dots are already visible; only dot-3 breathes, used when the mark
//                 is already "present" in the scene (auth-choice and beyond)
const Logo = ({ size = 48, settled = false }) => {
    const cls = settled ? 'logo-dot logo-dot-settled' : 'logo-dot';
    return (
        <svg
            width={size}
            height={Math.round(size / 4)}
            viewBox="0 0 48 12"
            fill="none"
            role="img"
            aria-label="Unsaid"
        >
            <circle className={`${cls} logo-dot-1`} cx="6"  cy="6" r="4" fill="#C25C3D" />
            <circle className={`${cls} logo-dot-2`} cx="24" cy="6" r="4" fill="#C25C3D" />
            <circle className={`${cls} logo-dot-3`} cx="42" cy="6" r="4" fill="#C25C3D" />
        </svg>
    );
};

export default Logo;
