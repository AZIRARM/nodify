import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {Node} from "../../../modeles/Node";
import {Language} from "../../../modeles/Language";
import {NodeService} from "../../../services/NodeService";
import {LanguageService} from "../../../services/LanguageService";

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
    private nodeService: NodeService,
    private languageService: LanguageService
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
    if (this.node && !this.node.id) {
      this.node.code = this.node.name.replace(/[\W_]+/g, "_").toUpperCase() + '-' +
        (this.isProject ? '' : this.node.parentCodeOrigin.split("-")[0] + '-') +
        (new Date()).getTime();
    }
  }
}
