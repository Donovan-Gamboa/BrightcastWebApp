import {Injectable} from '@angular/core';
import {RxStomp} from '@stomp/rx-stomp';

@Injectable({
  providedIn: 'root',
})
export class GameSocketService extends RxStomp {
  constructor() {
    super();
  }

  public activateGameSocket() {
    const proto = window.location.protocol === 'https:' ? 'wss' : 'ws';
    const host = window.location.host;
    this.configure({
      brokerURL: `${proto}://${host}/brightcast-websocket`,
      reconnectDelay: 200,
    });
    this.activate();
  }
}
