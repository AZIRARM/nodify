import { Component, OnInit, inject, signal, WritableSignal } from '@angular/core';
import { MatTableDataSource } from "@angular/material/table";
import { User } from "../../../modeles/User";
import { TranslateService } from "@ngx-translate/core";
import { LoggerService } from "../../../services/LoggerService";
import { MatDialog, MatDialogRef } from "@angular/material/dialog";
import { UserService } from "../../../services/UserService";
import { UserDialogComponent } from "../user-dialog/user-dialog.component";
import { ValidationDialogComponent } from "../../commons/validation-dialog/validation-dialog.component";
import { UserAccessService } from "../../../services/UserAccessService";
import { of } from "rxjs";
import { catchError, finalize, switchMap } from "rxjs/operators";

@Component({
  selector: 'app-users',
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.css'],
  standalone: false
})
export class UsersComponent implements OnInit {
  displayedColumns: string[] = ['Firstname', 'Lastname', 'Email', 'Role', 'Actions'];
  dataSource: WritableSignal<MatTableDataSource<User>> = signal(new MatTableDataSource<User>([]));
  dialogRef: MatDialogRef<UserDialogComponent>;
  user: WritableSignal<User> = signal<User>({} as User);
  dialogValidationRef: MatDialogRef<ValidationDialogComponent>;
  isLoading: WritableSignal<boolean> = signal(false);

  private translate = inject(TranslateService);
  private loggerService = inject(LoggerService);
  private userService = inject(UserService);
  public userAccessService = inject(UserAccessService);
  private dialog = inject(MatDialog);

  ngOnInit() {
    this.user.set(this.userAccessService.getCurrentUser());
    this.init();
  }

  init() {
    this.isLoading.set(true);
    this.userService.getAll().pipe(
      catchError((error) => {
        console.error('Request failed with error');
        return of([]);
      }),
      finalize(() => this.isLoading.set(false))
    ).subscribe((response: any) => {
      if (response) {
        response = response.sort((a: any, b: any) => a.lastname.localeCompare(b.lastname));
        this.dataSource.set(new MatTableDataSource(response));
      }
    });
  }

  create() {
    let user: User = new User();
    this.dialogRef = this.dialog.open(UserDialogComponent, {
      data: user,
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
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
    });
    this.dialogRef.afterClosed()
      .subscribe(result => {
        if (result && result.data) {
          user = result.data;
          this.save(user);
        }
      });
  }

  activate(user: User) {
    user.validated = !user.validated;
    this.save(user);
  }

  save(user: User) {
    this.isLoading.set(true);
    this.userService.save(user).pipe(
      switchMap((response) => {
        if (!response) {
          return this.translate.get("SAVE_ERROR").pipe(
            switchMap((trad: string) => {
              this.loggerService.warn(trad);
              throw new Error(trad);
            })
          );
        } else {
          return this.translate.get("SAVE_SUCCESS");
        }
      }),
      catchError((error) => {
        if (error.message?.includes("SAVE_ERROR")) {
          return this.translate.get("CHANGE_USER_CODE_MESSAGE").pipe(
            switchMap((trad2: string) => {
              this.loggerService.error(error.message + ",  " + trad2);
              throw error;
            })
          );
        }
        return this.translate.get("SAVE_ERROR").pipe(
          switchMap((trad: string) => {
            this.loggerService.error(trad);
            throw error;
          })
        );
      }),
      finalize(() => this.isLoading.set(false))
    ).subscribe((trad: string) => {
      this.loggerService.success(trad);
      this.init();
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

    this.dialogValidationRef.afterClosed().pipe(
      switchMap((result: any) => {
        if (result && result.data && result.data === "validated") {
          return this.userService.delete(user.id).pipe(
            switchMap(() => this.translate.get("DELETE_SUCCESS")),
            catchError((error: any) => {
              return this.translate.get("DELETE_ERROR").pipe(
                switchMap((trad: string) => {
                  this.loggerService.error(trad);
                  throw error;
                })
              );
            })
          );
        }
        return of(null);
      })
    ).subscribe((trad: string | null) => {
      if (trad) {
        this.loggerService.success(trad);
        this.init();
      }
    });
  }
}