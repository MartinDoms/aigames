// Storage service for managing local storage operations
export const StorageService = {
  getItem(key, lobbyIdentifier) {
    return localStorage.getItem(`${key}_${lobbyIdentifier}`);
  },

  setItem(key, value, lobbyIdentifier) {
    localStorage.setItem(`${key}_${lobbyIdentifier}`, value);
  },
};
