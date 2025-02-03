import {Component, Inject, OnInit} from '@angular/core';
import {User} from "../../../modeles/User";
import {NodeDialogComponent} from "../../node/node-dialog/node-dialog.component";
import {Node} from "../../../modeles/Node";
import {MAT_DIALOG_DATA, MatDialog, MatDialogRef} from "@angular/material/dialog";
import {TranslateService} from "@ngx-translate/core";
import {LoggerService} from "../../../services/LoggerService";
import {ActivatedRoute, Router} from "@angular/router";
import {NodeService} from "../../../services/NodeService";
import {RoleService} from "../../../services/RoleService";
import {PasswordDialogComponent} from "../password-dialog/password-dialog.component";
import {Env} from "../../../../assets/configurations/environment";

@Component({
  selector: 'app-user-infos',
  templateUrl: './user-infos.component.html',
  styleUrls: ['./user-infos.component.css']
})
export class UserInfosComponent implements OnInit {
  user:User;
  dialogRef: MatDialogRef<PasswordDialogComponent>;
  constructor(
    private log: LoggerService,
    private loggerService: LoggerService,
    private router: Router,
    private dialog: MatDialog
  ) {

  }
  ngOnInit(): void {
    this.user = JSON.parse(
      JSON.parse(
        JSON.stringify( ( window.localStorage.getItem('userInfo') ) )
      )
    );
  }

  changePassword() {
    this.dialogRef = this.dialog.open(PasswordDialogComponent,{
      height: '80vh',
      width:  '80vw',
      disableClose: true
    });
    this.dialogRef.afterClosed()
      .subscribe(result => {

      });
  }
}
