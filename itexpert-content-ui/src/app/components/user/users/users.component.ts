import { Component, OnInit } from '@angular/core';
import { MatTableDataSource } from "@angular/material/table";
import { User } from "../../../modeles/User";
import { TranslateService } from "@ngx-translate/core";
import { LoggerService } from "../../../services/LoggerService";
import { MatDialog, MatDialogRef } from "@angular/material/dialog";
import { UserService } from "../../../services/UserService";
import { UserDialogComponent } from "../user-dialog/user-dialog.component";
import { ValidationDialogComponent } from "../../commons/validation-dialog/validation-dialog.component";
import { UserAccessService } from "../../../services/UserAccessService";

@Component({
  selector: 'app-users',
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.css']
})
export class UsersComponent implements OnInit {
  displayedColumns: string[] = ['Firstname', 'Lastname', 'Email', 'Role', 'Actions'];
  dataSource: MatTableDataSource<User>;
  dialogRef: MatDialogRef<UserDialogComponent>;
  user: User;

  dialogValidationRef: MatDialogRef<ValidationDialogComponent>;

  constructor(private translate: TranslateService,
    private loggerService: LoggerService,
    private userService: UserService,
    public userAccessService: UserAccessService,
    private dialog: MatDialog) {
  }

  ngOnInit() {
   this.user = this.userAccessService.getCurrentUser()
   this.init();
  }

  init() {
    this.userService.getAll().subscribe(
      (response: any) => {
        response=response.sort((a:any, b:any) => a.lastname.localeCompare(b.lastname));
        this.dataSource = new MatTableDataSource(response);
      },
      (error) => {
        console.error('Request failed with error');
      });
  }

  create() {
    let user: User = new User();
    this.dialogRef = this.dialog.open(UserDialogComponent, {
      data: user,
      height: '80vh',
      width: '80vw',
      disableClose: true
    }
    );
    this.dialogRef.afterClosed()
      .subscribe(result => {
        if (result && result.data) {
          user = result.data;
          this.save(user);
        }
      });
  }

  update(user: User) {
    this.dialogRef = this.dialog.open(UserDialogComponent, {
      data: user,
      height: '80vh',
      width: '80vw',
      disableClose: true
    }
    );
    this.dialogRef.afterClosed()
      .subscribe(result => {
        if (result && result.data) {
          user = result.data;
          this.save(user);
        }
      });
  }

  save(user: User) {
    this.userService.save(user).subscribe(
      response => {
        if (!response) {
          this.translate.get("SAVE_ERROR").subscribe(trad => {
            this.loggerService.warn(trad);
          });
        } else {
          this.translate.get("SAVE_SUCCESS").subscribe(trad => {
            this.loggerService.success(trad);
            this.init();
          });
        }
      },
      error => {
        this.translate.get("SAVE_ERROR").subscribe(trad1 => {
          this.translate.get("CHANGE_USER_CODE_MESSAGE").subscribe(trad2 => {
            this.loggerService.error(trad1 + ",  " + trad2);
          })
        })
      });
  }

  delete(user: User) {
    this.dialogValidationRef = this.dialog.open(ValidationDialogComponent, {
      data: {
        title: "DELETE_TITLE",
        message: "DELETE_MESSAGE"
      },
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    this.dialogValidationRef.afterClosed()
      .subscribe((result: any) => {
        if (result && result.data && result.data === "validated") {


          this.userService.delete(user.id).subscribe(
            response => {
              this.translate.get("DELETE_SUCCESS").subscribe(trad => {
                this.loggerService.success(trad);
                this.init();
              });
            },
            error => {
              this.translate.get("DELETE_ERROR").subscribe(trad => {
                this.loggerService.error(trad);
              })
            });

        }
      });

  }

}
