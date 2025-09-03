import '../css/input.css';

import Alpine from 'alpinejs';

// YouTube API setup
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

// Initialize YouTube player for the selected template
function initYouTubePlayer(template) {
  if (!youtubeAPILoaded) {
    youtubeAPICallbacks.push(() => initYouTubePlayer(template));
    return;
  }

  console.log(template.youtubeVideoId);
  if (document.getElementById(`ytplayer`)) {
    document.getElementById(`ytplayer`).innerHTML = '';
    return new YT.Player(document.getElementById(`ytplayer`), {
      height: '100%',
      width: '100%',
      videoId: template.youtubeVideoId,
      playerVars: {
        start: template.startTime || 0,
        controls: 1,
        modestbranding: 1,
        rel: 0,
      },
      events: {
        onReady: (event) => {
          // Pause video initially to prevent autoplay
          event.target.pauseVideo();
        },
      },
    });
  }
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

// Alpine.js component for the round template map
document.addEventListener('alpine:init', () => {
  Alpine.data('roundTemplateMap', () => ({
    map: null,
    templates: [],
    markers: [],
    markerCount: 0,
    loading: true,
    selectedTemplate: null,
    selectedMarker: null,
    youtubePlayer: null,
    sources: [],
    selectedSource: 'all',
    selectedStatus: 'all',
    markerClusterGroup: null,
    clusteringOptions: {
      enabled: true,
      radius: 80,
      maxZoom: 16,
      spiderfyOnMaxZoom: true,
    },

    async init() {
      // Load YouTube API
      loadYouTubeAPI();
    },

    async initMap() {
      // Initialize the Leaflet map
      this.map = L.map('map', {
        zoomControl: false, // We'll add it in a different position
        attributionControl: false, // We'll add it back for better placement
        minZoom: 2,
        worldCopyJump: true, // Better handling of world wrap
      }).setView([20, 0], 2);

      // Add the base tile layer
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution:
          '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      }).addTo(this.map);

      // Add controls in better positions
      L.control
        .zoom({
          position: 'bottomright',
        })
        .addTo(this.map);

      L.control
        .attribution({
          position: 'bottomleft',
        })
        .addTo(this.map);

      // Add fullscreen control if browser supports it
      if (document.fullscreenEnabled || document.webkitFullscreenEnabled) {
        // Create custom fullscreen button
        const fullscreenControl = L.Control.extend({
          options: {
            position: 'bottomright',
          },
          onAdd: (map) => {
            const container = L.DomUtil.create(
              'div',
              'leaflet-bar leaflet-control',
            );
            const button = L.DomUtil.create('a', '', container);
            button.innerHTML = '⛶';
            button.href = '#';
            button.title = 'Toggle Fullscreen';
            button.style.fontWeight = 'bold';
            button.style.fontSize = '16px';

            L.DomEvent.on(button, 'click', (e) => {
              L.DomEvent.stop(e);
              this.toggleFullscreen();
            });

            return container;
          },
        });

        this.map.addControl(new fullscreenControl());
      }

      // Make sure MarkerCluster library is loaded
      if (typeof L.markerClusterGroup !== 'function') {
        console.error(
          'MarkerCluster plugin not loaded! Make sure the script is included in your HTML.',
        );
        // Fallback to not using clustering
        this.markerClusterGroup = {
          clearLayers: () => {},
          addLayer: (marker) => marker.addTo(this.map),
          getLayers: () => this.markers,
        };
      } else {
        // Initialize marker cluster group
        this.initializeMarkerCluster();
      }

      // Load templates
      await this.loadTemplates();

      // Map event listeners
      this.map.on('popupclose', (e) => {
        // Clear selected template when popup closed
        if (
          this.selectedTemplate &&
          e.popup._source &&
          e.popup._source.options.templateId === this.selectedTemplate.id
        ) {
          this.selectedTemplate = null;
          if (this.youtubePlayer) {
            this.youtubePlayer.pauseVideo();
            this.youtubePlayer = null;
          }
        }
      });
    },

    toggleFullscreen() {
      const mapContainer = document.getElementById('map');

      if (!document.fullscreenElement && !document.webkitFullscreenElement) {
        if (mapContainer.requestFullscreen) {
          mapContainer.requestFullscreen();
        } else if (mapContainer.webkitRequestFullscreen) {
          mapContainer.webkitRequestFullscreen();
        }
      } else {
        if (document.exitFullscreen) {
          document.exitFullscreen();
        } else if (document.webkitExitFullscreen) {
          document.webkitExitFullscreen();
        }
      }
    },

    initializeMarkerCluster() {
      // Check if MarkerCluster is available
      if (typeof L.markerClusterGroup !== 'function') {
        console.error('MarkerCluster plugin not loaded!');
        return;
      }

      // Remove existing cluster group if it exists
      if (this.markerClusterGroup) {
        this.map.removeLayer(this.markerClusterGroup);
      }

      // Initialize new marker cluster group with options
      this.markerClusterGroup = L.markerClusterGroup({
        disableClusteringAtZoom: this.clusteringOptions.enabled
          ? this.clusteringOptions.maxZoom
          : 0,
        maxClusterRadius: this.clusteringOptions.radius,
        spiderfyOnMaxZoom: this.clusteringOptions.spiderfyOnMaxZoom,
        showCoverageOnHover: true,
        zoomToBoundsOnClick: true,
        iconCreateFunction: (cluster) => {
          const count = cluster.getChildCount();
          let size, className;

          if (count < 10) {
            size = 'small';
            className = 'bg-secondary-light';
          } else if (count < 50) {
            size = 'medium';
            className = 'bg-secondary';
          } else {
            size = 'large';
            className = 'bg-primary';
          }

          return L.divIcon({
            html: `<div class="flex items-center justify-center rounded-full ${className} w-full h-full">
                                <span class="text-white font-bold">${count}</span>
                              </div>`,
            className: `marker-cluster marker-cluster-${size}`,
            iconSize: [40, 40],
          });
        },
      });

      // Add the cluster group to the map
      this.map.addLayer(this.markerClusterGroup);
    },

    async loadTemplates() {
      this.loading = true;

      try {
        const response = await fetch('/admin/api/round-templates/all');
        const data = await response.json();

        this.templates = data;
        this.markerCount = data.length;

        // Extract unique sources for filtering
        this.sources = [
          ...new Set(data.map((template) => template.source)),
        ].sort();

        // Add markers to the map
        this.addMarkers();
      } catch (error) {
        console.error('Error loading templates:', error);
        alert('Failed to load round templates. Please try again.');
      } finally {
        this.loading = false;
      }
    },

    addMarkers() {
      // Clear existing markers
      if (this.markerClusterGroup) {
        if (typeof this.markerClusterGroup.clearLayers === 'function') {
          this.markerClusterGroup.clearLayers();
        } else {
          // Fallback if markerClusterGroup isn't properly initialized
          this.markers.forEach((marker) => marker.remove());
        }
      }
      this.markers = [];

      // Organize templates by location to handle duplicates
      const locationMap = new Map();

      this.templates.forEach((template) => {
        const key = `${template.latitude}_${template.longitude}`;
        if (!locationMap.has(key)) {
          locationMap.set(key, []);
        }
        locationMap.get(key).push(template);
      });

      // Create markers
      locationMap.forEach((templates, key) => {
        const firstTemplate = templates[0];

        // Skip if doesn't match current filters
        const sourceMatch =
          this.selectedSource === 'all' ||
          firstTemplate.source === this.selectedSource;
        const statusMatch =
          this.selectedStatus === 'all' ||
          (this.selectedStatus === 'approved' && firstTemplate.approveAt) ||
          (this.selectedStatus === 'pending' && !firstTemplate.approveAt);

        if (!sourceMatch || !statusMatch) {
          return;
        }

        // Determine marker color based on approval status
        const markerColor = firstTemplate.approveAt ? 'green' : 'orange';

        // Create marker icon
        const markerIcon = L.divIcon({
          className: 'custom-marker',
          html: `<div class="w-6 h-6 rounded-full bg-${markerColor}-500 border-2 border-white shadow-md flex items-center justify-center">
                            ${templates.length > 1 ? `<span class="text-white text-xs font-bold">${templates.length}</span>` : ''}
                          </div>`,
          iconSize: [24, 24],
          iconAnchor: [12, 12],
        });

        // Create marker
        const marker = L.marker(
          [firstTemplate.latitude, firstTemplate.longitude],
          {
            icon: markerIcon,
            templateId: firstTemplate.id,
            templates: templates,
            source: firstTemplate.source,
            approved: firstTemplate.approveAt != null,
          },
        );

        // Add popup
        /*const popupContent = `
                    <div class="p-2">
                        <h3 class="font-medium text-primary">${templates.length} template${templates.length > 1 ? 's' : ''} at this location</h3>
                        <p class="text-sm text-primary-dark">Source: ${firstTemplate.source}</p>
                        <button class="mt-2 px-3 py-1 bg-secondary text-primary-dark text-sm rounded view-details-btn"
                                data-id="${firstTemplate.id}">
                            View Details
                        </button>
                    </div>
                `;*/
        marker.on('click', () => {
          this.showTemplateDetails(firstTemplate);
        });
        //marker.bindPopup(popupContent);

        // Store marker reference and add to cluster group
        this.markers.push(marker);

        // Add to cluster group if available, otherwise directly to map
        if (
          this.markerClusterGroup &&
          typeof this.markerClusterGroup.addLayer === 'function'
        ) {
          this.markerClusterGroup.addLayer(marker);
        } else {
          marker.addTo(this.map);
        }
      });

      // Handle popup button clicks (only add once)
      if (!this._popupClickListenerAdded) {
        document.addEventListener('click', (e) => {
          if (e.target.classList.contains('view-details-btn')) {
            const templateId = e.target.getAttribute('data-id');
            const template = this.templates.find((t) => t.id === templateId);
            if (template) {
              this.showTemplateDetails(template);
            }
          }
        });
        this._popupClickListenerAdded = true;
      }

      // Update marker count
      this.updateMarkerCount();
    },

    showTemplateDetails(template) {
      this.selectedTemplate = template;

      // Initialize YouTube player after a short delay
      setTimeout(() => {
        this.youtubePlayer = null;
        this.youtubePlayer = initYouTubePlayer(template);
      }, 500);
    },

    filterMarkers() {
      // Clear all existing markers from the cluster
      if (
        this.markerClusterGroup &&
        typeof this.markerClusterGroup.clearLayers === 'function'
      ) {
        this.markerClusterGroup.clearLayers();
      } else {
        // Fallback
        this.markers.forEach((marker) => marker.remove());
      }

      // Add back markers that match the filters
      this.markers.forEach((marker) => {
        // Check if marker matches selected filters
        const sourceMatch =
          this.selectedSource === 'all' ||
          marker.options.source === this.selectedSource;
        const statusMatch =
          this.selectedStatus === 'all' ||
          (this.selectedStatus === 'approved' && marker.options.approved) ||
          (this.selectedStatus === 'pending' && !marker.options.approved);

        // Add only matching markers to the cluster
        if (sourceMatch && statusMatch) {
          if (
            this.markerClusterGroup &&
            typeof this.markerClusterGroup.addLayer === 'function'
          ) {
            this.markerClusterGroup.addLayer(marker);
          } else {
            marker.addTo(this.map);
          }
        }
      });

      // Update marker count
      this.updateMarkerCount();
    },

    updateClustering() {
      // We need to reinitialize the cluster group with new options
      this.initializeMarkerCluster();

      // Re-add all markers that match the current filters
      this.filterMarkers();
    },

    updateMarkerCount() {
      // Count templates in visible markers
      let count = 0;
      this.markerClusterGroup.getLayers().forEach((marker) => {
        count += marker.options.templates.length;
      });
      this.markerCount = count;
    },

    resetFilters() {
      this.selectedSource = 'all';
      this.selectedStatus = 'all';
      this.clusteringOptions.enabled = true;
      this.clusteringOptions.radius = 80;
      this.updateClustering();
      this.filterMarkers();
    },

    async refreshTemplates() {
      // Clear selection
      this.selectedTemplate = null;
      if (this.youtubePlayer) {
        this.youtubePlayer.pauseVideo();
        this.youtubePlayer = null;
      }

      // Reload templates
      await this.loadTemplates();
    },

    formatTime,
    formatDate,
  }));
});

// Initialize Alpine.js
Alpine.start();
