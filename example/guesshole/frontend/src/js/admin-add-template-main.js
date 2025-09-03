import '../css/input.css';
import Alpine from 'alpinejs';

// Global variables for YouTube API and map
let youtubePlayer = null;
let youtubeAPILoaded = false;
let youtubeAPICallbacks = [];
let map = null;
let marker = null;

// Load YouTube API
function loadYouTubeAPI() {
  if (!document.getElementById('youtube-api')) {
    const tag = document.createElement('script');
    tag.id = 'youtube-api';
    tag.src = 'https://www.youtube.com/iframe_api';
    const firstScriptTag = document.getElementsByTagName('script')[0];
    firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);
  }
}

// Called by YouTube API when ready
window.onYouTubeIframeAPIReady = function () {
  youtubeAPILoaded = true;
  youtubeAPICallbacks.forEach((callback) => callback());
  youtubeAPICallbacks = [];
};

// Extract YouTube video ID from URL
function extractYoutubeId(url) {
  if (!url) return null;

  // Match standard YouTube URL formats
  const standardRegex =
    /(?:youtube\.com\/(?:[^\/]+\/.+\/|(?:v|e(?:mbed)?)\/|.*[?&]v=)|youtu\.be\/)([^"&?\/\s]{11})/i;
  const standardMatch = url.match(standardRegex);

  if (standardMatch && standardMatch[1]) {
    return standardMatch[1];
  }

  // Try to match the ID directly if it's just the ID
  if (/^[a-zA-Z0-9_-]{11}$/.test(url)) {
    return url;
  }

  return null;
}

// Initialize YouTube player
function initYouTubePlayer(videoId, containerId, onPlayerReady) {
  if (!youtubeAPILoaded) {
    youtubeAPICallbacks.push(() =>
      initYouTubePlayer(videoId, containerId, onPlayerReady),
    );
    return;
  }

  if (youtubePlayer) {
    youtubePlayer.destroy();
  }

  youtubePlayer = new YT.Player(containerId, {
    height: '100%',
    width: '100%',
    videoId: videoId,
    playerVars: {
      controls: 1,
      modestbranding: 1,
      rel: 0,
    },
    events: {
      onReady: onPlayerReady,
    },
  });
}

// Initialize Leaflet map
function initMap(lat = 0, lng = 0, onClick) {
  // Default to world view if no coordinates are provided
  const zoom = lat === 0 && lng === 0 ? 2 : 12;

  // Create the map if it doesn't exist
  if (!map) {
    map = L.map('map-container').setView([lat, lng], zoom);

    // Add tile layer (OpenStreetMap)
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution:
        '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    }).addTo(map);

    // Add click handler
    map.on('click', onClick);
  } else {
    // Just update the view if map already exists
    map.setView([lat, lng], zoom);
  }

  // Force map to recalculate its size
  setTimeout(() => {
    map.invalidateSize();
  }, 100);
}

// Update or create marker
function updateMarker(lat, lng) {
  // Remove existing marker if any
  if (marker) {
    map.removeLayer(marker);
  }

  // Add new marker
  marker = L.marker([lat, lng], { draggable: true }).addTo(map);

  // Return the marker for event handlers
  return marker;
}

// Format seconds to MM:SS
function formatTime(seconds) {
  if (!seconds && seconds !== 0) return '00:00';

  const mins = Math.floor(seconds / 60);
  const secs = Math.floor(seconds % 60);

  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
}

// Format ISO date
function formatDate(isoString) {
  if (!isoString) return 'Unknown';

  const date = new Date(isoString);
  return date.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

// Nominatim geocoding service URL
const NOMINATIM_URL = 'https://nominatim.openstreetmap.org/search';

// Search for a location using Nominatim
async function searchLocationByQuery(query) {
  if (!query || query.trim().length < 3) {
    return [];
  }

  const params = new URLSearchParams({
    q: query,
    format: 'json',
    addressdetails: 1,
    limit: 5,
  });

  try {
    const response = await fetch(`${NOMINATIM_URL}?${params.toString()}`, {
      headers: {
        'Accept-Language': 'en-US,en;q=0.9',
        'User-Agent': 'GuessHole-AdminTool',
      },
    });

    if (!response.ok) {
      throw new Error(`Search request failed: ${response.status}`);
    }

    const results = await response.json();

    // Format the results for easier display
    return results.map((result) => ({
      lat: parseFloat(result.lat),
      lng: parseFloat(result.lon),
      name:
        result.name ||
        formatMainAddress(result.address) ||
        result.display_name.split(',')[0],
      displayName: result.display_name,
      boundingBox: result.boundingbox
        ? [
            parseFloat(result.boundingbox[0]), // south
            parseFloat(result.boundingbox[2]), // west
            parseFloat(result.boundingbox[1]), // north
            parseFloat(result.boundingbox[3]), // east
          ]
        : null,
    }));
  } catch (error) {
    console.error('Error searching for location:', error);
    throw error;
  }
}

// Helper to format the main part of an address
function formatMainAddress(address) {
  if (!address) return null;

  // Try to pick the most relevant part of the address
  // Order of preference for display
  const addressParts = [];

  if (address.tourism) addressParts.push(address.tourism);
  if (address.attraction) addressParts.push(address.attraction);
  if (address.building) addressParts.push(address.building);
  if (address.natural) addressParts.push(address.natural);
  if (address.road) {
    const roadWithNumber = [address.house_number, address.road]
      .filter(Boolean)
      .join(' ');
    addressParts.push(roadWithNumber);
  }
  if (address.city) addressParts.push(address.city);
  if (address.town) addressParts.push(address.town);
  if (address.village) addressParts.push(address.village);
  if (address.suburb) addressParts.push(address.suburb);
  if (address.state) addressParts.push(address.state);
  if (address.country) addressParts.push(address.country);

  return addressParts[0] || null;
}

// Alpine.js component for adding round templates
document.addEventListener('alpine:init', () => {
  Alpine.data('addRoundTemplate', () => ({
    // Form data
    youtubeUrl: '',
    youtubeVideoId: null,
    startTime: 300, // Default to 5 minutes (300 seconds)
    videoLength: null,
    latitude: null,
    longitude: null,

    // Location search
    locationSearchQuery: '',
    searchResults: [],
    isSearching: false,
    searchError: '',
    searchPerformed: false,

    // UI state
    isSubmitting: false,
    showSuccess: false,
    errors: {},
    recentTemplates: [],

    // Initialize component
    init() {
      // Load YouTube API
      loadYouTubeAPI();

      // Initialize map
      this.$nextTick(() => {
        initMap(0, 0, (e) => {
          const { lat, lng } = e.latlng;
          this.latitude = lat.toFixed(6);
          this.longitude = lng.toFixed(6);

          const newMarker = updateMarker(lat, lng);

          // Update coordinates when marker is dragged
          newMarker.on('dragend', (event) => {
            const position = newMarker.getLatLng();
            this.latitude = position.lat.toFixed(6);
            this.longitude = position.lng.toFixed(6);
          });
        });
      });

      // Add event listener to search input for Enter key
      this.$nextTick(() => {
        const searchInput = document.getElementById('location-search');
        if (searchInput) {
          searchInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
              e.preventDefault();
              this.searchLocation();
            }
          });
        }
      });

      // Load recent templates
      this.loadRecentTemplates();
    },

    // Process YouTube URL to extract video ID
    processYoutubeUrl(forceRefresh = false) {
      this.errors.youtubeUrl = '';
      const videoId = extractYoutubeId(this.youtubeUrl);

      if (!videoId) {
        if (forceRefresh) {
          this.errors.youtubeUrl = 'Invalid YouTube URL or ID';
        }
        return;
      }

      // If already loaded the same video and not forcing refresh, skip
      if (this.youtubeVideoId === videoId && !forceRefresh) {
        return;
      }

      this.youtubeVideoId = videoId;

      // Initialize player after a short delay to ensure container is ready
      this.$nextTick(() => {
        initYouTubePlayer(videoId, 'youtube-player', (event) => {
          try {
            // Get video duration
            const duration = event.target.getDuration();
            if (duration > 0) {
              this.videoLength = Math.floor(duration);
            }
          } catch (e) {
            console.error('Error getting video information:', e);
          }
        });
      });
    },

    // Load recently added templates
    async loadRecentTemplates() {
      try {
        const response = await fetch(
          '/admin/api/round-templates/recent?limit=5',
        );
        if (response.ok) {
          this.recentTemplates = await response.json();
        } else {
          console.error('Failed to load recent templates');
        }
      } catch (error) {
        console.error('Error loading recent templates:', error);
      }
    },

    // Save the new template
    async saveTemplate() {
      // Clear previous errors
      this.errors = {};

      // Validate form
      let isValid = true;

      if (!this.youtubeVideoId) {
        this.errors.youtubeUrl = 'A valid YouTube URL is required';
        isValid = false;
      }

      if (!this.latitude || !this.longitude) {
        this.errors.location = 'Please select a location on the map';
        isValid = false;
      }

      if (!isValid) {
        return;
      }

      this.isSubmitting = true;

      try {
        const response = await fetch('/admin/api/round-templates', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            youtubeVideoId: this.youtubeVideoId,
            startTime: parseInt(this.startTime) || 300,
            videoLength: this.videoLength,
            latitude: parseFloat(this.latitude),
            longitude: parseFloat(this.longitude),
            source: 'admin_page',
          }),
        });

        if (response.ok) {
          const result = await response.json();
          console.log('Template created:', result);

          // Show success message and reload recent templates
          this.showSuccess = true;
          this.loadRecentTemplates();
        } else {
          const error = await response.json();
          throw new Error(error.message || 'Failed to create template');
        }
      } catch (error) {
        console.error('Error saving template:', error);
        this.showToast(error.message || 'Failed to save template', 'error');
      } finally {
        this.isSubmitting = false;
      }
    },

    // Search for a location
    async searchLocation() {
      if (
        !this.locationSearchQuery ||
        this.locationSearchQuery.trim().length < 3
      ) {
        this.searchError = 'Please enter at least 3 characters to search';
        return;
      }

      this.searchError = '';
      this.isSearching = true;
      this.searchPerformed = true;

      try {
        this.searchResults = await searchLocationByQuery(
          this.locationSearchQuery,
        );
      } catch (error) {
        this.searchError = 'Error searching for location. Please try again.';
        this.searchResults = [];
      } finally {
        this.isSearching = false;
      }
    },

    // Select a search result
    selectSearchResult(result) {
      this.latitude = result.lat.toFixed(6);
      this.longitude = result.lng.toFixed(6);

      // Clear search results
      this.searchResults = [];

      // Update the map view and add a marker
      if (result.boundingBox) {
        // If there's a bounding box, fit the map to it
        const southWest = L.latLng(
          result.boundingBox[0],
          result.boundingBox[1],
        );
        const northEast = L.latLng(
          result.boundingBox[2],
          result.boundingBox[3],
        );
        const bounds = L.latLngBounds(southWest, northEast);
        map.fitBounds(bounds);
      } else {
        // Otherwise just center on the point
        map.setView([result.lat, result.lng], 14);
      }

      // Add marker
      const newMarker = updateMarker(result.lat, result.lng);

      // Update coordinates when marker is dragged
      newMarker.on('dragend', (event) => {
        const position = newMarker.getLatLng();
        this.latitude = position.lat.toFixed(6);
        this.longitude = position.lng.toFixed(6);
      });
    },

    // Reset form to initial state
    resetForm() {
      this.youtubeUrl = '';
      this.youtubeVideoId = null;
      this.startTime = 300;
      this.videoLength = null;
      this.latitude = null;
      this.longitude = null;
      this.locationSearchQuery = '';
      this.searchResults = [];
      this.searchError = '';
      this.searchPerformed = false;
      this.errors = {};

      // Destroy player if exists
      if (youtubePlayer) {
        youtubePlayer.destroy();
        youtubePlayer = null;
      }

      // Reset map view
      if (map) {
        map.setView([0, 0], 2);
      }

      // Remove marker if exists
      if (marker) {
        map.removeLayer(marker);
        marker = null;
      }
    },

    // Helper function to format time
    formatTime(seconds) {
      return formatTime(seconds);
    },

    // Helper function to format date
    formatDate(isoString) {
      return formatDate(isoString);
    },

    // Show toast message
    showToast(message, type = 'info') {
      // Simple toast implementation
      const toast = document.createElement('div');
      toast.className = `fixed bottom-4 right-4 px-4 py-2 rounded-lg shadow-lg z-50 ${
        type === 'success'
          ? 'bg-green-500'
          : type === 'error'
            ? 'bg-red-500'
            : 'bg-blue-500'
      } text-white`;
      toast.textContent = message;
      document.body.appendChild(toast);

      // Remove after 3 seconds
      setTimeout(() => {
        toast.classList.add('opacity-0', 'transition-opacity', 'duration-500');
        setTimeout(() => toast.remove(), 500);
      }, 3000);
    },
  }));
});

// Initialize Alpine.js
Alpine.start();
