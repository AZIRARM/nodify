import {Component, Inject, OnDestroy, OnInit} from '@angular/core';
import {MatTableDataSource} from "@angular/material/table";
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {LanguageService} from "../../../services/LanguageService";
import {Language} from "../../../modeles/Language";
import {Translation} from "../../../modeles/Translation";
import {UserAccessService} from "../../../services/UserAccessService";
import { LockService } from 'src/app/services/LockService';
import { LoggerService } from 'src/app/services/LoggerService';
import { TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import {AuthenticationService} from "../../../services/AuthenticationService";

@Component({
    selector: 'app-translations-dialog',
    templateUrl: './translations-dialog.component.html',
    styleUrls: ['./translations-dialog.component.css'],
    standalone: false
})
export class TranslationsDialogComponent implements  OnInit, OnDestroy {
  data: any;
  workingTranslations: Translation[] = [];
  current: any;
  languages: Language[] = [];
  displayedColumns: string[] = ['Language', 'Key', 'Value', 'Actions'];
  dataSource: MatTableDataSource<Translation>;
  private lockCheckSub: Subscription;
  editingItem: Translation | null = null;
  originalItemBackup: any = null;

  constructor(
    public dialogRef: MatDialogRef<TranslationsDialogComponent>,
    public userAccessService: UserAccessService,
    @Inject(MAT_DIALOG_DATA) public content: any,
    private languageService: LanguageService,
    private loggerService: LoggerService,
    private lockService: LockService,
    private authenticationService: AuthenticationService,
    private translateService: TranslateService,
  ) {
    if (content) {
      this.data = content;
      if (!this.data.translations) {
        this.data.translations = [];
      }
      this.workingTranslations = this.data.translations.map((t: Translation) => ({...t}));
    }
  }

  ngOnInit() {
    this.init();

    this.lockService.acquire(this.data.code).subscribe(acquired => {
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

       this.lockService.getLockInfoSocket(this.data.code, this.authenticationService.getAccessToken()).subscribe((lockInfo: any) => {
         if (lockInfo.locked) {
           this.translateService.get("RESOURCE_LOCKED_BY_OTHER")
             .subscribe(translation => this.loggerService.warn(translation));
           this.dialogRef.close();
         }
       });

      }
    });
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
    this.data.translations = this.workingTranslations;
    this.dialogRef.close({data: this.data});
  }

  init() {
    this.languageService.getAll().subscribe(
      (response: any) => {
        if (response) {
          this.languages = response;
          this.workingTranslations.sort((a: Translation, b: Translation) => a.language.localeCompare(b.language));
          this.dataSource = new MatTableDataSource(this.workingTranslations);
          this.current = new Translation();
        }
      },
      error => {
        console.error(error);
      }
    );
  }

  delete(translation: Translation) {
    if (translation) {
      this.workingTranslations = this.workingTranslations
        .filter((v: Translation) => !(v.key == translation.key && v.language == translation.language));
      this.dataSource = new MatTableDataSource(this.workingTranslations);
      this.current = new Translation();
    }
  }

  create() {
    if (this.current?.language && this.current?.key && this.current?.value) {
      this.workingTranslations = this.workingTranslations?.filter(
        (trans: Translation) =>
          !(trans.language === this.current.language && trans.key === this.current.key)
      ) || [];

      this.workingTranslations.push(this.current);
      this.workingTranslations.sort((a: Translation, b: Translation) => a.language.localeCompare(b.language));

      this.dataSource = new MatTableDataSource(this.workingTranslations);
      this.current = new Translation();
    }
  }

  startEdit(element: Translation) {
    if (this.editingItem) {
      this.cancelEdit();
    }
    this.originalItemBackup = {
      language: element.language,
      key: element.key,
      value: element.value
    };
    this.editingItem = element;
  }

  saveEdit(element: Translation) {
    if (!element.language || !element.key || !element.value) {
      this.translateService.get("FIELDS_REQUIRED").subscribe(translation => {
        this.loggerService.warn(translation);
      });
      return;
    }

    const exists = this.workingTranslations.some(t =>
      t !== element && t.language === element.language && t.key === element.key
    );

    if (exists) {
      this.translateService.get("TRANSLATION_ALREADY_EXISTS").subscribe(translation => {
        this.loggerService.warn(translation);
      });
      return;
    }

    const index = this.workingTranslations.findIndex(t => t === element);
    if (index !== -1) {
      this.workingTranslations[index] = element;
      this.workingTranslations.sort((a: Translation, b: Translation) => a.language.localeCompare(b.language));
      this.dataSource = new MatTableDataSource(this.workingTranslations);
    }

    this.editingItem = null;
    this.originalItemBackup = null;
  }

  cancelEdit() {
    if (this.editingItem && this.originalItemBackup) {
      this.editingItem.language = this.originalItemBackup.language;
      this.editingItem.key = this.originalItemBackup.key;
      this.editingItem.value = this.originalItemBackup.value;
    }
    this.editingItem = null;
    this.originalItemBackup = null;
  }

  addNewRow() {
    const newTranslation = new Translation();
    newTranslation.language = '';
    newTranslation.key = '';
    newTranslation.value = '';
    this.workingTranslations.push(newTranslation);
    this.workingTranslations.sort((a: Translation, b: Translation) => a.language.localeCompare(b.language));
    this.dataSource = new MatTableDataSource(this.workingTranslations);
    this.startEdit(newTranslation);
  }

  hasChanges(): boolean {
    if (this.editingItem !== null) return true;

    const originalTranslations = this.data.translations || [];
    const currentTranslations = this.workingTranslations || [];

    if (originalTranslations.length !== currentTranslations.length) return true;

    for (let i = 0; i < currentTranslations.length; i++) {
      const original = originalTranslations[i];
      const current = currentTranslations[i];
      if (!original || !current) return true;
      if (current.language !== original.language ||
          current.key !== original.key ||
          current.value !== original.value) {
        return true;
      }
    }

    return false;
  }

  isFormValid(): boolean {
    return !!(this.current.language && this.current.key && this.current.value);
  }
}
