import { WebSocketService } from '../../services/websocket-service.js';

/**
 * Creates a handler for the HEARTBEAT message type
 *
 * @param {Object} lobbyManager - The lobby manager instance
 * @returns {Function} - The message handler function
 */
export function createHeartbeatHandler(lobbyManager) {
  return function handleHeartbeat() {
    // Respond to heartbeat to keep the connection alive
    if (WebSocketService.isConnected()) {
      WebSocketService.send({ type: 'HEARTBEAT_ACK' });
    }
  };
}
