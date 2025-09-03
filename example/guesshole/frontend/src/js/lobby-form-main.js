import '../css/input.css';

// Main entry point for the lobby functionality
import Alpine from 'https://cdn.jsdelivr.net/npm/alpinejs@3.x.x/dist/module.esm.js';
import { createLobbyForm } from './lobby-form.js';

// Make the component factory function globally available for Alpine to use
window.lobbyForm = createLobbyForm;

// Initialize Alpine
Alpine.start();
