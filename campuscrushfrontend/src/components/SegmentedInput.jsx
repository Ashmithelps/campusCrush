import React, { useRef, useEffect, useImperativeHandle, forwardRef } from 'react';

const SegmentedInput = forwardRef(({
    length = 6,
    value,
    onChange,
    onComplete,
    status,
    disabled,
}, ref) => {
    const inputRefs = useRef([]);

    useImperativeHandle(ref, () => ({
        focus: (index = 0) => inputRefs.current[index]?.focus(),
        clear: () => {
            onChange(Array(length).fill(''));
            setTimeout(() => inputRefs.current[0]?.focus(), 0);
        },
    }));

    useEffect(() => {
        inputRefs.current[0]?.focus();
    }, []);

    const handleChange = (index, raw) => {
        if (!/^\d*$/.test(raw)) return;
        const digit = raw.slice(-1);
        const next = [...value];
        next[index] = digit;
        onChange(next);
        if (digit && index < length - 1) {
            inputRefs.current[index + 1]?.focus();
        }
        if (digit && next.every(d => d !== '') && onComplete) {
            onComplete(next.join(''));
        }
    };

    const handleKeyDown = (index, e) => {
        if (e.key === 'Backspace') {
            if (value[index]) {
                const next = [...value];
                next[index] = '';
                onChange(next);
            } else if (index > 0) {
                const next = [...value];
                next[index - 1] = '';
                onChange(next);
                inputRefs.current[index - 1]?.focus();
            }
        } else if (e.key === 'ArrowLeft' && index > 0) {
            e.preventDefault();
            inputRefs.current[index - 1]?.focus();
        } else if (e.key === 'ArrowRight' && index < length - 1) {
            e.preventDefault();
            inputRefs.current[index + 1]?.focus();
        }
    };

    const handlePaste = (e) => {
        e.preventDefault();
        const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, length);
        if (!pasted.length) return;
        const next = Array(length).fill('');
        pasted.split('').forEach((d, i) => { next[i] = d; });
        onChange(next);
        const focusIndex = Math.min(pasted.length, length - 1);
        inputRefs.current[focusIndex]?.focus();
        if (pasted.length === length && onComplete) {
            onComplete(pasted);
        }
    };

    const rowClass = [
        'vf-row',
        status === 'error' ? 'vf-row--shake' : '',
    ].filter(Boolean).join(' ');

    const boxClass = (index) => [
        'vf-box',
        `vf-box--${index}`,
        value[index] ? 'vf-box--filled' : '',
        status === 'error'   ? 'vf-box--error'   : '',
        status === 'success' ? 'vf-box--success'  : '',
    ].filter(Boolean).join(' ');

    return (
        <div
            className={rowClass}
            onPaste={handlePaste}
            role="group"
            aria-label={`${length}-digit verification code`}
        >
            {value.map((digit, i) => (
                <input
                    key={i}
                    ref={el => inputRefs.current[i] = el}
                    className={boxClass(i)}
                    type="text"
                    inputMode="numeric"
                    autoComplete={i === 0 ? 'one-time-code' : 'off'}
                    maxLength={1}
                    value={digit}
                    disabled={disabled || status === 'success'}
                    onChange={e => handleChange(i, e.target.value)}
                    onKeyDown={e => handleKeyDown(i, e)}
                    aria-label={`Digit ${i + 1} of ${length}`}
                />
            ))}
        </div>
    );
});

SegmentedInput.displayName = 'SegmentedInput';
export default SegmentedInput;
