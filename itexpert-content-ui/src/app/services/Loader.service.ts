import { Injectable } from '@angular/core';
import { BehaviorSubject, async } from 'rxjs';

@Injectable()
export class LoaderService {
  private _loading = new BehaviorSubject<boolean>(false);
  public readonly loading$ = this._loading.asObservable();

  show() {
    this._loading.next(true);
  }

  hide() {
    this._loading.next(false);
  }
}
