import { LogService } from '../../services/log-service.js';
import { WebSocketService } from '../../services/websocket-service.js';
import { SoundEffectsService } from '../../services/sound-effects-service.js';

export const GameMap = {
  map: null,
  marker: null,
  confirmMarker: null,
  selectedLocation: null,
  playerAvatarIcon: null,
  currentAvatarId: null,
  markerClickHandler: null, // Store reference to click handler for proper removal
  markerDragHandler: null, // Store reference to drag handler for proper removal
  isSubmitting: false, // Flag to prevent double-submissions

  // Store other players' guesses
  playerGuesses: new Map(), // Map of playerId -> { marker, line, distanceMarker }
  pendingPlayerGuesses: new Map(),

  currentGameLayerGroup: null, // this is the map layer that keeps all of the guess markers, lines etc

  initMap() {
    LogService.add('Initializing map', 'info');

    // Initialize the map - allow wrapping for user experience
    this.map = L.map('map', {
      worldCopyJump: true, // Enable jumping when scrolling across dateline
      minZoom: 1,
    }).setView([20, 0], 2);

    L.tileLayer(
      'https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}.webp',
      {
        maxZoom: 19,
        attribution:
          '&copy; <a href="https://openstreetmap.org/copyright">OpenStreetMap contributors</a>',
      },
    ).addTo(this.map);

    this.currentGameLayerGroup = L.layerGroup();
    this.currentGameLayerGroup.addTo(this.map);

    // Add click event to place/move marker
    this.map.on('click', (e) => {
      this.placeMarker(e.latlng);
    });

    this.map.on('zoomend', (ev) => {
      this.map.panTo(this.map.wrapLatLng(this.map.getCenter()), {
        animate: false,
      });
    });
  },

  // Helper method to normalize coordinates to the -180 to 180 range
  normalizeLatLng(latlng) {
    let lng = latlng.lng;

    // Normalize longitude to -180 to 180
    while (lng > 180) lng -= 360;
    while (lng < -180) lng += 360;

    return L.latLng(latlng.lat, lng);
  },

  applyPlayerGuess(
    player,
    guessLocation,
    actualLocation,
    distanceKm,
    isCurrentPlayerGuess,
  ) {
    this.renderPlayerGuess(
      player,
      guessLocation,
      actualLocation,
      distanceKm,
      isCurrentPlayerGuess,
    );
    this.renderGuessLine(
      player,
      guessLocation,
      actualLocation,
      distanceKm,
      isCurrentPlayerGuess,
    );

    // the actual marker only needs to be rendered once, so do it for the current player
    if (isCurrentPlayerGuess) {
      this.renderActualMarker(actualLocation);
    }
  },

  placeMarker(latlng) {
    // Check if we're in a scoreboard state
    if (this.gameState.lobbyState !== 'GAME_IN_PROGRESS') {
      LogService.add(
        `Cannot place marker while in state ${this.gameState.lobbyState}`,
        'warning',
      );
      return;
    }

    // If already submitted, don't allow changes
    if (this.hasSubmittedGuessForCurrentRound()) {
      LogService.add(
        'Cannot place marker: Already submitted for this round',
        'warning',
      );
      return;
    }

    // Make sure latlng is normalized
    const normalizedLatLng = this.normalizeLatLng(latlng);

    // Update the selected location
    this.selectedLocation = normalizedLatLng;

    const avatarId = this.playerState.playerAvatar;

    // Not submitted yet - just render the confirm button
    this.createConfirmMarkerIcon();

    // Clean up any existing marker and handlers first to prevent duplicates
    if (this.marker) {
      if (this.markerClickHandler) {
        this.marker.off('click', this.markerClickHandler);
        this.markerClickHandler = null;
      }

      if (this.markerDragHandler) {
        this.marker.off('dragend', this.markerDragHandler);
        this.markerDragHandler = null;
      }

      this.marker.setLatLng(normalizedLatLng);
      this.marker.setIcon(this.confirmAvatarIcon);
    } else {
      // Create a new marker with the confirm icon
      const markerOptions = {
        icon: this.confirmAvatarIcon,
        draggable: true,
      };

      this.marker = L.marker(normalizedLatLng, markerOptions).addTo(
        this.currentGameLayerGroup,
      );
    }

    // Create and store click handler
    this.markerClickHandler = () => {
      this.submitGuess();
    };

    // Create and store drag handler
    this.markerDragHandler = (event) => {
      // Normalize the position when dragging ends
      const normalizedPos = this.normalizeLatLng(event.target.getLatLng());
      event.target.setLatLng(normalizedPos);
      this.selectedLocation = normalizedPos;

      // Pan the map to show the normalized position
      this.map.panTo(normalizedPos);
    };

    // Add the stored handlers
    this.marker.on('click', this.markerClickHandler);
    this.marker.on('dragend', this.markerDragHandler);

    // Pan map to the normalized position so user can see where their marker actually is
    this.map.panTo(normalizedLatLng);

    SoundEffectsService.playPlop();
  },

  renderPlayerGuess(
    player,
    guessLocation,
    actualLocation,
    distanceKm,
    isCurrentPlayerGuess,
  ) {
    // Remove previous guess for this player if it exists
    // TODO I think this is unnecessary - check this
    if (this.playerGuesses.has(player.id)) {
      const elements = this.playerGuesses.get(player.id);
      if (elements.marker) this.map.removeLayer(elements.marker);
      if (elements.line) this.map.removeLayer(elements.line);
      if (elements.distanceMarker)
        this.map.removeLayer(elements.distanceMarker);
    }

    // Normalize coordinates
    const guessPos = this.normalizeLatLng(
      L.latLng(guessLocation.latitude, guessLocation.longitude),
    );
    const actualPos = this.normalizeLatLng(
      L.latLng(actualLocation.latitude, actualLocation.longitude),
    );

    // Create marker for player's guess
    const guessMarker = L.marker([guessPos.lat, guessPos.lng], {
      icon: this.createPlayerGuessIcon(player),
    }).addTo(this.currentGameLayerGroup);
    L.marker([guessPos.lat, guessPos.lng - 360], {
      icon: this.createPlayerGuessIcon(player),
    }).addTo(this.currentGameLayerGroup);
    L.marker([guessPos.lat, guessPos.lng + 360], {
      icon: this.createPlayerGuessIcon(player),
    }).addTo(this.currentGameLayerGroup);

    // Draw a line between the guess and actual location using normalized coordinates
    const guessLine = L.polyline(
      [
        [guessPos.lat, guessPos.lng],
        [actualPos.lat, actualPos.lng],
      ],
      { color: '#90adc6', dashArray: '5, 10', weight: 2 },
    ).addTo(this.currentGameLayerGroup);
    L.polyline(
      [
        [guessPos.lat, guessPos.lng - 360],
        [actualPos.lat, actualPos.lng - 360],
      ],
      { color: '#90adc6', dashArray: '5, 10', weight: 2 },
    ).addTo(this.currentGameLayerGroup);
    L.polyline(
      [
        [guessPos.lat, guessPos.lng + 360],
        [actualPos.lat, actualPos.lng + 360],
      ],
      { color: '#90adc6', dashArray: '5, 10', weight: 2 },
    ).addTo(this.currentGameLayerGroup);

    // wait a second in case the map changes size, then move the map over the line
    if (isCurrentPlayerGuess) {
      setTimeout(() => {
        this.map.invalidateSize();
        this.map.fitBounds([
          [guessPos.lat, guessPos.lng],
          [actualPos.lat, actualPos.lng],
        ]);
      }, 200);
    }

    // Calculate midpoint using normalized coordinates
    const midpoint = [
      (parseFloat(guessPos.lat) + parseFloat(actualPos.lat)) / 2,
      (parseFloat(guessPos.lng) + parseFloat(actualPos.lng)) / 2,
    ];

    const distanceLabel = L.divIcon({
      html: `<div class="px-2 py-1 bg-white bg-opacity-80 rounded shadow text-xs font-medium text-secondary">
                    ${player.name} - ${Math.round(distanceKm)} km
                   </div>`,
      className: 'distance-label',
      iconSize: [70, 18],
      iconAnchor: [35, 9],
    });

    const distanceMarker = L.marker(midpoint, { icon: distanceLabel }).addTo(
      this.currentGameLayerGroup,
    );
    L.marker(
      { lat: midpoint[0], lng: midpoint[1] - 360 },
      { icon: distanceLabel },
    ).addTo(this.currentGameLayerGroup);
    L.marker(
      { lat: midpoint[0], lng: midpoint[1] + 360 },
      { icon: distanceLabel },
    ).addTo(this.currentGameLayerGroup);

    // Store references to these elements
    this.playerGuesses.set(player.id, {
      marker: guessMarker,
      line: guessLine,
      distanceMarker: distanceMarker,
    });

    LogService.add(
      `Added guess visualization for player ${player.name}`,
      'info',
    );
  },

  renderGuessLine(player, guessLocation, actualLocation) {},

  renderActualMarker(actualLocation) {
    L.marker([actualLocation.latitude, actualLocation.longitude], {
      icon: this.createActualLocationIcon(),
    }).addTo(this.currentGameLayerGroup);

    L.marker([actualLocation.latitude, actualLocation.longitude - 360], {
      icon: this.createActualLocationIcon(),
    }).addTo(this.currentGameLayerGroup);

    L.marker([actualLocation.latitude, actualLocation.longitude + 360], {
      icon: this.createActualLocationIcon(),
    }).addTo(this.currentGameLayerGroup);
  },

  createConfirmMarkerIcon() {
    this.confirmAvatarIcon = L.divIcon({
      html: `<div class="flex flex-col items-center cursor-pointer">
                    <div class="rounded-full bg-white border-2 border-primary shadow-md overflow-hidden" style="width: 32px; height: 32px;">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-primary mx-auto my-auto" viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
                      </svg>
                    </div>
                    <div class="w-0 h-0 border-l-[8px] border-r-[8px] border-t-[8px] border-l-transparent border-r-transparent border-t-primary" style="margin-top: -2px;"></div>
                    <div class="bg-white text-primary px-2 py-1 rounded-full text-xs font-bold mt-1 border border-primary shadow-sm">
                      Confirm
                    </div>
                   </div>`,
      className: 'confirm-avatar-marker',
      iconSize: [80, 65],
      iconAnchor: [40, 38], // Position the anchor at the bottom of the pointer
    });

    return this.confirmAvatarIcon;
  },

  // Create a custom icon using the player's avatar
  createPlayerAvatarIcon(avatarId) {
    if (!avatarId) {
      avatarId = 'avatar1'; // Default avatar
    }

    // Get avatar URL - adjust this based on how your avatars are served
    const avatarUrl = `/images/avatars/${avatarId}.webp`;
    this.currentAvatarId = avatarId;

    this.playerAvatarIcon = L.divIcon({
      html: `<div class="flex flex-col items-center">
                    <div class="rounded-full bg-white border-2 border-primary shadow-md overflow-hidden" style="width: 32px; height: 32px;">
                      <img src="${avatarUrl}" class="w-full h-full object-cover" alt="Your location" />
                    </div>
                    <div class="w-0 h-0 border-l-[8px] border-r-[8px] border-t-[8px] border-l-transparent border-r-transparent border-t-primary" style="margin-top: -2px;"></div>
                   </div>`,
      className: 'player-avatar-marker',
      iconSize: [32, 40],
      iconAnchor: [16, 38], // Position the anchor at the bottom of the pointer
    });

    LogService.add(
      `Created player avatar marker with avatar ID: ${avatarId}`,
      'info',
    );
    return this.playerAvatarIcon;
  },

  // Create a custom icon for another player's guess
  createPlayerGuessIcon(player) {
    if (!player || !player.avatar) {
      return this.createPlayerAvatarIcon('avatar1'); // Fallback
    }

    const avatarUrl = `/images/avatars/${player.avatar}.webp`;

    return L.divIcon({
      html: `<div class="flex flex-col items-center">
                    <div class="rounded-full bg-white border-2 border-secondary shadow-md overflow-hidden" style="width: 28px; height: 28px;">
                      <img src="${avatarUrl}" class="w-full h-full object-cover" alt="${player.name}'s guess" />
                    </div>
                    <div class="w-0 h-0 border-l-[7px] border-r-[7px] border-t-[7px] border-l-transparent border-r-transparent border-t-secondary" style="margin-top: -2px;"></div>
                   </div>`,
      className: 'player-guess-marker',
      iconSize: [28, 36],
      iconAnchor: [14, 34],
    });
  },

  /**
   * Create a custom icon for the actual location
   */
  createActualLocationIcon() {
    return L.divIcon({
      html: `<div class="w-4 h-4 rounded-full bg-accent border-2 border-primary animate-pulse"></div>`,
      className: 'actual-location-marker',
      iconSize: [16, 16],
      iconAnchor: [8, 8],
    });
  },

  // Check if already submitted for this round
  hasSubmittedGuessForCurrentRound() {
    return this.gameState.hasSubmittedGuessForCurrentRound;
  },

  resetMarker() {
    // Don't allow resetting the marker if already submitted for this round
    if (this.hasSubmittedGuessForCurrentRound()) {
      LogService.add(
        'Cannot reset marker: Already submitted for this round',
        'warning',
      );
      return;
    }

    if (this.marker) {
      // Properly clean up event handlers
      if (this.markerClickHandler) {
        this.marker.off('click', this.markerClickHandler);
        this.markerClickHandler = null;
      }

      if (this.markerDragHandler) {
        this.marker.off('dragend', this.markerDragHandler);
        this.markerDragHandler = null;
      }

      this.map.removeLayer(this.marker);
      this.marker = null;
      this.selectedLocation = null;
    }

    // Also remove the confirm marker if it exists
    if (this.confirmMarker) {
      this.map.removeLayer(this.confirmMarker);
      this.confirmMarker = null;
    }
  },

  // Clear all result visualization elements
  clearMap() {
    if (this.currentGameLayerGroup) {
      this.currentGameLayerGroup.clearLayers();
    }
  },

  // Method to submit the guess
  submitGuess() {
    // Triple-check to absolutely ensure we can't submit twice
    if (this.hasSubmittedGuessForCurrentRound() || this.isSubmitting) {
      LogService.add(
        'Already submitted or currently submitting a guess for this round',
        'warning',
      );
      return;
    }

    if (!this.selectedLocation) {
      LogService.add('Cannot submit guess: No location selected', 'warning');
      return;
    }

    // Set flag to prevent double-submissions
    this.isSubmitting = true;

    // Make sure coordinates are normalized
    const normalizedLocation = this.normalizeLatLng(this.selectedLocation);

    const coordinates = {
      lat: normalizedLocation.lat,
      lng: normalizedLocation.lng,
    };

    LogService.add(
      `Map submit guess called with normalized coordinates: ${JSON.stringify(coordinates)}`,
      'info',
    );

    // Replace the confirm marker with the avatar icon
    if (this.marker) {
      // Remove ALL event listeners
      if (this.markerClickHandler) {
        this.marker.off('click', this.markerClickHandler);
        this.markerClickHandler = null;
      }

      if (this.markerDragHandler) {
        this.marker.off('dragend', this.markerDragHandler);
        this.markerDragHandler = null;
      }

      // Get the avatar icon
      const avatarId = this.playerState.playerAvatar;
      if (!this.playerAvatarIcon || this.currentAvatarId !== avatarId) {
        this.createPlayerAvatarIcon(avatarId);
      }

      try {
        // Change to avatar icon and make non-interactive
        this.marker.setIcon(this.playerAvatarIcon);
        this.marker.dragging.disable();
        // Completely disable all interactions with the marker
        this.marker.options.interactive = false;
        // Force update the marker (helps ensure changes take effect)
        this.marker.update();

        SoundEffectsService.playSound('plop2');
      } catch (e) {
        LogService.add(`Error updating marker: ${e}`, 'error');
      }
    }

    this.gameState.submitGuess();
  },

  // Store a player's guess (either add to map or store for later)
  storePlayerGuess(playerId, player, guess, hasUserSubmittedGuess) {
    // First, store the data
    const guessData = {
      playerId,
      player,
      guess,
    };

    // If player has already submitted their guess, show it immediately
    if (hasUserSubmittedGuess) {
      this.renderPlayerGuess(playerId, player, guess);
    } else {
      // Otherwise, store it for later display
      this.pendingPlayerGuesses.set(playerId, guessData);
      LogService.add(`Stored pending guess for player ${player.name}`, 'info');
    }
  },

  // Show all pending guesses after user has submitted their own
  showPendingGuesses() {
    LogService.add(
      `Showing ${this.pendingPlayerGuesses.size} pending player guesses`,
      'info',
    );

    this.pendingPlayerGuesses.forEach((data) => {
      this.renderPlayerGuess(data.playerId, data.player, data.guess);
    });

    // Clear pending guesses after showing them
    this.pendingPlayerGuesses.clear();
  },

  clearAllVisualizations() {
    // Explicitly remove player marker
    if (this.marker) {
      // Properly clean up event handlers
      if (this.markerClickHandler) {
        this.marker.off('click', this.markerClickHandler);
        this.markerClickHandler = null;
      }

      if (this.markerDragHandler) {
        this.marker.off('dragend', this.markerDragHandler);
        this.markerDragHandler = null;
      }

      // Remove the marker from the map
      this.map.removeLayer(this.marker);
      this.marker = null;
      this.selectedLocation = null;

      LogService.add('Player marker removed for new round', 'info');
    }

    this.clearMap();

    // Reset submission flag for new round
    this.isSubmitting = false;

    // Reset the map zoom and center to default values
    if (this.map) {
      this.map.setView([20, 0], 2); // Reset to the initial view coordinates and zoom level
      LogService.add('Map zoom and position reset for new round', 'info');
    }
  },

  refreshMap() {
    LogService.add('Map visibility changed, rendering map', 'info');
    // This will be called whenever the element becomes visible
    if (this.map) {
      // If map already exists, just invalidate its size
      setTimeout(() => {
        this.map.invalidateSize();
      }, 100);
    } else {
      // If map doesn't exist yet for some reason, initialize it
      this.initMap();
    }

    // crappy hack to stop rendering grey blocks on the map - sometimes
    setTimeout(() => {
      this.map.invalidateSize();
    }, 400);
  },

  invalidateSize() {
    this.map.invalidateSize();
  },
};
