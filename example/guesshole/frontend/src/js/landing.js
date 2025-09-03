import '../css/input.css';
/**
 * Landing page functionality
 * Handles map initialization and solo game creation
 */
document.addEventListener('DOMContentLoaded', function () {
  // Initialize map functionality
  initializeMap();

  // Find all solo game buttons and att   ach event listeners
  const soloButtons = document.querySelectorAll('[data-solo-game]');

  soloButtons.forEach((button) => {
    button.addEventListener('click', function (e) {
      e.preventDefault();
      createSoloGame();
    });
  });

  const joinWithLobbyCodeButtons = document.querySelectorAll('[data-join-lobby]');
  joinWithLobbyCodeButtons.forEach((button) => {
      button.addEventListener('click', function (e) {
        e.preventDefault();
        joinWithLobbyCode();
      });
    });

  const lobbyCodeInput = document.getElementById('lobbyCodeInput');
  if (lobbyCodeInput) {
    lobbyCodeInput.addEventListener('keydown', (event) => {
      if (event.key === 'Enter') {
        event.preventDefault(); // Prevent default form submission
        joinWithLobbyCode(); // Trigger the same join action
      }
    });
  }
});

/**
 * Initializes the interactive map on the landing page
 */
function initializeMap() {
  // Map initialization is already handled in the inline script
  // This function is a placeholder in case you want to add more map functionality later
}

/**
 * Creates a solo game by submitting a form with pre-filled values
 */
function createSoloGame() {
  // Create a form data object with pre-filled values for solo play
  const formData = new FormData();
  formData.append('name', 'Solo Game');
  formData.append('playerName', 'Player');
  formData.append('privacy', 'PRIVATE');

  // Also set a default avatar
  const defaultAvatar = 'avatar4';

  // Convert FormData to URLSearchParams for application/x-www-form-urlencoded format
  const urlEncodedData = new URLSearchParams();
  for (const pair of formData.entries()) {
    urlEncodedData.append(pair[0], pair[1]);
  }

  // Submit the form data to create a lobby
  fetch('/lobbies', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: urlEncodedData,
  })
    .then((response) => {
      if (!response.ok) {
        throw new Error('Network response was not ok');
      }
      return response.text();
    })
    .then((data) => {
      // The response contains shortCode|playerId
      const parts = data.split('|');
      if (parts.length === 2) {
        const lobbyShortCode = parts[0];
        const playerId = parts[1];

        // Store player ID with lobby-specific key
        localStorage.setItem(`playerId_${lobbyShortCode}`, playerId);

        // Store player name and avatar for this lobby
        localStorage.setItem(`playerName_${lobbyShortCode}`, 'Player');
        localStorage.setItem(`playerAvatar_${lobbyShortCode}`, 'avatar4');

        // Redirect to the newly created lobby
        window.location.href = `/lobbies/${lobbyShortCode}`;
      } else {
        throw new Error('Invalid response format');
      }
    })
    .catch((error) => {
      console.error('Error creating solo game:', error);
      // Remove loading indicator
      document.body.removeChild(loadingDiv);

      // Show error message
      alert('Failed to create a solo game. Please try again.');
    });
}

async function joinWithLobbyCode() {
  const lobbyCodeInput = document.getElementById('lobbyCodeInput');
  const lobbyCode = lobbyCodeInput.value.trim().toUpperCase();

  // Clear any existing error styling
  lobbyCodeInput.classList.remove('border-red-500', 'border-2');

  // Remove any existing error message
  const existingError = document.querySelector('.lobby-error-message');
  if (existingError) {
    existingError.remove();
  }

  // 1. Check if the lobby code matches the regex pattern
  const pattern = /^[A-Z0-9]{6}$/;
  if (!pattern.test(lobbyCode)) {
    displayError('Please enter a valid 6-character lobby code (letters and numbers only)');
    return;
  }

  try {
    // 2. Check if the lobby exists by fetching the details endpoint
    const response = await fetch(`/api/lobbies/${lobbyCode}`);

    if (response.status === 404) {
      displayError('Lobby not found. Please check your lobby code and try again.');
      return;
    }

    if (!response.ok) {
      displayError('Unable to connect to lobby. Please try again later.');
      return;
    }

    // 4. If it exists, redirect to the lobby page
    window.location.href = `/lobbies/${lobbyCode}`;

  } catch (error) {
    console.error('Error checking lobby:', error);
    displayError('Unable to connect to lobby. Please check your connection and try again.');
  }
}

function displayError(message) {
  const lobbyCodeInput = document.getElementById('lobbyCodeInput');

  // Add error styling to input
  lobbyCodeInput.classList.add('border-red-500', 'border-2');

  // Create and display error message
  const errorDiv = document.createElement('div');
  errorDiv.className = 'lobby-error-message text-red-500 text-sm mt-1';
  errorDiv.textContent = message;

  // Insert error message after the input
  lobbyCodeInput.parentNode.insertBefore(errorDiv, lobbyCodeInput.nextSibling);
}
