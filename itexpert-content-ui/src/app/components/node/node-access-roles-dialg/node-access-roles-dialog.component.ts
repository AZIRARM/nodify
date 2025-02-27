import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {AccessRole} from "../../../modeles/AccessRole";
import {AccessRoleService} from "../../../services/AccessRoleService";
import {ToastrService} from "ngx-toastr";

@Component({
  selector: 'app-node-access-roles-dialog',
  templateUrl: './node-access-roles-dialog.component.html',
  styleUrls: ['./node-access-roles-dialog.component.css']
})
export class NodeAccessRolesDialogComponent implements OnInit {


  node: any;
  roles: AccessRole[];

  constructor(private toast: ToastrService,
              private acessRoleService: AccessRoleService,
              public dialogRef: MatDialogRef<NodeAccessRolesDialogComponent>,
              @Inject(MAT_DIALOG_DATA) public content: any
  ) {
    if (content) {
      this.node = content;
    }
  }

  ngOnInit() {
    this.init();
  }

  init() {
    this.acessRoleService.getAll().subscribe(
      (response: any) => {                           //next() callback
        this.roles = response;
      },
      (error) => {                              //error() callback
        this.toast.error('Request failed with error');
      });
  }

  cancel() {
    this.dialogRef.close();
  }

  validate() {
    this.dialogRef.close({data: this.node});
  }
}
