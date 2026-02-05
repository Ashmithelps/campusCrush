import { Client } from '@stomp/stompjs';

class SocketService {
  constructor() {
    this.client = null;
    this.connected = false;
    this.pendingSubscriptions = []; // Queue subscriptions if not yet connected
  }

  // Connect globally with Token
  connect(token) {
    if (this.client && this.client.active) return; // Already connected

    this.client = new Client({
      brokerURL: import.meta.env.VITE_WS_URL || `ws://${window.location.hostname}:8080/ws`,
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        this.connected = true;
        console.log('WS Connected');
        this.processPendingSubscriptions();
      },
      onStompError: (frame) => {
        console.error('WS Error', frame);
      },
      onWebSocketClose: () => {
        this.connected = false;
      }
    });

    this.client.activate();
  }

  processPendingSubscriptions() {
    while (this.pendingSubscriptions.length > 0) {
      const { topic, callback, resolve } = this.pendingSubscriptions.shift();
      const sub = this.client.subscribe(topic, (msg) => callback(msg));
      if (resolve) resolve(sub);
    }
  }

  // Subscribe to a topic. Returns a Promise that resolves to the subscription object (so you can unsubscribe)
  subscribe(topic, callback) {
    return new Promise((resolve) => {
      if (this.connected && this.client) {
        const sub = this.client.subscribe(topic, (msg) => callback(msg));
        resolve(sub);
      } else {
        // Queue it
        this.pendingSubscriptions.push({ topic, callback, resolve });
      }
    });
  }

  sendMessage(confessionId, content) {
    if (this.client && this.connected) {
      this.client.publish({
        destination: `/app/confession/${confessionId}/send`,
        body: JSON.stringify({ content }),
      });
    } else {
      console.error("Socket not connected");
    }
  }

  disconnect() {
    if (this.client) {
      this.client.deactivate();
      this.connected = false;
    }
  }
}

export default new SocketService();
