class InMemoryCache {
  private readonly cache = new Map<string, { value: any; expiry: number }>();
  private readonly interval: NodeJS.Timeout | null = null;

  constructor(private readonly defaultTtlMs: number = 300000) { // Default 5 minutes
    // Clean up expired keys every 60 seconds
    this.interval = setInterval(() => this.cleanup(), 60000);
    // Unref the timer so it doesn't prevent Node process from exiting
    if (this.interval && typeof this.interval.unref === "function") {
      this.interval.unref();
    }
  }

  get<T>(key: string): T | undefined {
    const cached = this.cache.get(key);
    if (!cached) return undefined;
    if (Date.now() > cached.expiry) {
      this.cache.delete(key);
      return undefined;
    }
    return cached.value as T;
  }

  set<T>(key: string, value: T, ttlMs?: number): void {
    const ttl = ttlMs ?? this.defaultTtlMs;
    this.cache.set(key, {
      value,
      expiry: Date.now() + ttl,
    });
  }

  delete(key: string): void {
    this.cache.delete(key);
  }

  clear(): void {
    this.cache.clear();
  }

  private cleanup(): void {
    const now = Date.now();
    for (const [key, cached] of this.cache.entries()) {
      if (now > cached.expiry) {
        this.cache.delete(key);
      }
    }
  }
}

export const inMemoryCache = new InMemoryCache();
