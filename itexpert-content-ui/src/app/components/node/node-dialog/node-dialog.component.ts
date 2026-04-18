import { Component, Inject, OnDestroy, OnInit, inject, signal, WritableSignal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from "@angular/material/dialog";
import { Node } from "../../../modeles/Node";
import { Language } from "../../../modeles/Language";
import { NodeService } from "../../../services/NodeService";
import { LanguageService } from "../../../services/LanguageService";
import { LoggerService } from "../../../services/LoggerService";
import { TranslateService } from "@ngx-translate/core";
import { SlugService } from 'src/app/services/SlugService';
import { map } from 'rxjs/operators';
import { interval, Observable, Subscription, of } from 'rxjs';
import { LockService } from 'src/app/services/LockService';
import { AuthenticationService } from "../../../services/AuthenticationService";
import { ValidationDialogComponent } from "../../commons/validation-dialog/validation-dialog.component";
import { catchError, switchMap } from 'rxjs/operators';

@Component({
  selector: 'app-node-dialog',
  templateUrl: './node-dialog.component.html',
  styleUrls: ['./node-dialog.component.css'],
  standalone: false
})
export class NodeDialogComponent implements OnInit, OnDestroy {

  node: Node;
  isProject: boolean;
  isCreation: boolean;

  parents: WritableSignal<Node[]> = signal<Node[]>([]);
  childreens: WritableSignal<Node[]> = signal<Node[]>([]);
  languages: WritableSignal<Language[]> = signal<Language[]>([]);

  currentSlug: WritableSignal<string | null> = signal<string | null>(null);
  slugAvailable: WritableSignal<boolean | null> = signal<boolean | null>(true);
  isLoading: WritableSignal<boolean> = signal(false);

  private lockCheckSub?: Subscription;
  validationModal: MatDialogRef<ValidationDialogComponent>;

  public dialogRef = inject(MatDialogRef<NodeDialogComponent>);
  private nodeService = inject(NodeService);
  private languageService = inject(LanguageService);
  private translateService = inject(TranslateService);
  private loggerService = inject(LoggerService);
  private slugService = inject(SlugService);
  private authenticationService = inject(AuthenticationService);
  private lockService = inject(LockService);
  private dialog = inject(MatDialog);
  private content = inject(MAT_DIALOG_DATA);

  constructor() {
    if (this.content) {
      this.node = this.content;
      this.isProject = !this.node.parentCode;
      this.isCreation = !this.node.id;
      this.currentSlug.set(this.node.slug);
    }
  }

  ngOnInit(): void {
    this.init();

    if (this.node && this.node.code) {
      this.lockService.acquire(this.node.code).subscribe(acquired => {
        if (!acquired) {
          this.translateService.get("RESOURCE_LOCKED")
            .subscribe(translation => {
              this.loggerService.warn(translation);
            });
          this.dialogRef.close();
        } else {
          this.lockService.startInactivityWatcher(30 * 60 * 1000, () => {
            this.translateService.get("RESOURCE_RELEASED")
              .subscribe(translation => {
                this.loggerService.warn(translation);
              });
            this.dialogRef.close();
          });

          this.lockCheckSub = interval(10000).subscribe(() => {
            this.lockService.getLockInfoSocket(this.node.code, this.authenticationService.getAccessToken()).subscribe((lockInfo: any) => {
              if (lockInfo.locked) {
                this.translateService.get("RESOURCE_LOCKED_BY_OTHER")
                  .subscribe(translation => this.loggerService.warn(translation));
                this.dialogRef.close();
              }
            });
          });
        }
      });
    }
  }

  ngOnDestroy(): void {
    if (this.lockCheckSub) {
      this.lockCheckSub.unsubscribe();
    }
    this.lockService.release();
  }

  cancel() {
    this.dialogRef.close();
  }

  validate() {
    this.isLoading.set(true);
    this.nodeService.slugExists(this.node.code, this.node.slug)
      .pipe(
        catchError((error) => {
          this.translateService.get('SLUG_ALREADY_EXISTS')
            .subscribe(translation => {
              this.loggerService.error(translation);
            });
          return of(null);
        })
      )
      .subscribe((exists: any) => {
        if (exists) {
          this.translateService.get('SLUG_ALREADY_EXISTS')
            .subscribe(translation => {
              this.loggerService.error(translation);
            });
        } else {
          this.dialogRef.close({ data: this.node });
        }
        this.isLoading.set(false);
      });
  }

  init() {
    this.isLoading.set(true);
    this.nodeService.getAllNodes().pipe(
      catchError(error => {
        console.error(error);
        return of([]);
      })
    ).subscribe(data => {
      if (data) {
        this.parents.set(data as Node[]);
        let tabChildreens: Node[] = [];
        (data as Node[]).map(child => {
          if (child.id !== this.node.id && child.id !== this.node.parentCode && this.node.id !== child.id) {
            tabChildreens.push(child);
          }
        });
        this.childreens.set(tabChildreens);
      }
      this.isLoading.set(false);
    });

    this.languageService.getAll().pipe(
      catchError(error => {
        console.error(error);
        return of([]);
      })
    ).subscribe(data => {
      if (data) {
        this.languages.set(data as Language[]);
      }
    });
  }

  generateCode() {
    if (this.node && !this.node.id) {
      this.node.code = this.node.name.replace(/[\W_]+/g, "_").toUpperCase() + '-' +
        (this.isProject ? '' : this.node.parentCodeOrigin?.split("-")[0] + '-') +
        (new Date()).getTime();
    }
  }

  onSlugChange(slug: string) {
    if (!slug) {
      this.slugAvailable.set(null);
      return;
    }

    (this.slugService.exists(slug) as Observable<string[]>)
      .pipe(
        map((codes: (string | null)[]) => {
          const filtered = codes.filter(c => c != null);
          return (
            filtered.length === 0 ||
            (filtered.length === 1 && filtered[0] === this.node.code)
          );
        })
      )
      .subscribe((available: boolean) => {
        this.slugAvailable.set(available);
        if (available) {
          this.node.slug = slug;
        }
      });
  }

  forcePropagation() {
    this.validationModal = this.dialog.open(ValidationDialogComponent, {
      data: {
        title: "PROPAGATION_MAX_HISTORY_TITLE",
        message: "PROPAGATION_MAX_HISTORY_MESSAGE"
      },
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    this.validationModal.afterClosed()
      .subscribe(result => {
        if (result && result.data !== 'canceled') {
          this.nodeService.propagateMaxHistoryToKeep(this.node.code).pipe(
            switchMap(() => this.translateService.get("PROPAGATION_MAX_HISTORY_SUCCESS")),
            catchError((error) => {
              return this.translateService.get("PROPAGATION_MAX_HISTORY_ERROR").pipe(
                switchMap((trad1: string) => {
                  this.loggerService.error(trad1);
                  throw error;
                })
              );
            })
          ).subscribe((trad: string) => {
            this.loggerService.success(trad);
            this.init();
          });
        }
      });
  }

  isFormValid(): boolean {
    return !!(this.node.defaultLanguage
      && this.node.code
      && this.slugAvailable() === true
      && this.node.name
      && this.node.name.trim().length >= 4);
  }

  getSelectedLanguagesDisplay(): string {
    if (!this.node.languages || this.node.languages.length === 0) {
      return '';
    }

    if (this.node.languages.length === 1) {
      return this.node.languages[0];
    }

    const firstLang = this.languages().find(l => l.code === this.node.languages[0]);
    const count = this.node.languages.length - 1;

    return `${firstLang?.name || firstLang?.code || this.node.languages[0]} +${count}`;
  }
}