// STOMP 클라이언트 유틸리티
export interface StompHeaders {
  [key: string]: string;
}

export interface StompFrame {
  command: string;
  headers: StompHeaders;
  body: string;
}

export class StompClient {
  private ws: WebSocket | null = null;
  private subscriptions: Map<string, (frame: StompFrame) => void> = new Map();
  private subscriptionIdCounter = 0;
  private connected = false;
  private onConnectCallback?: () => void;
  private onErrorCallback?: (error: Error) => void;
  private onDisconnectCallback?: () => void;
  private url: string;

  constructor(url: string) {
    this.url = url;
  }

  connect(headers: StompHeaders = {}, onConnect?: () => void, onError?: (error: Error) => void) {
    this.onConnectCallback = onConnect;
    this.onErrorCallback = onError;

    try {
      this.ws = new WebSocket(this.url);
      
      this.ws.onopen = () => {
        // STOMP CONNECT 프레임 전송
        const connectFrame = this.buildFrame("CONNECT", headers, "");
        if (this.ws) {
          this.ws.send(connectFrame);
        }
      };

      this.ws.onmessage = (event) => {
        const frame = this.parseFrame(event.data);
        if (!frame) return;

        if (frame.command === "CONNECTED") {
          this.connected = true;
          if (this.onConnectCallback) {
            this.onConnectCallback();
          }
        } else if (frame.command === "ERROR") {
          const error = new Error(frame.body || "STOMP Error");
          if (this.onErrorCallback) {
            this.onErrorCallback(error);
          }
        } else if (frame.command === "MESSAGE") {
          const subscriptionId = frame.headers["subscription"];
          const callback = subscriptionId ? this.subscriptions.get(subscriptionId) : null;
          if (callback) {
            callback(frame);
          }
        }
      };

      this.ws.onerror = (_error) => {
        if (this.onErrorCallback) {
          this.onErrorCallback(new Error("WebSocket error"));
        }
      };

      this.ws.onclose = () => {
        this.connected = false;
        this.subscriptions.clear();
        if (this.onDisconnectCallback) {
          this.onDisconnectCallback();
        }
      };
    } catch (error) {
      if (this.onErrorCallback) {
        this.onErrorCallback(error instanceof Error ? error : new Error("Connection failed"));
      }
    }
  }

  disconnect(onDisconnect?: () => void) {
    this.onDisconnectCallback = onDisconnect;
    if (this.connected && this.ws) {
      const disconnectFrame = this.buildFrame("DISCONNECT", {}, "");
      this.ws.send(disconnectFrame);
      setTimeout(() => {
        if (this.ws) {
          this.ws.close();
          this.ws = null;
        }
        this.connected = false;
        this.subscriptions.clear();
      }, 100);
    } else if (this.ws) {
      this.ws.close();
      this.ws = null;
      this.connected = false;
      this.subscriptions.clear();
    }
  }

  subscribe(destination: string, callback: (frame: StompFrame) => void): string {
    if (!this.connected || !this.ws) {
      throw new Error("Not connected");
    }

    const subscriptionId = `sub-${++this.subscriptionIdCounter}`;
    this.subscriptions.set(subscriptionId, callback);

    const headers: StompHeaders = {
      destination,
      id: subscriptionId,
    };

    const subscribeFrame = this.buildFrame("SUBSCRIBE", headers, "");
    this.ws.send(subscribeFrame);

    return subscriptionId;
  }

  unsubscribe(subscriptionId: string) {
    if (!this.connected || !this.ws) {
      return;
    }

    const headers: StompHeaders = {
      id: subscriptionId,
    };

    const unsubscribeFrame = this.buildFrame("UNSUBSCRIBE", headers, "");
    this.ws.send(unsubscribeFrame);
    this.subscriptions.delete(subscriptionId);
  }

  send(destination: string, headers: StompHeaders = {}, body: string = "") {
    if (!this.connected || !this.ws) {
      throw new Error("Not connected");
    }

    const sendHeaders: StompHeaders = {
      destination,
      ...headers,
    };

    const sendFrame = this.buildFrame("SEND", sendHeaders, body);
    this.ws.send(sendFrame);
  }

  isConnected(): boolean {
    return this.connected && this.ws?.readyState === WebSocket.OPEN;
  }

  private buildFrame(command: string, headers: StompHeaders, body: string): string {
    let frame = `${command}\n`;
    
    for (const [key, value] of Object.entries(headers)) {
      frame += `${key}:${value}\n`;
    }
    
    frame += `\n${body}\0`;
    return frame;
  }

  private parseFrame(data: string): StompFrame | null {
    try {
      const lines = data.split("\n");
      const command = lines[0]?.trim();
      if (!command) return null;

      const headers: StompHeaders = {};
      let bodyStartIndex = 1;

      // 헤더 파싱
      for (let i = 1; i < lines.length; i++) {
        const line = lines[i];
        if (!line || line.trim() === "") {
          bodyStartIndex = i + 1;
          break;
        }
        const colonIndex = line.indexOf(":");
        if (colonIndex > 0) {
          const key = line.substring(0, colonIndex).trim();
          const value = line.substring(colonIndex + 1).trim();
          headers[key] = value;
        }
      }

      // 본문 파싱 (null 문자 제거)
      const body = lines.slice(bodyStartIndex).join("\n").replace(/\0$/, "");

      return {
        command,
        headers,
        body,
      };
    } catch (error) {
      console.error("Failed to parse STOMP frame:", error);
      return null;
    }
  }
}

