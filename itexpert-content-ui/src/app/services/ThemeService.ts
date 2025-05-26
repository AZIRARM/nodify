import {Injectable} from "@angular/core";
import {ParametersService} from "./ParametersService";


@Injectable()
export class ThemeService {


  constructor(parameters: ParametersService) {
  }

  setTheme() {
    const themeLink = document.querySelector('#theme-link');
    const toggleBtn = document.querySelector("#toggle-btn");

    if (themeLink) {
      themeLink.remove();
    }

    const linkElement = document.createElement('link');
    linkElement.id = 'theme-link';
    linkElement.rel = 'stylesheet';
    linkElement.type = 'text/css';
    linkElement.media = 'all';

    let defaultTheme: string | null = window.localStorage.getItem("theme");

    if (defaultTheme && defaultTheme === "light") {
      linkElement.href = 'assets/themes/indigo-pink.css';
    } else if (defaultTheme && defaultTheme === "dark") {
      linkElement.href = 'assets/themes/pink-bluegrey.css';
    } else {
       linkElement.href = 'assets/themes/indigo-pink.css';
    }

    document.head.appendChild(linkElement);
  }

  toggleTheme(theme: string) {
    window.localStorage.setItem("theme", theme);

    this.setTheme();
  }

  isDarkModeEnabled(): boolean {
    let defaultTheme: string | null = window.localStorage.getItem("theme");
    if (defaultTheme && defaultTheme === "dark") {
      return true;
    }
    return false;
  }
}
