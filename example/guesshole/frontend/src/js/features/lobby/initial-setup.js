// Initial Setup Component
export const InitialSetup = {
  showSetupModal: false,
  playerNameInput: null,
  selectedAvatarId: 'avatar1',
  lobbyManager: null,

  init() {
    // Reference to the lobby manager will be set when initialized
  },

  openSetupModal() {
    this.showSetupModal = true;
  },

  closeSetupModal() {
    this.showSetupModal = false;
  },

  saveInitialSetup() {
    if (!this.lobbyManager) {
      console.error('Lobby manager reference not set');
      return;
    }

    // Update lobby manager with the chosen values
    this.lobbyManager.playerState.playerNameInput =
      this.playerNameInput.trim() || 'Anonymous player';
    this.lobbyManager.playerState.playerName =
      this.playerNameInput.trim() || 'Anonymous player';
    this.lobbyManager.playerState.playerAvatar = this.selectedAvatarId;
    this.lobbyManager.avatarPicker.selectedAvatarId = this.selectedAvatarId;

    // Save to local storage
    this.lobbyManager.savePlayerData();

    // Close the modal
    this.closeSetupModal();

    // Connect to WebSocket
    this.lobbyManager.setupWebSocket();
  },
};
