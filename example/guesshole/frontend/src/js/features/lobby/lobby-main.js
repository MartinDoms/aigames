import '../../../css/input.css';

// Main entry point for the lobby functionality
import Alpine from 'alpinejs';
import { createLobbyManager } from './lobby-manager.js';

// Make the component factory function globally available for Alpine to use
window.lobbyManager = createLobbyManager;

// Initialize Alpine
Alpine.start();
