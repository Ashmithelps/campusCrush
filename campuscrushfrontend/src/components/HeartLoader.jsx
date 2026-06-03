import React from 'react';

const HeartLoader = ({ size = 90 }) => (
  <div className="heart-loader-wrap">
    <svg
      className="heart-loader"
      width={size}
      height={size}
      viewBox="0 0 100 100"
      xmlns="http://www.w3.org/2000/svg"
    >
      <defs>
        {/* Heart clip path */}
        <clipPath id="heartClip">
          <path d="M50 85
            C50 85 10 58 10 33
            C10 18 22 8 35 8
            C42 8 48 12 50 16
            C52 12 58 8 65 8
            C78 8 90 18 90 33
            C90 58 50 85 50 85Z" />
        </clipPath>

        {/* Wave gradient — pink to magenta */}
        <linearGradient id="waveGrad" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%"   stopColor="#ff2d55" />
          <stop offset="50%"  stopColor="#ff6b9d" />
          <stop offset="100%" stopColor="#bf5af2" />
        </linearGradient>

        {/* Shimmer on top of liquid */}
        <linearGradient id="shineGrad" x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%"  stopColor="rgba(255,255,255,0.25)" />
          <stop offset="100%" stopColor="rgba(255,255,255,0)" />
        </linearGradient>
      </defs>

      {/* Liquid wave group clipped to heart shape */}
      <g clipPath="url(#heartClip)">
        {/* Background fill (dark base so wave contrast is clear) */}
        <rect x="0" y="0" width="100" height="100" fill="#1a0a12" />

        {/* Wave 1 — main fill */}
        <path className="heart-wave heart-wave-1" fill="url(#waveGrad)" opacity="0.95"
          d="M-20 60 Q0 45 20 60 Q40 75 60 60 Q80 45 100 60 Q120 75 140 60 L140 110 L-20 110Z" />

        {/* Wave 2 — offset for depth */}
        <path className="heart-wave heart-wave-2" fill="url(#waveGrad)" opacity="0.5"
          d="M-20 65 Q5 52 25 65 Q45 78 65 65 Q85 52 105 65 Q125 78 145 65 L145 110 L-20 110Z" />

        {/* Shine overlay */}
        <path clipPath="url(#heartClip)"
          fill="url(#shineGrad)"
          d="M10 8 C22 8 35 8 50 16 C65 8 78 8 90 8 L90 45 L10 45Z"
          opacity="0.4" />
      </g>

      {/* Heart outline */}
      <path
        d="M50 85
          C50 85 10 58 10 33
          C10 18 22 8 35 8
          C42 8 48 12 50 16
          C52 12 58 8 65 8
          C78 8 90 18 90 33
          C90 58 50 85 50 85Z"
        fill="none"
        stroke="rgba(255,45,85,0.6)"
        strokeWidth="2"
      />
    </svg>

    <div className="heart-loader-text">campuscrush</div>
  </div>
);

export default HeartLoader;
