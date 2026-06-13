import { WebSocketServer, WebSocket } from 'ws';
import { AddressInfo } from 'net';
import { DebugCommand } from './test-definition';

interface PendingRequest {
  resolve: (value: unknown) => void;
  reject: (err: Error) => void;
}

/** Minimal request/response WS server used by the parity tests. Each call to
 * `send` returns a Promise that resolves when the Elm app responds with the
 * matching id. Lives only for the duration of one test; teardown closes the
 * socket.
 */
export class TestWsServer {
  private server: WebSocketServer;
  private socket: WebSocket | null = null;
  private nextId = 1;
  private pending = new Map<string, PendingRequest>();
  public readonly port: number;

  private constructor(server: WebSocketServer) {
    this.server = server;
    const addr = server.address() as AddressInfo;
    this.port = addr.port;
    server.on('connection', (ws) => {
      this.socket = ws;
      ws.on('message', (raw) => this.handleIncoming(raw.toString()));
    });
  }

  static async start(): Promise<TestWsServer> {
    return new Promise((resolve) => {
      const wss = new WebSocketServer({ port: 0, host: '127.0.0.1' });
      wss.on('listening', () => resolve(new TestWsServer(wss)));
    });
  }

  async waitForConnection(timeoutMs = 5000): Promise<void> {
    const start = Date.now();
    while (!this.socket) {
      if (Date.now() - start > timeoutMs) throw new Error('WS connection timeout');
      await new Promise((r) => setTimeout(r, 50));
    }
  }

  async send(cmd: DebugCommand): Promise<unknown> {
    if (!this.socket) throw new Error('No WS connection — did the page load with ?debug= ?');
    const id = `req-${this.nextId++}`;
    return new Promise((resolve, reject) => {
      this.pending.set(id, { resolve, reject });
      this.socket!.send(JSON.stringify({ id, cmd }));
      // Per-request timeout: most commands are sub-100ms; allow 2s for export-html.
      setTimeout(() => {
        if (this.pending.has(id)) {
          this.pending.delete(id);
          reject(new Error(`Timeout waiting for response to ${JSON.stringify(cmd)}`));
        }
      }, 2000);
    });
  }

  private handleIncoming(raw: string): void {
    let parsed: { id?: string; error?: string; result?: unknown };
    try {
      parsed = JSON.parse(raw);
    } catch (e) {
      console.error('[ws-server] invalid JSON from Elm:', raw);
      return;
    }
    const pending = this.pending.get(parsed.id ?? '');
    if (!pending) {
      console.warn('[ws-server] response with unknown id:', parsed.id);
      return;
    }
    this.pending.delete(parsed.id!);
    if (parsed.error) pending.reject(new Error(parsed.error));
    else pending.resolve(parsed.result);
  }

  async close(): Promise<void> {
    if (this.socket) this.socket.close();
    return new Promise((resolve) => this.server.close(() => resolve()));
  }
}
