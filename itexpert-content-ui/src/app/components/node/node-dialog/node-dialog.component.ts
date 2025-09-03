import {Component, Inject, OnDestroy, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {Node} from "../../../modeles/Node";
import {Language} from "../../../modeles/Language";
import {NodeService} from "../../../services/NodeService";
import {LanguageService} from "../../../services/LanguageService";
import {LoggerService} from "../../../services/LoggerService";
import {TranslateService} from "@ngx-translate/core";
import { SlugService } from 'src/app/services/SlugService';
import { toArray, map } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { LockService } from 'src/app/services/LockService';

@Component({
  selector: 'app-node-dialog',
  templateUrl: './node-dialog.component.html',
  styleUrls: ['./node-dialog.component.css']
})
export class NodeDialogComponent implements OnInit, OnDestroy  {

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
    private slugService: SlugService,
    private lockService: LockService
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

      // 🔒 Tente d’acquérir le lock en entrant dans l’édition
      this.lockService.acquire(this.node.code).subscribe(acquired => {
        if (!acquired) {
          this.loggerService.error("Ce nœud est déjà en cours d'édition.");
          this.dialogRef.close();
        } else {
          // Si acquis → démarre la surveillance d’inactivité à 30 min
          this.lockService.startInactivityWatcher(30 * 60 * 1000, () => {
            this.loggerService.warn("Fermeture automatique après 30 min d'inactivité.");
            this.dialogRef.close();
          });
        }
      });
    }

   ngOnDestroy(): void {
      // Libère le lock proprement
      this.lockService.release();
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
      this.slugAvailable = null;
      return;
    }

    (this.slugService.exists(slug) as Observable<string[]>)
      .pipe(
        map((codes: (string | null)[]) => {
          const filtered = codes.filter(c => c != null); // supprime null / undefined
          return (
            filtered.length === 0 ||
            (filtered.length === 1 && filtered[0] === this.node.code)
          );
        })
      )
      .subscribe((available: boolean) => {
        this.slugAvailable = available;
        if (available) {
          this.node.slug = slug;
        }
      });
    }

}
