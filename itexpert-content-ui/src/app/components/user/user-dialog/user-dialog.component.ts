import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { NodeService } from "../../../services/NodeService";
import { User } from "../../../modeles/User";
import { RoleService } from "../../../services/RoleService";
import { StatusEnum } from "../../../modeles/StatusEnum";
import { UserAccessService } from "../../../services/UserAccessService";

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

  filteredProjects: any[] = [];
  projectSearchText: string = '';

  constructor(
    public dialogRef: MatDialogRef<UserDialogComponent>,
    @Inject(MAT_DIALOG_DATA) private currentUser: User,
    public userAccessService: UserAccessService,
    private nodeService: NodeService,
    private roleService: RoleService,
  ) {
    if (currentUser) {
      this.user = currentUser;
    } else {
      this.user = new User();
    }
  }

  ngOnInit() {
    this.connectedUser = this.userAccessService.getCurrentUser();
    this.init();
  }

  init() {
    this.roleService.getAll().subscribe(
      (data: any) => {
        if (data && Array.isArray(data)) {
          this.roles = data;
        } else if (data) {
          // Si data n'est pas un tableau mais un objet, on le convertit
          this.roles = Object.values(data);
        } else {
          this.roles = [];
        }
      },
      error => {
        console.error('Erreur chargement rôles', error);
        this.roles = [];
      }
    );

    this.nodeService.getAllActifs().subscribe(
      (data: any) => {
        const results = Array.isArray(data) ? data : (data ? Object.values(data) : []);
        this.projects = results;
        this.filteredProjects = results;
      },
      error => {
        console.error('Erreur chargement projets', error);
        this.projects = [];
        this.filteredProjects = [];
      }
    );
  }

  get selectedRole(): string {
    return this.user && this.user.roles && this.user.roles.length > 0 ? this.user.roles[0] : '';
  }

  set selectedRole(value: string) {
    this.user.roles = [value];
  }

  connectedUserIsAdmin(): boolean {
    return this.connectedUser?.roles?.includes("ADMIN") || false;
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
      const project = this.projects.find(p => p.code === this.user.projects[0]);
      return project?.name || this.user.projects[0];
    }

    const firstProject = this.projects.find(p => p.code === this.user.projects[0]);
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

  filterProjects(event: any) {
    const search = event.target.value.toLowerCase();
    this.projectSearchText = search;

    if (!search) {
      this.filteredProjects = this.projects;
      return;
    }

    this.filteredProjects = this.projects.filter(p =>
      p.code?.toLowerCase().includes(search) ||
      p.slug?.toLowerCase().includes(search) ||
      p.name?.toLowerCase().includes(search)
    );
  }
}
