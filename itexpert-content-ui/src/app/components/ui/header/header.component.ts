import {
  Component,
  OnInit,
  OnDestroy,
  inject,
  signal,
  effect
} from '@angular/core';

import { Router } from '@angular/router';
import { TranslateService } from "@ngx-translate/core";
import { MatDialog, MatDialogRef } from "@angular/material/dialog";

import { SidenavService } from "../../../services/SidenavService";
import { ValidationDialogComponent } from "../../commons/validation-dialog/validation-dialog.component";
import { NotificationService } from "../../../services/NotificationService";
import { ThemeService } from "../../../services/ThemeService";
import { AuthenticationService } from "../../../services/AuthenticationService";
import { UserAccessService } from "../../../services/UserAccessService";
import { CookiesService } from 'src/app/services/CookiesService';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css'],
  standalone: false
})
export class HeaderComponent implements OnInit, OnDestroy {

  private translate = inject(TranslateService);
  private notificationService = inject(NotificationService);
  private router = inject(Router);
  private dialog = inject(MatDialog);
  private sidenavService = inject(SidenavService);
  private themeService = inject(ThemeService);
  private authenticationService = inject(AuthenticationService);
  private cookiesService = inject(CookiesService);
  private userAccessService = inject(UserAccessService);

  user = signal<any>(null);

  theme = signal('dark');

  nbNotifications = signal(0);

  selectedLanguage = signal(
    window.localStorage.getItem("defaultLanguage") || 'fr'
  );

  isDarkMode = signal(this.themeService.isDarkModeEnabled());

  isToolbarVisible = signal(false);

  showHint = signal(true);

  activeLink = signal('workspace');

  languageList = [
    { code: 'en', label: 'English' },
    { code: 'fr', label: 'French' },
    { code: 'es', label: 'Español' },
    { code: 'de', label: 'Deutsch' },
    { code: 'pt', label: 'Português' },
    { code: 'ar', label: 'عربي' },
  ];

  private hideTimer: any;
  dialogRef!: MatDialogRef<ValidationDialogComponent>;

  ngOnInit() {

    document.addEventListener('mousemove', (e) => {
      if (e.clientY < 10 && !this.isToolbarVisible()) {
        this.showToolbar();
      }
    });

    this.themeService.setTheme();
    this.initCountUreadedNotifications();
  }

  ngOnDestroy() {
    if (this.hideTimer) {
      clearTimeout(this.hideTimer);
    }
  }

  isAdmin(): boolean {
    return this.userAccessService.isAdmin();
  }

  toggleSidenav(): void {
    this.sidenavService.toggle();
  }

  logout() {

    this.translate.get("LOGOUT_TITLE").subscribe(trad1 => {

      this.translate.get("LOGOUT_MESSAGE").subscribe(trad2 => {

        this.dialogRef = this.dialog.open(ValidationDialogComponent, {
          data: {
            title: trad1,
            message: trad2,
          },
          height: '80vh',
          width: '80vw',
          disableClose: true
        });

        this.dialogRef.afterClosed().subscribe(result => {

          if (result?.data === 'validated') {
            this.cookiesService.eraseCookie("userToken");
            this.router.navigate(["/login"]);
            window.location.reload();
          }

        });

      });

    });

  }

  initCountUreadedNotifications() {

    if (!this.authenticationService.isAuthenticated()) return;

    this.notificationService
      .connectWebSocket(this.authenticationService.getAccessToken())
      .subscribe({

        next: (data: any) => {
          this.nbNotifications.set(data.count || 0);
        },

        error: (err: any) => console.error("Erreur WebSocket:", err),

        complete: () => console.log("Socket fermé")

      });

  }

  changeSiteLanguage(localeCode: string): void {
    this.translate.use(localeCode);
    window.localStorage.setItem("defaultLanguage", localeCode);
    window.location.reload();
  }

  toggleTheme() {
    const newTheme = !this.isDarkMode();
    this.isDarkMode.set(newTheme);
    this.themeService.toggleTheme(newTheme ? 'dark' : 'light');
  }

  setActiveLink(link: string) {
    this.activeLink.set(link);
  }

  getCurrentLanguageLabel(): string {
    const current = this.languageList
      .find(lang => lang.code === this.selectedLanguage());

    return current ? current.label : this.selectedLanguage();
  }

  showToolbar() {
    this.cancelHideTimer();
    this.isToolbarVisible.set(true);
  }

  startHideTimer() {
    this.cancelHideTimer();

    this.hideTimer = setTimeout(() => {
      this.isToolbarVisible.set(false);
    }, 1500);
  }

  cancelHideTimer() {
    if (this.hideTimer) {
      clearTimeout(this.hideTimer);
      this.hideTimer = null;
    }
  }
}