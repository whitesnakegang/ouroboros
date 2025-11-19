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

  connect(
    headers: StompHeaders = {},
    onConnect?: () => void,
    onError?: (error: Error) => void
  ) {
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
        // 바이너리 데이터인 경우 처리
        let data: string;
        if (event.data instanceof ArrayBuffer) {
          const decoder = new TextDecoder();
          data = decoder.decode(event.data);
        } else if (event.data instanceof Blob) {
          // Blob은 비동기로 처리
          event.data
            .text()
            .then((text: string) => {
              const frame = this.parseFrame(text);
              if (frame) {
                this.handleFrame(frame);
              }
            })
            .catch((error) => {
              if (this.onErrorCallback) {
                this.onErrorCallback(
                  error instanceof Error
                    ? error
                    : new Error("Failed to process Blob data")
                );
              }
            });
          return;
        } else {
          data = event.data;
        }

        const frame = this.parseFrame(data);
        if (frame) {
          this.handleFrame(frame);
        }
      };

      this.ws.onerror = () => {
        if (this.onErrorCallback) {
          this.onErrorCallback(new Error("WebSocket error"));
        }
      };

      this.ws.onclose = () => {
        this.connected = false;
        this.subscriptions.clear();
        if (this.onDisconnectCallback) {
          this.onDisconnectCallback();
          this.onDisconnectCallback = undefined;
        }
      };
    } catch (error) {
      if (this.onErrorCallback) {
        this.onErrorCallback(
          error instanceof Error ? error : new Error("Connection failed")
        );
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
        // onclose 이벤트가 발생하지 않을 수 있으므로 여기서도 콜백 호출
        if (this.onDisconnectCallback) {
          this.onDisconnectCallback();
          this.onDisconnectCallback = undefined;
        }
      }, 100);
    } else if (this.ws) {
      this.ws.close();
      this.ws = null;
      this.connected = false;
      this.subscriptions.clear();
      // 즉시 콜백 호출
      if (this.onDisconnectCallback) {
        this.onDisconnectCallback();
        this.onDisconnectCallback = undefined;
      }
    } else {
      // WebSocket이 없는 경우에도 콜백 호출
      this.connected = false;
      this.subscriptions.clear();
      if (this.onDisconnectCallback) {
        this.onDisconnectCallback();
        this.onDisconnectCallback = undefined;
      }
    }
  }

  subscribe(
    destination: string,
    callback: (frame: StompFrame) => void
  ): string {
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

  private handleFrame(frame: StompFrame) {
    if (frame.command === "CONNECTED") {
      this.connected = true;
      if (this.onConnectCallback) {
        this.onConnectCallback();
      }
    } else if (frame.command === "ERROR") {
      const errorMessage =
        frame.body || frame.headers["message"] || "STOMP Error";
      const isSessionClosed = errorMessage
        .toLowerCase()
        .includes("session closed");

      // ERROR 발생 시 연결 해제
      const wasConnected = this.connected;
      this.connected = false;
      this.subscriptions.clear();

      // WebSocket 닫기
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.ws.close();
      }

      // 연결되어 있었던 경우 disconnect 콜백도 호출
      if (wasConnected && this.onDisconnectCallback) {
        this.onDisconnectCallback();
        this.onDisconnectCallback = undefined;
      }

      // "Session closed"는 정상적인 disconnect 응답이므로 Error 콜백 호출하지 않음
      // 실제 에러인 경우에만 Error 콜백 호출
      if (!isSessionClosed && this.onErrorCallback) {
        const error = new Error(errorMessage);
        this.onErrorCallback(error);
      }
    } else if (frame.command === "MESSAGE") {
      const subscriptionId = frame.headers["subscription"];
      const callback = subscriptionId
        ? this.subscriptions.get(subscriptionId)
        : null;
      if (callback) {
        callback(frame);
      }
    }
  }

  private buildFrame(
    command: string,
    headers: StompHeaders,
    body: string
  ): string {
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
    } catch {
      // Failed to parse STOMP frame
      return null;
    }
  }
}

/**
 * 백엔드 서버 주소를 기반으로 WebSocket URL을 생성합니다.
 * - https:// 환경에서는 wss:// 사용 (Mixed Content 정책 준수)
 * - http:// 환경에서는 ws:// 사용
 *
 * @param path WebSocket 경로 (예: "/ws")
 * @returns 완전한 WebSocket URL (예: "ws://localhost:8080/ws")
 */
export function buildWebSocketUrl(
  path: string,
  protocol?: "ws" | "wss"
): string {
  // 백엔드 서버 주소 가져오기 (환경 변수 또는 기본값)
  const backendUrl =
    import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

  // URL 파싱하여 호스트와 포트 추출
  const backendUrlObj = new URL(backendUrl);

  // protocol 파라미터가 제공되면 사용, 없으면 백엔드 URL 기반으로 결정
  const wsProtocol = protocol
    ? `${protocol}:`
    : backendUrlObj.protocol === "https:"
    ? "wss:"
    : "ws:";
  const host = backendUrlObj.host; // hostname:port 형태

  const wsPath = path.startsWith("/") ? path : `/${path}`;
  return `${wsProtocol}//${host}${wsPath}`;
}
