import { Component, Inject, OnInit, inject, signal, WritableSignal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { NodeService } from "../../../services/NodeService";
import { User } from "../../../modeles/User";
import { RoleService } from "../../../services/RoleService";
import { StatusEnum } from "../../../modeles/StatusEnum";
import { UserAccessService } from "../../../services/UserAccessService";
import { of } from "rxjs";
import { catchError } from "rxjs/operators";

@Component({
  selector: 'app-user-dialog',
  templateUrl: './user-dialog.component.html',
  styleUrls: ['./user-dialog.component.css'],
  standalone: false
})
export class UserDialogComponent implements OnInit {

  user: User;
  connectedUser: WritableSignal<User> = signal<User>({} as User);
  roles: WritableSignal<any[]> = signal<any[]>([]);
  projects: WritableSignal<any[]> = signal<any[]>([]);

  public dialogRef = inject(MatDialogRef<UserDialogComponent>);
  public userAccessService = inject(UserAccessService);
  private nodeService = inject(NodeService);
  private roleService = inject(RoleService);
  private currentUser = inject(MAT_DIALOG_DATA);

  constructor() {
    if (this.currentUser) {
      this.user = this.currentUser;
    } else {
      this.user = new User();
    }
  }

  ngOnInit() {
    this.connectedUser.set(this.userAccessService.getCurrentUser());
    this.init();
  }

  init() {
    this.roleService.getAll().pipe(
      catchError(error => {
        console.error('Erreur chargement rôles', error);
        return of([]);
      })
    ).subscribe((data: any) => {
      if (data && Array.isArray(data)) {
        this.roles.set(data);
      } else if (data) {
        this.roles.set(Object.values(data));
      } else {
        this.roles.set([]);
      }
    });

    this.nodeService.getParentsNodes(StatusEnum.SNAPSHOT).pipe(
      catchError(error => {
        console.error('Erreur chargement projets', error);
        return of([]);
      })
    ).subscribe((data: any) => {
      if (data && Array.isArray(data)) {
        this.projects.set(data);
      } else if (data) {
        this.projects.set(Object.values(data));
      } else {
        this.projects.set([]);
      }
    });
  }

  get selectedRole(): string {
    return this.user && this.user.roles && this.user.roles.length > 0 ? this.user.roles[0] : '';
  }

  set selectedRole(value: string) {
    this.user.roles = [value];
  }

  connectedUserIsAdmin(): boolean {
    return this.connectedUser()?.roles?.includes("ADMIN") || false;
  }

  userIsAdmin(): boolean {
    return this.user?.roles?.includes("ADMIN") || false;
  }

  isFormValid(): boolean {
    return !!(this.user &&
      this.user.email &&
      this.user.firstname &&
      this.user.lastname &&
      this.user.roles &&
      this.user.roles.length > 0);
  }

  getSelectedProjectsDisplay(): string {
    if (!this.user.projects || this.user.projects.length === 0) {
      return '';
    }

    if (this.user.projects.length === 1) {
      const project = this.projects().find(p => p.code === this.user.projects[0]);
      return project?.name || this.user.projects[0];
    }

    const firstProject = this.projects().find(p => p.code === this.user.projects[0]);
    const count = this.user.projects.length - 1;

    return `${firstProject?.name || this.user.projects[0]} +${count}`;
  }

  cancel() {
    this.dialogRef.close();
  }

  validate() {
    if (!Array.isArray(this.user.roles)) {
      const role: string = this.user.roles as any;
      this.user.roles = [];
      if (role) {
        this.user.roles.push(role);
      }
    }
    this.dialogRef.close({ data: this.user });
  }
}