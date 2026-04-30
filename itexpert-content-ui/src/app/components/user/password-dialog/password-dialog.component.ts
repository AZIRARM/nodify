import { Component, OnInit, inject, signal, WritableSignal } from '@angular/core';
import { MatDialogRef } from "@angular/material/dialog";
import { TranslateService } from "@ngx-translate/core";
import { LoggerService } from "../../../services/LoggerService";
import { User } from "../../../modeles/User";
import { UserService } from "../../../services/UserService";
import { UserAccessService } from "../../../services/UserAccessService";
import { switchMap } from "rxjs/operators";

@Component({
  selector: 'app-password-dialog',
  templateUrl: './password-dialog.component.html',
  styleUrls: ['./password-dialog.component.css'],
  standalone: false
})
export class PasswordDialogComponent implements OnInit {

  password: WritableSignal<string> = signal('');
  newPassword: WritableSignal<string> = signal('');
  confirmNewPassword: WritableSignal<string> = signal('');
  user: WritableSignal<User> = signal<User>({} as User);


  hideCurrentPassword = true;
  hideNewPassword = true;
  hideConfirmPassword = true;

  public dialogRef = inject(MatDialogRef<PasswordDialogComponent>);
  private translate = inject(TranslateService);
  private loggerService = inject(LoggerService);
  private userAccessService = inject(UserAccessService);
  private userService = inject(UserService);

  ngOnInit() {
    this.user.set(this.userAccessService.getCurrentUser());
  }

  isFormValid(): boolean {
    return !!(this.password() &&
      this.newPassword() &&
      this.confirmNewPassword() &&
      this.newPassword() === this.confirmNewPassword() &&
      this.user()?.id);
  }

  validate() {
    if (this.isFormValid()) {

      this.userService.changePassword({
        'password': this.password(),
        'newPassword': this.newPassword(),
        'userId': this.user()?.id,
      }).pipe(
        switchMap((data: any) => {
          if (data) {
            return this.translate.get("SAVE_SUCCESS");
          }
          throw new Error('No data');
        })
      ).subscribe({
        next: (trad: string) => {
          this.loggerService.success(trad);
          this.dialogRef.close();
          window.localStorage.removeItem("userToken");
          window.location.reload();

        },
        error: () => {
          this.translate.get("SAVE_ERROR").subscribe(trad => {
            this.loggerService.error(trad);

          });
        }
      });
    } else {
      this.translate.get("ERROR_FIELDS").subscribe(trad => {
        this.loggerService.error(trad);
      });
    }
  }

  cancel() {
    this.dialogRef.close();
  }
}