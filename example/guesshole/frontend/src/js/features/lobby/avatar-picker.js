import { LogService } from '../../services/log-service.js';

export const AvatarPicker = {
  totalAvatars: 25,
  selectedAvatarId: 'avatar1',
  tempSelectedAvatarId: 'avatar1',
  showAvatarPicker: false,

  get availableAvatars() {
    return Array.from(
      { length: this.totalAvatars },
      (_, i) => `avatar${i + 1}`,
    );
  },

  openAvatarPicker() {
    this.tempSelectedAvatarId = this.selectedAvatarId;
    this.showAvatarPicker = true;
  },

  tempSelectAvatar(avatarId) {
    this.tempSelectedAvatarId = avatarId;
  },

  applyAvatarSelection() {
    this.selectedAvatarId = this.tempSelectedAvatarId;
    this.tempSelectedAvatarId = null;
    this.showAvatarPicker = false;
    LogService.add(
      `Avatar selected: ${this.selectedAvatarId} (not sent to server yet)`,
      'info',
    );
  },

  cancelAvatarSelection() {
    this.tempSelectedAvatarId = null;
    this.showAvatarPicker = false;
  },
};
