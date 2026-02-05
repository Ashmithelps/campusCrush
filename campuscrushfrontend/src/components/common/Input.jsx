import React from 'react';

const Input = ({ label, error, ...props }) => {
    return (
        <div className="input-group" style={{ marginBottom: '1rem' }}>
            {label && (
                <label style={{
                    display: 'block',
                    marginBottom: '0.5rem',
                    color: 'hsl(var(--text-main))',
                    fontWeight: 500
                }}>
                    {label}
                </label>
            )}
            <input className="input" {...props} />
            {error && (
                <span style={{
                    color: 'hsl(var(--danger))',
                    fontSize: '0.875rem',
                    marginTop: '0.25rem',
                    display: 'block'
                }}>
                    {error}
                </span>
            )}
        </div>
    );
};

export default Input;
