// WebSocket service for handling real-time communication
export const WebSocketService = {
  socket: null,
  url: null,
  onOpen: null,
  onMessage: null,
  onError: null,
  onClose: null,

  connect(url) {
    this.url = url;

    // Close existing socket if it exists
    if (this.socket) {
      this.socket.close();
    }

    try {
      this.socket = new WebSocket(url);

      this.socket.onopen = () => {
        if (this.onOpen) this.onOpen();
      };

      this.socket.onmessage = (event) => {
        if (this.onMessage) this.onMessage(event);
      };

      this.socket.onerror = (error) => {
        if (this.onError) this.onError(error);
      };

      this.socket.onclose = (event) => {
        if (this.onClose) this.onClose(event);
      };

      return true;
    } catch (error) {
      if (this.onError) this.onError(error);
      return false;
    }
  },

  send(message) {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
      throw new Error('WebSocket is not connected');
    }

    this.socket.send(JSON.stringify(message));
  },

  isConnected() {
    return this.socket && this.socket.readyState === WebSocket.OPEN;
  },

  disconnect() {
    if (this.socket) {
      this.socket.close();
      this.socket = null;
    }
  },
};
