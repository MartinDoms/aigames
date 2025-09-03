export const Notification = {
  visible: false,
  message: '',
  type: 'success',
  timeout: null,

  showNotification(message, type = 'success', showForMs = 2300) {
    // Clear any existing timeout
    if (this.timeout) {
      clearTimeout(this.timeout);
    }

    // Update notification data
    this.message = message;
    this.type = type;
    this.visible = true;

    // Hide notification after delay
    this.timeout = setTimeout(() => {
      this.visible = false;
    }, showForMs);
  },
};
