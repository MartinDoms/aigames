import { StorageService } from '../../services/storage-service.js';
import { LogService } from '../../services/log-service.js';
import { SoundEffectsService } from '../../services/sound-effects-service.js';

export const VideoPlayer = {
  volumeLevel: 75,
  isMuted: false,
  initialized: false,
  ytPlayer: null,
  currentVideoId: null,

  initializeVideoState() {
    // Only initialize once
    if (!this.initialized) {
      // Load volume settings from global storage
      this.loadVolumeSettings();
      this.initialized = true;
      LogService.add('Video state initialized with saved settings', 'info');
    }
  },

  // In your VideoPlayer component
  setupYoutubeEmbed() {
    LogService.add('Setting up YouTube embed for game', 'info');

    // Initialize video state with saved volume settings
    this.initializeVideoState();

    // Load YouTube iframe API for better control
    if (!window.YT) {
      const tag = document.createElement('script');
      tag.src = 'https://www.youtube.com/iframe_api';
      const firstScriptTag = document.getElementsByTagName('script')[0];
      firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

      LogService.add('Done preparing Youtube iframe API', 'info');
    }

    // API will be available but no video will be loaded yet
    window.onYouTubeIframeAPIReady = () => {
      LogService.add('YouTube iframe API ready', 'info');
      // Don't create the player yet
      this.ytApiReady = true;
    };
  },

  createAndPlayVideo(videoId, startTimeSeconds = 0) {
    if (!videoId) {
      LogService.add('No YouTube video ID provided', 'error');
      return false;
    }

    this.currentVideoId = videoId;
    LogService.add(`Creating YouTube player for video: ${videoId}`, 'info');

    // Check if API is ready
    if (!window.YT || !window.YT.Player) {
      LogService.add('YouTube API not ready yet', 'warning');

      // Set up a timeout to retry
      setTimeout(() => {
        this.createAndPlayVideo(videoId, startTimeSeconds);
      }, 1000);

      return false;
    }

    // Get container
    const container = this.$refs.youtubeContainer;
    if (!container) {
      LogService.add('YouTube container not found', 'error');
      return false;
    }

    try {
      // Clear any existing content
      container.innerHTML = '';

      // Create new player
      this.ytPlayer = new YT.Player(container, {
        videoId: videoId,
        playerVars: {
          autoplay: 1,
          mute: this.isMuted ? 1 : 0,
          controls: 0,
          showinfo: 0,
          rel: 0,
          iv_load_policy: 3,
          modestbranding: 1,
          enablejsapi: 1,
          disablekb: 1,
          fs: 0,
          playsinline: 1,
          start: startTimeSeconds,
        },
        events: {
          onReady: (event) => {
            LogService.add('YouTube player ready', 'info');

            // Apply volume settings when player is ready
            if (this.isMuted) {
              event.target.mute();
            } else {
              event.target.unMute();
              event.target.setVolume(this.volumeLevel);
            }

            // Start playing
            event.target.playVideo();
          },
          onStateChange: (event) => {
            if (event.data === YT.PlayerState.PLAYING) {
              LogService.add('YouTube video started playing', 'info');
            }
          },
          onError: (event) => {
            LogService.add(`YouTube error: ${event.data}`, 'error');
          },
        },
      });

      // Store reference to iframe for future use
      this.$refs.youtubeFrame = container.querySelector('iframe');

      return true;
    } catch (error) {
      LogService.add(`Error creating YouTube player: ${error}`, 'error');
      return false;
    }
  },

  initYouTubePlayer() {
    try {
      // Get iframe element
      const iframe = this.$refs.youtubeFrame;
      if (!iframe) {
        LogService.add('YouTube iframe not found', 'error');
        return;
      }

      // Create YouTube player instance
      this.ytPlayer = new YT.Player(iframe, {
        events: {
          onReady: (event) => {
            LogService.add('YouTube player ready', 'info');

            // Apply volume settings when player is ready
            if (this.isMuted) {
              event.target.mute();
            } else {
              event.target.unMute();
              event.target.setVolume(this.volumeLevel);
            }

            // Use a slight delay to ensure browser is ready for playback
            setTimeout(() => {
              try {
                LogService.add('Attempting to play video', 'info');
                event.target.playVideo();
              } catch (e) {
                LogService.add(`Failed to auto-play: ${e}`, 'warning');

                // Try an alternative approach
                const iframe = this.$refs.youtubeFrame;
                if (iframe) {
                  // Sometimes refreshing the source can trigger autoplay
                  const currentSrc = iframe.src;
                  iframe.src = currentSrc;
                  LogService.add(
                    'Attempted iframe refresh to trigger autoplay',
                    'info',
                  );
                }
              }
            }, 500);
          },
          onStateChange: (event) => {
            // Check if video has started playing
            if (event.data === YT.PlayerState.PLAYING) {
              LogService.add('YouTube video started playing', 'info');
            }
          },
          onError: (event) => {
            LogService.add(`YouTube error: ${event.data}`, 'error');
          },
        },
      });
    } catch (error) {
      LogService.add(`Error initializing YouTube player: ${error}`, 'error');
    }
  },

  loadVolumeSettings() {
    const savedVolume = StorageService.getItem('app_volumeLevel');
    const savedMute = StorageService.getItem('app_isMuted');

    if (savedVolume !== null) {
      this.volumeLevel = parseInt(savedVolume, 10);
    }

    if (savedMute !== null) {
      this.isMuted = savedMute === 'true';
    }

    LogService.add(
      `Loaded app-wide volume settings: ${this.volumeLevel}%, muted: ${this.isMuted}`,
      'info',
    );
  },

  toggleMute() {
    this.isMuted = !this.isMuted;

    // Also mute/unmute sound effects
    SoundEffectsService.setMute(this.isMuted);

    if (this.ytPlayer) {
      if (this.isMuted) {
        this.ytPlayer.mute();
        LogService.add('Video muted', 'info');
      } else {
        this.ytPlayer.unMute();
        // Apply the stored volume level
        this.ytPlayer.setVolume(this.volumeLevel);
        LogService.add(`Video unmuted (volume: ${this.volumeLevel}%)`, 'info');
      }
    } else {
      LogService.add(`No ytPlayer defined`, 'error');
    }

    // Save mute state to local storage
    this.saveVolumeSettings();
  },

  // Method to set volume level
  setVolume(value) {
    // Convert to integer
    const volumeLevel = parseInt(value, 10);
    this.volumeLevel = volumeLevel;

    if (this.ytPlayer) {
      // Apply volume to YouTube player
      this.ytPlayer.setVolume(volumeLevel);

      // If volume is set to 0, also mute
      if (volumeLevel === 0) {
        this.isMuted = true;
        this.ytPlayer.mute();
      } else if (this.isMuted) {
        // If volume is increased while muted, unmute
        this.isMuted = false;
        this.ytPlayer.unMute();
      }

      LogService.add(`Volume set to ${volumeLevel}%`, 'info');
    }

    // Save volume settings to local storage
    this.saveVolumeSettings();
  },

  saveVolumeSettings() {
    StorageService.setItem('app_volumeLevel', this.volumeLevel);
    StorageService.setItem('app_isMuted', this.isMuted);
  },

  /**
   * Play a YouTube video with the specified video ID and optional start time
   * @param {string} youtubeVideoId - The YouTube video ID to play
   * @param {number} startTimeSeconds - Optional start time in seconds (defaults to 0)
   * @returns {boolean} - Success status
   */
  playVideo(youtubeVideoId, startTimeSeconds = 0) {
    LogService.add(
      `Attempting to play YouTube video: ${youtubeVideoId} from ${startTimeSeconds}s`,
      'info',
    );

    if (!youtubeVideoId) {
      LogService.add('No YouTube video ID provided', 'error');
      return false;
    }

    // If player exists, use it to load the video
    if (this.ytPlayer) {
      return this._loadAndPlayVideo(youtubeVideoId, startTimeSeconds);
    } else {
      // Otherwise create a new player with this video
      return this.createAndPlayVideo(youtubeVideoId, startTimeSeconds);
    }
  },

  /**
   * Internal method to load and play a video with the initialized player
   * @private
   */
  _loadAndPlayVideo(youtubeVideoId, startTimeSeconds) {
    try {
      if (!this.ytPlayer && this.$refs.youtubeFrame) {
        // If player isn't initialized yet, update the iframe directly
        const startTimeParam =
          startTimeSeconds > 0 ? `&start=${startTimeSeconds}` : '';
        this.$refs.youtubeFrame.src = `https://www.youtube.com/embed/${youtubeVideoId}?autoplay=1&mute=${this.isMuted ? 1 : 0}&controls=0&showinfo=0&rel=0&iv_load_policy=3&modestbranding=1&enablejsapi=1&disablekb=1&fs=0&playsinline=1${startTimeParam}`;
        return true;
      }

      // Existing code for when ytPlayer is available...
      this.ytPlayer.loadVideoById({
        videoId: youtubeVideoId,
        startSeconds: startTimeSeconds,
      });

      // Rest of your existing method...
    } catch (error) {
      LogService.add(`Error playing video: ${error}`, 'error');
      return false;
    }
  },

  /**
   * Stop the currently playing video
   * @returns {boolean} - Success status
   */
  stopPlaying() {
    LogService.add('Attempting to stop video playback', 'info');

    if (!this.ytPlayer) {
      LogService.add(
        'Cannot stop video: YouTube player not initialized',
        'warning',
      );
      return false;
    }

    try {
      // Stop the video using YouTube API
      this.ytPlayer.stopVideo();
      LogService.add('Video playback stopped', 'info');
      return true;
    } catch (error) {
      LogService.add(`Error stopping video: ${error}`, 'error');
      return false;
    }
  },
};
