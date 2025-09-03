export const LogService = {
  logs: [],
  maxLogs: 100,

  add(message, type = 'info') {
    const entry = {
      message,
      type,
      timestamp: new Date().toLocaleTimeString(),
    };

    this.logs.push(entry);

    // Keep a reasonable limit on log entries
    if (this.logs.length > this.maxLogs) {
      this.logs.shift();
    }

    // Log to console as well
    console.log(`[${type}] ${message}`);

    // Dispatch custom event for interested listeners
    document.dispatchEvent(
      new CustomEvent('logs-updated', {
        detail: { logs: this.logs },
      }),
    );

    return entry;
  },

  getLogs() {
    return [...this.logs]; // Return a copy to prevent unexpected mutations
  },
};
