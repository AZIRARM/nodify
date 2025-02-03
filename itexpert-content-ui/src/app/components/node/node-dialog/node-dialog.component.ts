import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialog, MatDialogRef} from "@angular/material/dialog";
import {Node} from "../../../modeles/Node";
import {Language} from "../../../modeles/Language";
import {TranslateService} from "@ngx-translate/core";
import {ToastrService} from "ngx-toastr";
import {ActivatedRoute, Router} from "@angular/router";
import {NodeService} from "../../../services/NodeService";
import {LoggerService} from "../../../services/LoggerService";
import {LanguageService} from "../../../services/LanguageService";
import {AccessRole} from "../../../modeles/AccessRole";
import {AccessRoleService} from "../../../services/AccessRoleService";

@Component({
  selector: 'app-node-dialog',
  templateUrl: './node-dialog.component.html',
  styleUrls: ['./node-dialog.component.css']
})
export class NodeDialogComponent implements OnInit {

  node: Node;
  isProject: boolean;
  isCreation: boolean;

  parents: Node[] = [];
  childreens: Node[] = [];
  languages: Language[] = [];


  constructor(
    public dialogRef: MatDialogRef<NodeDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public content: Node,
    private translate: TranslateService,
    private toast: ToastrService,
    private route: ActivatedRoute,
    private nodeService: NodeService,
    private loggerService: LoggerService,
    private languageService: LanguageService,
    private accessRoleService: AccessRoleService,
    private router: Router,
    private dialog: MatDialog
  ) {
    if (content) {
      this.node = content;
      this.isProject = !this.node.parentCode;
      this.isCreation = !this.node.id;
    }
  }

  ngOnInit(): void {
    this.init();
  }


  cancel() {
    this.dialogRef.close();
  }

  validate() {
    this.dialogRef.close({data: this.node});
  }


  init() {
    this.nodeService.getAllNodes().subscribe(
      data => {
        if (data) {
          this.parents = <Array<Node>>data;
          this.childreens = <Array<Node>>data;
          let tabChildreens: Node[] = new Array();
          this.childreens.map(child => {
            if (child.id !== this.node.id && child.id !== this.node.parentCode && this.node.id !== child.id) {
              tabChildreens.push(child);
            }
          });
          this.childreens = tabChildreens;
        }
      },
      error => {
        console.error(error);
      }
    );

    this.languageService.getAll().subscribe(
      data => {
        if (data) {
          this.languages = <Array<Language>>data;
        }
      },
      error => {
        console.error(error);
      }
    );
  }

  generateCode() {
    if(this.node && !this.node.id){
      this.node.code = this.node.name.replace(/[\W_]+/g,"_").toUpperCase()+'-'+
        (this.isProject ? '': this.node.parentCodeOrigin.split("-")[0]+'-')+
        (new Date()).getTime();
    }
  }
}
