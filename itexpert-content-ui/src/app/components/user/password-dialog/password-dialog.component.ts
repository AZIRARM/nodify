import {Component, OnInit} from '@angular/core';
import {MatDialogRef} from "@angular/material/dialog";
import {TranslateService} from "@ngx-translate/core";
import {LoggerService} from "../../../services/LoggerService";
import {User} from "../../../modeles/User";
import {UserService} from "../../../services/UserService";
import {UserAccessService} from "../../../services/UserAccessService";

@Component({
  selector: 'app-password-dialog',
  templateUrl: './password-dialog.component.html',
  styleUrls: ['./password-dialog.component.css']
})
export class PasswordDialogComponent implements OnInit {

  password: string = '';
  newPassword: string = '';
  confirmNewPassword: string = '';
  user: User;

  constructor(
    public dialogRef: MatDialogRef<PasswordDialogComponent>,
    private translate: TranslateService,
    private loggerService: LoggerService,
    private userAccessService: UserAccessService,
    private userService: UserService
  ) {}

  ngOnInit() {
    this.user = this.userAccessService.getCurrentUser();
  }

  isFormValid(): boolean {
    return !!(this.password &&
             this.newPassword &&
             this.confirmNewPassword &&
             this.newPassword === this.confirmNewPassword &&
             this.user?.id);
  }

  validate() {
    if (this.isFormValid()) {
      this.userService.changePassword({
        'password': this.password,
        'newPassword': this.newPassword,
        'userId': this.user?.id,
      }).subscribe({
        next: (data: any) => {
          if (data) {
            this.translate.get("SAVE_SUCCESS").subscribe(trad => {
              this.loggerService.success(trad);
              this.dialogRef.close();
              window.localStorage.removeItem("userToken");
              window.location.reload();
            });
          }
        },
        error: (error) => {
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
