import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {Node} from "../../../modeles/Node";
import {Language} from "../../../modeles/Language";
import {NodeService} from "../../../services/NodeService";
import {LanguageService} from "../../../services/LanguageService";
import {LoggerService} from "../../../services/LoggerService";
import {TranslateService} from "@ngx-translate/core";
import { SlugService } from 'src/app/services/SlugService';
import { map, of, switchMap } from 'rxjs';

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

  currentSlug: string  | null = null;
  slugAvailable: boolean | null = true;

  constructor(
    public dialogRef: MatDialogRef<NodeDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public content: Node,
    private nodeService: NodeService,
    private languageService: LanguageService,
    private translateService: TranslateService,
    private loggerService: LoggerService,
    private slugService: SlugService
  ) {
    if (content) {
      this.node = content;
      this.isProject = !this.node.parentCode;
      this.isCreation = !this.node.id;
      this.currentSlug = this.node.slug;
    }
  }

  ngOnInit(): void {
    this.init();
  }


  cancel() {
    this.dialogRef.close();
  }

  validate() {
    this.nodeService.slugExists(this.node.code, this.node.slug)
      .subscribe((exists: any) => {
        if (exists) {
          this.translateService.get('SLUG ALREADY EXISTS')
            .subscribe(translation => {
              this.loggerService.error(translation);
            });
        } else {
          this.dialogRef.close({data: this.node});
        }
      });
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

  onSlugChange(slug: string) {
  if (!slug) {
    this.slugAvailable = true;
    return;
  }

  this.slugService.exists(slug).pipe(
    switchMap((exists: any) => {
      if (exists) {
        return this.nodeService.getNodeByCodeAndStatus(this.node.code, this.node.status).pipe(
          map((node: any) => node.slug === slug) // true si c’est le même slug, sinon false
        );
      } else {
        return of(true);
      }
    })
  ).subscribe(isAvailable => {
    this.slugAvailable = isAvailable;
  });
}

}
