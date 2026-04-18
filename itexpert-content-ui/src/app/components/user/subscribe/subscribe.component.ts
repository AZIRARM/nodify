import { Component, inject, signal, WritableSignal } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { User } from 'src/app/modeles/User';
import { LoggerService } from 'src/app/services/LoggerService';
import { UserService } from 'src/app/services/UserService';
import { switchMap } from 'rxjs/operators';

@Component({
  selector: 'app-subscribe',
  templateUrl: './subscribe.component.html',
  styleUrl: './subscribe.component.css',
  standalone: false
})
export class SubscribeComponent {

  userSubscribe: WritableSignal<User> = signal<User>(new User());
  isLoading: WritableSignal<boolean> = signal(false);

  private userService = inject(UserService);
  private translate = inject(TranslateService);
  private loggerService = inject(LoggerService);
  private router = inject(Router);

  subscribe(): void {
    if (this.validateForm()) {
      this.isLoading.set(true);
      this.userService.subscribe(this.userSubscribe())
        .pipe(
          switchMap(() => this.translate.get("SUBSCRIBE_SUCCESS"))
        )
        .subscribe({
          next: (trad) => {
            this.loggerService.success(trad);
            this.router.navigateByUrl("/login");
            this.isLoading.set(false);
          },
          error: () => {
            this.translate.get("SUBSCRIBE_ERROR").subscribe(trad => {
              this.loggerService.error(trad);
              this.isLoading.set(false);
            });
          }
        });
    }
  }

  cancel(): void {
    this.userSubscribe.set(new User());
    this.router.navigate(['/login']);
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }

  private validateForm(): boolean {
    const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&]).{8,}$/;
    const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    const user = this.userSubscribe();

    const isPasswordValid = !!(user.password && passwordRegex.test(user.password));
    const isEmailValid = !!(user.email && emailRegex.test(user.email));

    if (!isEmailValid && user.email) {
      this.translate.get("INVALID_EMAIL_MESSAGE").subscribe(trad => {
        this.loggerService.error(trad);
      });
    }

    if (!isPasswordValid && user.password) {
      this.translate.get("INVALID_PASSWORD_MESSAGE").subscribe(trad => {
        this.loggerService.error(trad);
      });
    }

    return !!(
      isEmailValid &&
      isPasswordValid &&
      user.firstname &&
      user.lastname
    );
  }

  login() {
    this.router.navigateByUrl("/login");
  }
}