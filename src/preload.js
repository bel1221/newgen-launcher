// The only bridge between the UI (renderer) and the main process.
const { contextBridge, ipcRenderer } = require('electron');

const EVENTS = ['status', 'progress', 'log', 'game-closed'];

contextBridge.exposeInMainWorld('api', {
  getInfo: () => ipcRenderer.invoke('get-info'),
  login: (username, password) => ipcRenderer.invoke('login', { username, password }),
  logout: () => ipcRenderer.invoke('logout'),
  play: () => ipcRenderer.invoke('play'),
  minimize: () => ipcRenderer.invoke('minimize'),
  close: () => ipcRenderer.invoke('close'),
  openLink: (name) => ipcRenderer.invoke('open-link', name), // a name from config.json "links", never an address
  on: (event, callback) => {
    if (EVENTS.includes(event)) ipcRenderer.on(event, (_e, data) => callback(data));
  },
});
