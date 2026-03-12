import { useEffect, useRef } from 'react';

/**
 * Debounce hook — returns a stable callback that delays invocation.
 */
export function useDebouncedCallback<T extends (...args: never[]) => void>(
    callback: T,
    delay: number,
): T {
    const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
    const callbackRef = useRef(callback);
    callbackRef.current = callback;

    useEffect(() => {
        return () => {
            if (timeoutRef.current) clearTimeout(timeoutRef.current);
        };
    }, []);

    return ((...args: Parameters<T>) => {
        if (timeoutRef.current) clearTimeout(timeoutRef.current);
        timeoutRef.current = setTimeout(() => {
            callbackRef.current(...args);
        }, delay);
    }) as T;
}
