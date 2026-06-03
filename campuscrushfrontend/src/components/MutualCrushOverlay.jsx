import React, { useEffect, useState } from 'react';

const HEARTS = [
  { size: 28, left: '10%',  delay: 0,    duration: 2.8 },
  { size: 18, left: '22%',  delay: 0.3,  duration: 3.2 },
  { size: 36, left: '38%',  delay: 0.1,  duration: 2.6 },
  { size: 22, left: '55%',  delay: 0.5,  duration: 3.0 },
  { size: 14, left: '68%',  delay: 0.2,  duration: 3.4 },
  { size: 30, left: '80%',  delay: 0.4,  duration: 2.9 },
  { size: 20, left: '90%',  delay: 0.6,  duration: 3.1 },
  { size: 16, left: '5%',   delay: 0.7,  duration: 2.7 },
  { size: 24, left: '46%',  delay: 0.15, duration: 3.3 },
  { size: 12, left: '74%',  delay: 0.55, duration: 3.5 },
];

const MutualCrushOverlay = ({ onDone }) => {
  const [showText, setShowText]   = useState(false);
  const [showBtn, setShowBtn]     = useState(false);

  useEffect(() => {
    const t1 = setTimeout(() => setShowText(true), 600);
    const t2 = setTimeout(() => setShowBtn(true), 2000);
    return () => { clearTimeout(t1); clearTimeout(t2); };
  }, []);

  return (
    <div className="mutual-overlay">
      {/* Floating hearts */}
      {HEARTS.map((h, i) => (
        <span
          key={i}
          className="mutual-heart"
          style={{
            fontSize: h.size,
            left: h.left,
            animationDelay: `${h.delay}s`,
            animationDuration: `${h.duration}s`,
          }}
        >
          💕
        </span>
      ))}

      {/* Centre content */}
      <div className={`mutual-content${showText ? ' mutual-content--visible' : ''}`}>
        <div className="mutual-emoji">💕</div>
        <h1 className="mutual-title">It's mutual</h1>
        <p className="mutual-sub">You both had feelings for each other all along.</p>

        {showBtn && (
          <button className="mutual-btn" onClick={onDone}>
            Open Chat →
          </button>
        )}
      </div>
    </div>
  );
};

export default MutualCrushOverlay;
