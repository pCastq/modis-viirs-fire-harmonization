import '@testing-library/jest-dom/vitest';

// jsdom non ha ResizeObserver: un segnaposto basta, nei test non c'e' layout reale
class ResizeObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
}
globalThis.ResizeObserver ??= ResizeObserverStub;
