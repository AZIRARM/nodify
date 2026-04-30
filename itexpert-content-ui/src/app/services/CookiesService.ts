import { Injectable } from '@angular/core';

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
  eraseAllCookies(): void {
    const cookies = document.cookie.split(";");

    for (let i = 0; i < cookies.length; i++) {
      const cookie = cookies[i];
      const eqPos = cookie.indexOf("=");
      const name = eqPos > -1 ? cookie.substring(0, eqPos) : cookie;

      document.cookie = name + "=;expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/";
      document.cookie = name + "=;expires=Thu, 01 Jan 1970 00:00:00 UTC; path=";
      document.cookie = name + "=;expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/v0";
      document.cookie = name + "=;expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/authentication";
    }

    console.log("All cookies erased");
  }
}

