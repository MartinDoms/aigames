import '../css/input.css';

import Alpine from 'alpinejs';

// YouTube API script
let youtubeAPILoaded = false;
let youtubeAPICallbacks = [];

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

// Initialize YouTube players for visible videos
function initYouTubePlayers(videos) {
  if (!youtubeAPILoaded) {
    youtubeAPICallbacks.push(() => initYouTubePlayers(videos));
    return;
  }

  videos.forEach((video) => {
    if (!video.player && document.getElementById(`player-${video.id}`)) {
      video.player = new YT.Player(`player-${video.id}`, {
        height: '100%',
        width: '100%',
        videoId: video.youtubeVideoId,
        playerVars: {
          start: video.startTime || 0,
          controls: 1,
          modestbranding: 1,
          rel: 0,
        },
        events: {
          onReady: (event) => {
            // Pause video initially to prevent autoplay
            event.target.pauseVideo();

            // Try to get video title if not already set
            if (!video.videoTitle) {
              try {
                video.videoTitle = event.target.getVideoData().title;
                // Trigger Alpine.js to update the UI
                window.dispatchEvent(
                  new CustomEvent('video-title-updated', { detail: video }),
                );
              } catch (e) {
                console.error('Error getting video title:', e);
              }
            }

            // Get video duration if not already set
            if (!video.videoLength) {
              try {
                const durationSeconds = Math.floor(event.target.getDuration());
                if (durationSeconds > 0) {
                  // Update the video object
                  video.videoLength = durationSeconds;

                  // Send the duration to the server
                  fetch(`/admin/api/videos/${video.id}/update-length`, {
                    method: 'POST',
                    headers: {
                      'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({ videoLength: durationSeconds }),
                  })
                    .then((response) => {
                      if (response.ok) {
                        console.log(
                          `Video length updated for ${video.id}: ${durationSeconds} seconds`,
                        );
                      } else {
                        console.error(
                          'Failed to update video length on server',
                        );
                      }
                    })
                    .catch((error) => {
                      console.error('Error updating video length:', error);
                    });

                  // Trigger Alpine.js to update the UI
                  window.dispatchEvent(
                    new CustomEvent('video-length-updated', { detail: video }),
                  );
                }
              } catch (e) {
                console.error('Error getting video duration:', e);
              }
            }
          },
        },
      });
    }
  });
}

// Format seconds to MM:SS
function formatTime(seconds) {
  if (!seconds) return '00:00';

  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;

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

// Map and marker variables
let map = null;
let marker = null;

// Initialize Leaflet map
function initMap(lat = 0, lng = 0) {
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
  } else {
    // Just update the view if map already exists
    map.setView([lat, lng], zoom);
  }

  // Clear existing marker
  if (marker) {
    map.removeLayer(marker);
    marker = null;
  }

  // Add marker if coordinates are valid
  if (lat !== 0 || lng !== 0) {
    marker = L.marker([lat, lng], { draggable: true }).addTo(map);

    // Update coordinates when marker is dragged
    marker.on('dragend', function (event) {
      const position = marker.getLatLng();
      window.dispatchEvent(
        new CustomEvent('marker-moved', {
          detail: { lat: position.lat, lng: position.lng },
        }),
      );
    });
  }

  // Add click handler to map
  map.on('click', function (e) {
    const { lat, lng } = e.latlng;

    // Remove existing marker if any
    if (marker) {
      map.removeLayer(marker);
    }

    // Add new marker
    marker = L.marker([lat, lng], { draggable: true }).addTo(map);

    // Update Alpine data
    window.dispatchEvent(
      new CustomEvent('marker-moved', {
        detail: { lat: lat, lng: lng },
      }),
    );

    // Add drag handler
    marker.on('dragend', function (event) {
      const position = marker.getLatLng();
      window.dispatchEvent(
        new CustomEvent('marker-moved', {
          detail: { lat: position.lat, lng: position.lng },
        }),
      );
    });
  });

  // Force map to recalculate its size
  setTimeout(() => {
    map.invalidateSize();
  }, 100);
}

// Alpine.js component for video administration
document.addEventListener('alpine:init', () => {
  Alpine.data('videoAdmin', () => ({
    videos: [],
    loading: true,
    isProcessing: false,
    currentPage: parseInt(
      new URLSearchParams(window.location.search).get('page') || 0,
    ),
    pageSize: 50,
    totalElements: 0,
    totalPages: 0,
    activeFilter:
      new URLSearchParams(window.location.search).get('filter') || 'recent',
    // Add new sorting properties
    sortField:
      new URLSearchParams(window.location.search).get('sort') || 'created_at',
    sortDirection:
      new URLSearchParams(window.location.search).get('direction') || 'DESC',
    // Map modal state
    showLocationModal: false,
    selectedVideoId: null,
    selectedLat: null,
    selectedLng: null,

    async init() {
      // Load YouTube API
      loadYouTubeAPI();

      // Listen for URL changes
      window.addEventListener('popstate', () => {
        this.currentPage = parseInt(
          new URLSearchParams(window.location.search).get('page') || 0,
        );
        this.activeFilter =
          new URLSearchParams(window.location.search).get('filter') || 'recent';
        this.sortField =
          new URLSearchParams(window.location.search).get('sort') ||
          'created_at';
        this.sortDirection =
          new URLSearchParams(window.location.search).get('direction') ||
          'DESC';
        this.loadVideos();
      });

      // Listen for video title updates from YouTube API
      window.addEventListener('video-title-updated', (e) => {
        // Force Alpine to notice the change
        this.videos = [...this.videos];
      });

      // Listen for video length updates from YouTube API
      window.addEventListener('video-length-updated', (e) => {
        // Force Alpine to notice the change
        this.videos = [...this.videos];
      });

      // Listen for marker updates from map
      window.addEventListener('marker-moved', (e) => {
        this.selectedLat = e.detail.lat;
        this.selectedLng = e.detail.lng;
      });

      console.log(
        'Alpine component initialized with page:',
        this.currentPage,
        'filter:',
        this.activeFilter,
        'sort:',
        this.sortField,
        'direction:',
        this.sortDirection,
      );
    },

    async loadVideos() {
      this.loading = true;

      try {
        // Update the URL to include sorting parameters
        const response = await fetch(
          `/admin/api/videos?page=${this.currentPage}&size=${this.pageSize}&filter=${this.activeFilter}&sort=${this.sortField}&direction=${this.sortDirection}`,
        );
        const data = await response.json();

        this.videos = data.content.map((video) => ({
          ...video,
          videoTitle: null, // Will be filled by YouTube API
          player: null, // Will hold the YouTube player instance
        }));
        this.totalElements = data.totalElements;
        this.totalPages = data.totalPages;
        this.activeFilter = data.filter;
        this.sortField = data.sortField;
        this.sortDirection = data.sortDirection;

        // Update browser history without reloading
        const url = new URL(window.location);
        url.searchParams.set('page', this.currentPage);
        url.searchParams.set('filter', this.activeFilter);
        url.searchParams.set('sort', this.sortField);
        url.searchParams.set('direction', this.sortDirection);
        window.history.pushState({}, '', url);

        // Initialize YouTube players after a short delay to let the DOM update
        setTimeout(() => {
          initYouTubePlayers(this.videos);
        }, 100);
      } catch (error) {
        console.error('Error loading videos:', error);
        // Show error message to user
        alert('Failed to load videos. Please try again.');
      } finally {
        this.loading = false;
      }
    },

    // Map-related methods
    openLocationModal(videoId) {
      if (this.isProcessing) return;

      this.selectedVideoId = videoId;
      const video = this.videos.find((v) => v.id === videoId);

      // Reset selected coordinates
      this.selectedLat = null;
      this.selectedLng = null;

      // Show the modal
      this.showLocationModal = true;

      // Initialize map after modal is shown
      setTimeout(() => {
        // Use existing coordinates if available, otherwise default to center view
        let initialLat = 0;
        let initialLng = 0;

        if (video.latitude && video.longitude) {
          initialLat = parseFloat(video.latitude);
          initialLng = parseFloat(video.longitude);
          this.selectedLat = initialLat;
          this.selectedLng = initialLng;
        }

        initMap(initialLat, initialLng);
      }, 100);
    },

    async saveCoordinates() {
      if (this.isProcessing || !this.selectedLat || !this.selectedLng) return;

      this.isProcessing = true;
      console.log(
        `Saving coordinates (${this.selectedLat}, ${this.selectedLng}) for video ${this.selectedVideoId}`,
      );

      try {
        const response = await fetch(
          `/admin/api/videos/${this.selectedVideoId}/update-coordinates`,
          {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
            },
            body: JSON.stringify({
              latitude: this.selectedLat,
              longitude: this.selectedLng,
            }),
          },
        );

        if (response.ok) {
          const result = await response.json();

          // Update the video in the local array
          const videoIndex = this.videos.findIndex(
            (v) => v.id === this.selectedVideoId,
          );
          if (videoIndex >= 0) {
            this.videos[videoIndex].latitude = this.selectedLat;
            this.videos[videoIndex].longitude = this.selectedLng;
            // Force Alpine to notice the change
            this.videos = [...this.videos];
          }

          // Close the modal
          this.showLocationModal = false;

          // Show success message
          this.showToast(
            result.message || 'Coordinates updated successfully',
            'success',
          );
        } else {
          const error = await response.json();
          throw new Error(error.message || 'Failed to update coordinates');
        }
      } catch (error) {
        console.error('Error updating coordinates:', error);
        this.showToast(
          error.message || 'Failed to update coordinates',
          'error',
        );
      } finally {
        this.isProcessing = false;
      }
    },

    // Add these new methods for sorting
    changeSort(field) {
      if (this.isProcessing || this.loading) return;

      console.log('Changing sort field to:', field);

      // If clicking the same field, toggle direction
      if (field === this.sortField) {
        this.sortDirection = this.sortDirection === 'ASC' ? 'DESC' : 'ASC';
      } else {
        // Default to DESC for date fields and ASC for other fields
        if (field.includes('_at')) {
          this.sortDirection = 'DESC'; // Default to newest first for dates
        } else {
          this.sortDirection = 'ASC'; // Default to ascending for other fields
        }
        this.sortField = field;
      }

      this.currentPage = 0; // Reset to first page when changing sort
      this.loadVideos();
    },

    toggleSortDirection() {
      if (this.isProcessing || this.loading) return;

      this.sortDirection = this.sortDirection === 'ASC' ? 'DESC' : 'ASC';
      this.loadVideos();
    },

    // Add sort field to existing methods

    changeFilter(filterName) {
      if (this.isProcessing || this.loading) return;

      console.log('Changing filter to:', filterName);
      this.activeFilter = filterName;
      this.currentPage = 0; // Reset to first page when changing filters
      this.loadVideos();
    },

    formatTime(seconds) {
      return formatTime(seconds);
    },

    formatDate(isoString) {
      return formatDate(isoString);
    },

    prevPage() {
      if (this.currentPage > 0) {
        this.currentPage--;
        console.log('Navigate to previous page:', this.currentPage);
        this.loadVideos();
      }
    },

    nextPage() {
      if (this.currentPage < this.totalPages - 1) {
        this.currentPage++;
        console.log('Navigate to next page:', this.currentPage);
        this.loadVideos();
      }
    },

    goToPage(pageNum) {
      if (pageNum >= 0 && pageNum < this.totalPages) {
        this.currentPage = pageNum;
        console.log('Go to page:', pageNum);
        this.loadVideos();
      }
    },

    generatePageNumbers() {
      const pageNumbers = [];
      const totalPageButtons = 5; // Total number of page buttons to show

      let startPage = Math.max(
        0,
        this.currentPage - Math.floor(totalPageButtons / 2),
      );
      let endPage = Math.min(
        this.totalPages - 1,
        startPage + totalPageButtons - 1,
      );

      // Adjust startPage if endPage is at maximum
      if (endPage === this.totalPages - 1) {
        startPage = Math.max(0, endPage - totalPageButtons + 1);
      }

      for (let i = startPage; i <= endPage; i++) {
        pageNumbers.push(i);
      }

      return pageNumbers;
    },

    async approveVideo(id) {
      if (this.isProcessing) return;

      this.isProcessing = true;
      console.log('Approving video with ID:', id);

      try {
        const response = await fetch(`/admin/api/videos/${id}/approve`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
        });

        if (response.ok) {
          const result = await response.json();
          console.log('Approval result:', result);

          // Update the video in the local array
          const videoIndex = this.videos.findIndex((v) => v.id === id);
          if (videoIndex >= 0) {
            this.videos[videoIndex].approveAt = new Date().toISOString();
            // Force Alpine to notice the change
            this.videos = [...this.videos];
          }

          // Show success message
          this.showToast(
            result.message || 'Video approved successfully',
            'success',
          );
        } else {
          const error = await response.json();
          throw new Error(error.message || 'Failed to approve video');
        }
      } catch (error) {
        console.error('Error approving video:', error);
        this.showToast(error.message || 'Failed to approve video', 'error');
      } finally {
        this.isProcessing = false;
      }
    },

    confirmDelete(id, title) {
      if (this.isProcessing) return;

      if (
        confirm(
          `Are you sure you want to delete "${title}"? This action cannot be undone.`,
        )
      ) {
        this.deleteVideo(id);
      }
    },

    async deleteVideo(id) {
      if (this.isProcessing) return;

      this.isProcessing = true;
      console.log('Deleting video with ID:', id);

      try {
        const response = await fetch(`/admin/api/videos/${id}`, {
          method: 'DELETE',
        });

        if (response.ok) {
          const result = await response.json();
          console.log('Delete result:', result);

          // Remove the video from the local array
          this.videos = this.videos.filter((v) => v.id !== id);

          // Show success message
          this.showToast(
            result.message || 'Video deleted successfully',
            'success',
          );

          // If we've removed all videos from the current page and it's not the first page, go to previous page
          if (this.videos.length === 0 && this.currentPage > 0) {
            this.currentPage--;
            this.loadVideos();
          }
        } else {
          const error = await response.json();
          throw new Error(error.message || 'Failed to delete video');
        }
      } catch (error) {
        console.error('Error deleting video:', error);
        this.showToast(error.message || 'Failed to delete video', 'error');
      } finally {
        this.isProcessing = false;
      }
    },

    async approveAllVideos() {
      if (this.isProcessing) return;

      // Find videos on this page that need approval
      const videosToApprove = this.videos.filter((v) => !v.approveAt);

      if (videosToApprove.length === 0) {
        this.showToast('All videos on this page are already approved', 'info');
        return;
      }

      // Confirm with the user
      const confirmMessage = `Are you sure you want to approve all ${videosToApprove.length} videos on this page?`;
      if (!confirm(confirmMessage)) return;

      this.isProcessing = true;
      this.showToast(`Approving ${videosToApprove.length} videos...`, 'info');

      let successCount = 0;
      let failCount = 0;

      // Process each video sequentially to avoid overwhelming the server
      for (const video of videosToApprove) {
        try {
          const response = await fetch(
            `/admin/api/videos/${video.id}/approve`,
            {
              method: 'POST',
              headers: {
                'Content-Type': 'application/json',
              },
            },
          );

          if (response.ok) {
            // Update the video in the local array
            const videoIndex = this.videos.findIndex((v) => v.id === video.id);
            if (videoIndex >= 0) {
              this.videos[videoIndex].approveAt = new Date().toISOString();
            }
            successCount++;
          } else {
            failCount++;
            console.error(`Failed to approve video ${video.id}`);
          }
        } catch (error) {
          failCount++;
          console.error(`Error approving video ${video.id}:`, error);
        }
      }

      // Force Alpine to notice the changes
      this.videos = [...this.videos];

      // Show final result to user
      if (failCount === 0) {
        this.showToast(
          `Successfully approved all ${successCount} videos`,
          'success',
        );
      } else {
        this.showToast(
          `Approved ${successCount} videos, ${failCount} failed`,
          'error',
        );
      }

      this.isProcessing = false;
    },

    showToast(message, type = 'info') {
      // Simple toast implementation (you can replace with a more sophisticated one)
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
