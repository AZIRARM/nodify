import {Component, OnInit} from '@angular/core';
import {MatDialogRef} from "@angular/material/dialog";
import {TranslateService} from "@ngx-translate/core";
import {LoggerService} from "../../../services/LoggerService";
import {User} from "../../../modeles/User";
import {UserService} from "../../../services/UserService";

@Component({
  selector: 'app-password-dialog',
  templateUrl: './password-dialog.component.html',
  styleUrls: ['./password-dialog.component.css']
})
export class PasswordDialogComponent implements OnInit {

  password: String;
  newPassword: String;
  confirmNewPassword: string;
  userId: String;

  constructor(
    public dialogRef: MatDialogRef<PasswordDialogComponent>,
    private translate: TranslateService,
    private loggerService: LoggerService,
    private userService: UserService
  ) {

  }


  cancel() {
    this.dialogRef.close();
  }

  validate() {

    if(this.password && this.newPassword && this.userId && (this.newPassword === this.confirmNewPassword) ) {

      this.userService.changePassword({
        'password': this.password,
        'newPassword': this.newPassword,
        'userId': this.userId,
      }).subscribe(
        (data: any) => {
          if (data) {
            this.translate.get("SAVE_SUCCESS").subscribe(trad => {
              this.loggerService.success(trad);
              this.dialogRef.close();
              window.localStorage.removeItem("userToken");
              window.localStorage.removeItem("userInfo");
            })
          }
        },
        error => {
          this.translate.get("SAVE_ERROR").subscribe(trad => {
            this.loggerService.success(trad);
          })
        }
      );
    } else {
      this.translate.get("ERROR_FIELDS").subscribe(trad => {
        this.loggerService.error(trad);
      })
    }
  }

  ngOnInit(): void {
    let user: User = JSON.parse(
      JSON.parse(
        JSON.stringify((window.localStorage.getItem('userInfo')))
      )
    );
    this.userId = user.id;
  }


}
