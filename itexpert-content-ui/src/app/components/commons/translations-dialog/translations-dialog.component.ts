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

@Component({
  selector: 'app-translations-dialog',
  templateUrl: './translations-dialog.component.html',
  styleUrls: ['./translations-dialog.component.css']
})
export class TranslationsDialogComponent implements  OnInit, OnDestroy {
  data: any;

  current: any;

  languages: Language[] = [];

  displayedColumns: string[] = ['Language', 'Key', 'Value', 'Actions'];

  dataSource: MatTableDataSource<Translation>;

  constructor(
    public dialogRef: MatDialogRef<TranslationsDialogComponent>,
    public userAccessService: UserAccessService,
    @Inject(MAT_DIALOG_DATA) public content: any,
    private languageService: LanguageService,
    private loggerService: LoggerService,
    private lockService: LockService,
    private translateService: TranslateService,
  ) {
    if (content) {
      this.data = content;
      if (!this.data.translations) {
        this.data.translations = [];
      }
    }
  }


  ngOnInit() {
    this.init();
    
    // 🔒 Tente d’acquérir le lock en entrant dans l’édition
    this.lockService.acquire(this.data.code).subscribe(acquired => {
      if (!acquired) {       
         this.translateService.get("RESOURCE_LOCKED")
            .subscribe(translation => {
              this.loggerService.error(translation);
            });
        this.dialogRef.close();
      } else {
        // Si acquis → démarre la surveillance d’inactivité à 30 min
        this.lockService.startInactivityWatcher(30 * 60 * 1000, () => {          
         this.translateService.get("RESOURCE_RELEASED")
            .subscribe(translation => {
              this.loggerService.warn(translation);
            });
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
    this.dialogRef.close({data: this.data});
  }


  init() {
    this.languageService.getAll().subscribe(
      (response: any) => {
        if (response) {
          this.languages = response;
          this.data.translations.sort((a: Translation, b: Translation) => a.language.localeCompare(b.language));
          this.dataSource = new MatTableDataSource(this.data.translations);
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
      this.data.translations = this.data.translations
        .filter((v: Translation) => !(v.key == translation.key && v.language == translation.language));
      this.dataSource = new MatTableDataSource(this.data.translations);
      this.current = new Translation();
    }
  }

  create() {
    if (this.current?.language && this.current?.key && this.current?.value) {
      this.data.translations = this.data.translations?.filter(
        (trans: Translation) =>
          !(trans.language === this.current.language && trans.key === this.current.key)
      ) || [];

      this.data.translations.push(this.current);
      this.data.translations.sort((a: Translation, b: Translation) => a.language.localeCompare(b.language));

      this.init();
    }

  }

  update(element: any) {
    this.current = element;
  }
}
