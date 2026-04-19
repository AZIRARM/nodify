import { Component, OnInit, inject, signal, WritableSignal } from '@angular/core';
import { User } from "../../../modeles/User";
import { MatDialog, MatDialogRef } from "@angular/material/dialog";
import { PasswordDialogComponent } from "../password-dialog/password-dialog.component";
import { UserAccessService } from "../../../services/UserAccessService";

@Component({
  selector: 'app-user-infos',
  templateUrl: './user-infos.component.html',
  styleUrls: ['./user-infos.component.css'],
  standalone: false
})
export class UserInfosComponent implements OnInit {
  user: WritableSignal<User> = signal<User>({} as User);
  dialogRef: MatDialogRef<PasswordDialogComponent>;

  private dialog = inject(MatDialog);
  private userAccessService = inject(UserAccessService);

  ngOnInit() {
    this.user.set(this.userAccessService.getCurrentUser());
  }

  changePassword() {
    this.dialogRef = this.dialog.open(PasswordDialogComponent, {
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    this.dialogRef.afterClosed().subscribe(result => {
      // Optionnel : traiter le résultat
    });
  }
}