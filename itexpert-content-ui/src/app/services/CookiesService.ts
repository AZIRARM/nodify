import {Injectable} from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class CookiesService {

  // ----- SET -----
  setCookie(name: string, value: string | number, days: number): void {
    let expires = "";
    if (days) {
      const date = new Date();
      date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));
      expires = "; expires=" + date.toUTCString();
    }
    document.cookie = `${name}=${encodeURIComponent(value)}${expires}; path=/`;
  }

  // ----- GET -----
  getCookie(name: string): string | null {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) {
      return decodeURIComponent(parts.pop()!.split(";").shift()!);
    }
    return null;
  }

  // ----- ERASE -----
  eraseCookie(name: string): void {
    document.cookie = `${name}=; Max-Age=-99999999; path=/`;
  }
}

