import { Component, Inject, OnDestroy, OnInit, inject, signal, WritableSignal } from '@angular/core';
import { MatTableDataSource } from "@angular/material/table";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { LanguageService } from "../../../services/LanguageService";
import { Language } from "../../../modeles/Language";
import { Translation } from "../../../modeles/Translation";
import { UserAccessService } from "../../../services/UserAccessService";
import { LockService } from 'src/app/services/LockService';
import { LoggerService } from 'src/app/services/LoggerService';
import { TranslateService } from '@ngx-translate/core';
import { Subscription, of } from 'rxjs';
import { AuthenticationService } from "../../../services/AuthenticationService";
import { catchError, switchMap } from 'rxjs/operators';

@Component({
  selector: 'app-translations-dialog',
  templateUrl: './translations-dialog.component.html',
  styleUrls: ['./translations-dialog.component.css'],
  standalone: false
})
export class TranslationsDialogComponent implements OnInit, OnDestroy {
  data: any;
  workingTranslations: WritableSignal<Translation[]> = signal<Translation[]>([]);
  current: WritableSignal<Translation> = signal<Translation>(new Translation());
  languages: WritableSignal<Language[]> = signal<Language[]>([]);
  displayedColumns: string[] = ['Language', 'Key', 'Value', 'Actions'];
  dataSource: WritableSignal<MatTableDataSource<Translation>> = signal(new MatTableDataSource<Translation>([]));
  editingItem: WritableSignal<Translation | null> = signal<Translation | null>(null);
  originalItemBackup: any = null;
  private lockCheckSub?: Subscription;

  private dialogRef = inject(MatDialogRef<TranslationsDialogComponent>);
  public userAccessService = inject(UserAccessService);
  private languageService = inject(LanguageService);
  private loggerService = inject(LoggerService);
  private lockService = inject(LockService);
  private authenticationService = inject(AuthenticationService);
  private translateService = inject(TranslateService);
  private content = inject(MAT_DIALOG_DATA);

  constructor() {
    if (this.content) {
      this.data = this.content;
      if (!this.data.translations) {
        this.data.translations = [];
      }
      this.workingTranslations.set(this.data.translations.map((t: Translation) => ({ ...t })));
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
    this.data.translations = this.workingTranslations();
    this.dialogRef.close({ data: this.data });
  }

  init() {
    this.languageService.getAll().pipe(
      catchError(error => {
        console.error(error);
        return of([]);
      })
    ).subscribe((response: any) => {
      if (response) {
        this.languages.set(response);
        const currentWorking = this.workingTranslations();
        currentWorking.sort((a: Translation, b: Translation) => a.language.localeCompare(b.language));
        this.dataSource.set(new MatTableDataSource(currentWorking));
        this.current.set(new Translation());
      }
    });
  }

  delete(translation: Translation) {
    if (translation) {
      const currentWorking = this.workingTranslations();
      const filtered = currentWorking.filter((v: Translation) => !(v.key == translation.key && v.language == translation.language));
      this.workingTranslations.set(filtered);
      this.dataSource.set(new MatTableDataSource(filtered));
      this.current.set(new Translation());
    }
  }

  create() {
    const currentTranslation = this.current();
    if (currentTranslation?.language && currentTranslation?.key && currentTranslation?.value) {
      let currentWorking = this.workingTranslations();
      currentWorking = currentWorking?.filter(
        (trans: Translation) =>
          !(trans.language === currentTranslation.language && trans.key === currentTranslation.key)
      ) || [];

      currentWorking.push(currentTranslation);
      currentWorking.sort((a: Translation, b: Translation) => a.language.localeCompare(b.language));

      this.workingTranslations.set(currentWorking);
      this.dataSource.set(new MatTableDataSource(currentWorking));
      this.current.set(new Translation());
    }
  }

  startEdit(element: Translation) {
    if (this.editingItem()) {
      this.cancelEdit();
    }
    this.originalItemBackup = {
      language: element.language,
      key: element.key,
      value: element.value
    };
    this.editingItem.set(element);
  }

  saveEdit(element: Translation) {
    if (!element.language || !element.key || !element.value) {
      this.translateService.get("FIELDS_REQUIRED").subscribe(translation => {
        this.loggerService.warn(translation);
      });
      return;
    }

    const currentWorking = this.workingTranslations();
    const exists = currentWorking.some(t =>
      t !== element && t.language === element.language && t.key === element.key
    );

    if (exists) {
      this.translateService.get("TRANSLATION_ALREADY_EXISTS").subscribe(translation => {
        this.loggerService.warn(translation);
      });
      return;
    }

    const index = currentWorking.findIndex(t => t === element);
    if (index !== -1) {
      currentWorking[index] = element;
      currentWorking.sort((a: Translation, b: Translation) => a.language.localeCompare(b.language));
      this.workingTranslations.set(currentWorking);
      this.dataSource.set(new MatTableDataSource(currentWorking));
    }

    this.editingItem.set(null);
    this.originalItemBackup = null;
  }

  cancelEdit() {
    const editingItemValue = this.editingItem();
    if (editingItemValue && this.originalItemBackup) {
      editingItemValue.language = this.originalItemBackup.language;
      editingItemValue.key = this.originalItemBackup.key;
      editingItemValue.value = this.originalItemBackup.value;
    }
    this.editingItem.set(null);
    this.originalItemBackup = null;
  }

  addNewRow() {
    const newTranslation = new Translation();
    newTranslation.language = '';
    newTranslation.key = '';
    newTranslation.value = '';
    const currentWorking = this.workingTranslations();
    currentWorking.push(newTranslation);
    currentWorking.sort((a: Translation, b: Translation) => a.language.localeCompare(b.language));
    this.workingTranslations.set(currentWorking);
    this.dataSource.set(new MatTableDataSource(currentWorking));
    this.startEdit(newTranslation);
  }

  hasChanges(): boolean {
    if (this.editingItem() !== null) return true;

    const originalTranslations = this.data.translations || [];
    const currentTranslations = this.workingTranslations() || [];

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
    const currentTranslation = this.current();
    return !!(currentTranslation.language && currentTranslation.key && currentTranslation.value);
  }
}