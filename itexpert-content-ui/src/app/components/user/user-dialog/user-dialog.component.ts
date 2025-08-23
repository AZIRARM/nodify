import {Component, Inject, OnInit} from '@angular/core';
import {Node} from "../../../modeles/Node";
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {NodeService} from "../../../services/NodeService";
import {User} from "../../../modeles/User";
import {RoleService} from "../../../services/RoleService";
import {StatusEnum} from "../../../modeles/StatusEnum";
import {UserAccessService} from "../../../services/UserAccessService";

@Component({
  selector: 'app-user-dialog',
  templateUrl: './user-dialog.component.html',
  styleUrls: ['./user-dialog.component.css']
})
export class UserDialogComponent implements OnInit {

  user: User;
  connectedUser: User;

  roles: any[] = [];
  projects: any[] = [];

  constructor(
    public dialogRef: MatDialogRef<UserDialogComponent>,
    @Inject(MAT_DIALOG_DATA) private currentUser: User,
    private userAccessService: UserAccessService,
    private nodeService: NodeService,
    private roleService: RoleService,
  ) {
    if (currentUser) {
      this.user = currentUser;
    }
  }


  ngOnInit() {
   this.user = this.userAccessService.getCurrentUser()
   this.init();
  }

  cancel() {
    this.dialogRef.close();
  }

  validate() {
    if (!Array.isArray(this.user!.roles)) {
      let role: string = this.user!.roles;
      this.user!.roles = [];
      this.user!.roles.push(role);
    }
    this.dialogRef.close({data: this.user});
  }


  init() {
    this.roleService.getAll().subscribe(
      data => {
        if (data) {
          this.roles = <Array<Node>>data;
        }
      },
      error => {
        console.error(error);
      }
    );
    this.nodeService.getParentsNodes(StatusEnum.SNAPSHOT).subscribe(
      (data: any) => {
        if (data) {
          this.projects = data;
        }
      },
      error => {
        console.error(error);
      }
    );
  }

  public isAdmin(): boolean {
    return !!(this.connectedUser
      && this.connectedUser.roles
      && this.connectedUser.roles.includes("ADMIN"));

  }

  currentUserIsAdmin() {
    return (
      this.user
      && this.user!.id
      && this.user!.roles
      && this.user!.roles.includes("ADMIN")
    ) && this.user!.id === this.connectedUser!.id;
  }

  get selectedRole(): string {
    return this.user && this.user!.roles && this.user.roles.length > 0 ? this.user.roles[0] : '';
  }

  set selectedRole(value: string) {
    this.user!.roles = [value]; // Met à jour la première valeur du tableau
  }
}
