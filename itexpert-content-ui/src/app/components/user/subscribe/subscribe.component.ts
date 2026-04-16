import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { User } from 'src/app/modeles/User';
import { LoggerService } from 'src/app/services/LoggerService';
import { UserService } from 'src/app/services/UserService';

@Component({
  selector: 'app-subscribe',
  templateUrl: './subscribe.component.html',
  styleUrl: './subscribe.component.css'
})
export class SubscribeComponent {

  userSubscribe: User = new User();

  constructor(
    private userService: UserService,
    private translate: TranslateService,
    private loggerService: LoggerService,
    private router: Router,
  ) { }

  subscribe(): void {
    if (this.validateForm()) {
      this.userService.subscribe(this.userSubscribe)
        .subscribe({
          next: (response) => {
            this.translate.get("SUBSCRIBE_SUCCESS").subscribe(trad => {
              this.loggerService.success(trad);
              this.router.navigateByUrl("/login");
            });
          },
          error: () => {
            this.translate.get("SUBSCRIBE_ERROR").subscribe(trad => {
              this.loggerService.error(trad);
            });
          }
        });
    }
  }

  cancel(): void {
    this.userSubscribe = new User();
    this.router.navigate(['/login']);
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }

  private validateForm(): boolean {
    const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&]).{8,}$/;
    const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

    const isPasswordValid = !!(this.userSubscribe.password && passwordRegex.test(this.userSubscribe.password));
    const isEmailValid = !!(this.userSubscribe.email && emailRegex.test(this.userSubscribe.email));

    if (!isEmailValid && this.userSubscribe.email) {
      this.translate.get("INVALID_EMAIL_MESSAGE").subscribe(trad => {
        this.loggerService.error(trad);
      });
    }

    if (!isPasswordValid && this.userSubscribe.password) {
      this.translate.get("INVALID_PASSWORD_MESSAGE").subscribe(trad => {
        this.loggerService.error(trad);
      });
    }

    return !!(
      isEmailValid &&
      isPasswordValid &&
      this.userSubscribe.firstname &&
      this.userSubscribe.lastname
    );
  }

  login() {
    this.router.navigateByUrl("/login");
  }
}