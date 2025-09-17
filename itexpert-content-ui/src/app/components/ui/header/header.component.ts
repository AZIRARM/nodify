import {Component} from '@angular/core';
import {Router} from '@angular/router';
import {TranslateService} from "@ngx-translate/core";
import {SidenavService} from "../../../services/SidenavService";
import {ValidationDialogComponent} from "../../commons/validation-dialog/validation-dialog.component";
import {MatDialog, MatDialogRef} from "@angular/material/dialog";
import {NotificationService} from "../../../services/NotificationService";
import {ThemeService} from "../../../services/ThemeService";
import {AuthenticationService} from "../../../services/AuthenticationService";
import {UserAccessService} from "../../../services/UserAccessService";
import { CookiesService } from 'src/app/services/CookiesService';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent {
  user: any;

  theme: string = 'dark';

  lastNotificationUpdate: number;

  private isOpen: boolean = false;
  dialogRef: MatDialogRef<ValidationDialogComponent>;
  nbNotifications: number = 0;

  selectedLanguage = 'fr';

  languageList: any = [
    {code: 'en', label: 'English'},
    {code: 'fr', label: 'French'},
    {code: 'es', label: 'Español'},
    {code: 'de', label: 'Deutsch'},
    {code: 'pt', label: 'Português'},
    {code: 'ar', label: 'عربي'},
  ];


  isDarkMode: boolean = true;

  constructor(
    private translate: TranslateService,
    private notificationService: NotificationService,
    private router: Router,
    private dialog: MatDialog,
    private sidenavService: SidenavService,
    private themeService: ThemeService,
    private authenticationService: AuthenticationService,
    private cookiesService: CookiesService,
    private userAccessService: UserAccessService) {
    this.selectedLanguage = window.localStorage.getItem("defaultLanguage")!;

    this.isDarkMode = this.themeService.isDarkModeEnabled();
    this.themeService.setTheme();
    this.initCountUreadedNotifications();
  }


  toggleSidenav(): void {
    if (this.isOpen) {
      this.isOpen = false;
    } else {
      this.isOpen = true;
    }
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
        this.dialogRef.afterClosed()
          .subscribe(result => {
            if (result.data === 'validated') {
              this.cookiesService.eraseCookie("userToken");
              this.router.navigate(["/login"]);
              window.location.reload();
            }
          });
      });

    });
  }

  initCountUreadedNotifications() {
     if (this.authenticationService.isAuthenticated()) {
      this.notificationService.connectWebSocket(this.authenticationService.getAccessToken()).subscribe({
        next: (data: any) => {
          this.nbNotifications = data.count;
        },
        error: (err:any) => console.error("Erreur WebSocket:", err),
        complete: () => console.log("Socket fermé")
      });
    }
  }

  changeSiteLanguage(localeCode: string): void {
    this.translate.use(localeCode);
    window.localStorage.setItem("defaultLanguage", localeCode);
    window.location.reload();
  }


  toggleTheme() {
    this.isDarkMode = !this.isDarkMode;
    this.themeService.toggleTheme(this.isDarkMode ? 'dark' : 'light');
  }
}
