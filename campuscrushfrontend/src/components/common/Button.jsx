import React from 'react';
import classNames from 'classnames';

const Button = ({
    children,
    variant = 'primary',
    className,
    loading,
    disabled,
    ...props
}) => {
    return (
        <button
            className={classNames('btn', `btn-${variant}`, className)}
            disabled={disabled || loading}
            {...props}
        >
            {loading ? (
                <span className="loader">Loading...</span> // Simple text loader for now
            ) : (
                children
            )}
        </button>
    );
};

export default Button;
