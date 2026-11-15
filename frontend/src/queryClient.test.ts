import { ApiError } from '@/api/client';
import { shouldRetry } from './queryClient';

describe('shouldRetry', () => {
  it('retries server and network failures a couple of times', () => {
    expect(shouldRetry(0, new ApiError(503, 'x', 'y'))).toBe(true);
    expect(shouldRetry(1, new ApiError(0, 'x', 'y'))).toBe(true);
    expect(shouldRetry(2, new ApiError(503, 'x', 'y'))).toBe(false);
  });

  it('never retries a 4xx: the answer would be the same', () => {
    expect(shouldRetry(0, new ApiError(404, 'x', 'y'))).toBe(false);
    expect(shouldRetry(0, new ApiError(413, 'x', 'y'))).toBe(false);
  });
});
