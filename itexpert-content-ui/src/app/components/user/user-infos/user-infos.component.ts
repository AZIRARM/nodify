import { Component, OnInit, inject, signal, WritableSignal } from '@angular/core';
import { User } from "../../../modeles/User";
import { MatDialog, MatDialogRef } from "@angular/material/dialog";
import { PasswordDialogComponent } from "../password-dialog/password-dialog.component";
import { UserAccessService } from "../../../services/UserAccessService";
import { UserService } from "../../../services/UserService";
import { LoggerService } from 'src/app/services/LoggerService';
import { TranslateService } from '@ngx-translate/core';
import { take } from 'rxjs';

@Component({
  selector: 'app-user-infos',
  templateUrl: './user-infos.component.html',
  styleUrls: ['./user-infos.component.css'],
  standalone: false
})
export class UserInfosComponent implements OnInit {
  user: WritableSignal<User> = signal<User>({} as User);
  editedUser: User = {} as User;
  isEditing: WritableSignal<boolean> = signal(false);
  dialogRef: MatDialogRef<PasswordDialogComponent>;

  private dialog = inject(MatDialog);
  private userAccessService = inject(UserAccessService);
  private userService = inject(UserService);
  private translateService = inject(TranslateService);
  private loggerService = inject(LoggerService);

  ngOnInit() {
    this.user.set(this.userAccessService.getCurrentUser());
    this.editedUser = { ...this.user() };
  }

  edit() {
    this.isEditing.set(true);
    this.editedUser = { ...this.user() };
  }

  cancel() {
    this.isEditing.set(false);
    this.editedUser = { ...this.user() };
  }

  save() {
    this.userService.save(this.editedUser).subscribe(updatedUser => {
      this.userAccessService.setCurrentUser(updatedUser);
      this.isEditing.set(false);
      this.translateService.get('SAVE_SUCCESS')
        .pipe(take(1))
        .subscribe((message: unknown) => {
          this.loggerService.success(message as string);
          this.ngOnInit();
        });
    });
  }

  changePassword() {
    this.dialogRef = this.dialog.open(PasswordDialogComponent, {
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    this.dialogRef.afterClosed().subscribe(result => {
    });
  }
}