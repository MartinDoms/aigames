import { AvatarPicker } from './features/lobby/avatar-picker.js';

export const createLobbyForm = () => ({
  avatarPicker: AvatarPicker,

  formData: {
    playerName: '',
    name: '',
    privacy: 'PRIVATE',
  },
  isSubmitting: false,

  submitForm() {
    this.isSubmitting = true;

    // Create URL encoded form data from the formData object
    const formBody = new URLSearchParams();
    Object.keys(this.formData).forEach((key) => {
      formBody.append(key, this.formData[key]);
    });

    formBody.append('playerAvatar', this.avatarPicker.selectedAvatarId)

    // Send the form data to the server
    fetch('/lobbies', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: formBody,
    })
      .then((response) => {
        if (!response.ok) {
          throw new Error('Network response was not ok');
        }
        return response.text();
      })
      .then((responseText) => {
        // Split the response to get lobby short code and player ID
        const parts = responseText.split('|');
        if (parts.length > 1) {
          const lobbyShortCode = parts[0];
          const playerId = parts[1];

          // Store player ID with lobby-specific key (now using short code)
          localStorage.setItem(`playerId_${lobbyShortCode}`, playerId);

          // Navigate to the lobby using the short code
          window.location.href = '/lobbies/' + lobbyShortCode;
        } else {
          window.location.href = '/lobbies/' + responseText;
        }
      })
      .catch((error) => {
        console.error('Error:', error);
        alert('There was a problem creating the lobby. Please try again.');
        this.isSubmitting = false;
      });
  },
});
