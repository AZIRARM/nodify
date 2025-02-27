import {Component, OnInit} from '@angular/core';
import {User} from "../../../modeles/User";
import {MatDialog, MatDialogRef} from "@angular/material/dialog";
import {PasswordDialogComponent} from "../password-dialog/password-dialog.component";

@Component({
  selector: 'app-user-infos',
  templateUrl: './user-infos.component.html',
  styleUrls: ['./user-infos.component.css']
})
export class UserInfosComponent implements OnInit {
  user: User;
  dialogRef: MatDialogRef<PasswordDialogComponent>;

  constructor(private dialog: MatDialog
  ) {

  }

  ngOnInit(): void {
    this.user = JSON.parse(
      JSON.parse(
        JSON.stringify((window.localStorage.getItem('userInfo')))
      )
    );
  }

  changePassword() {
    this.dialogRef = this.dialog.open(PasswordDialogComponent, {
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    this.dialogRef.afterClosed()
      .subscribe(result => {

      });
  }
}
