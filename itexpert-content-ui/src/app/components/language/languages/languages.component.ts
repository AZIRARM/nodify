import {Component, OnInit} from '@angular/core';
import {MatTableDataSource} from "@angular/material/table";
import {TranslateService} from "@ngx-translate/core";
import {ToastrService} from "ngx-toastr";
import {LanguageService} from "../../../services/LanguageService";
import {Language} from "../../../modeles/Language";
import {LanguageDialogComponent} from "../language-dialog/language-dialog.component";
import {MatDialog, MatDialogRef} from "@angular/material/dialog";
import {LoggerService} from "../../../services/LoggerService";
import {ValidationDialogComponent} from "../../commons/validation-dialog/validation-dialog.component";
import {UserAccessService} from "../../../services/UserAccessService";

@Component({
    selector: 'app-languages',
    templateUrl: './languages.component.html',
    styleUrls: ['./languages.component.css'],
    standalone: false
})
export class LanguagesComponent implements OnInit {
  displayedColumns: string[] = ['Code', 'Name', 'UrlIcon', 'Description', 'Actions'];
  dataSource: MatTableDataSource<Language> = new MatTableDataSource<Language>([]);

  dialogRef: MatDialogRef<LanguageDialogComponent>;
  dialogValidationRef: MatDialogRef<ValidationDialogComponent>;
  user: any;

  constructor(
    private translate: TranslateService,
    private toast: ToastrService,
    private languageService: LanguageService,
    public userAccessService: UserAccessService,
    private loggerService: LoggerService,
    private dialog: MatDialog
  ) {}

  ngOnInit() {
    this.user = this.userAccessService.getCurrentUser();
    this.init();
  }

  init() {
    this.languageService.getAll().subscribe(
      (response: any) => {
        if (response && Array.isArray(response)) {
          response = response.sort((a: any, b: any) => a.code.localeCompare(b.code));
          this.dataSource.data = response;
        } else if (response && response.data && Array.isArray(response.data)) {
          response.data.sort((a: any, b: any) => a.code.localeCompare(b.code));
          this.dataSource.data = response.data;
        }
      },
      (error) => {
        console.error('Erreur chargement langues', error);
        this.toast.error(this.translate.instant('LOAD_ERROR'));
      }
    );
  }

  create() {
    this.dialogRef = this.dialog.open(LanguageDialogComponent, {
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    this.dialogRef.afterClosed().subscribe(result => {
      if (result && result.data) {
        let language: Language = result.data;
        this.save(language);
      }
    });
  }

  update(language: Language) {
    this.dialogRef = this.dialog.open(LanguageDialogComponent, {
      data: language,
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    this.dialogRef.afterClosed().subscribe(result => {
      if (result && result.data) {
        language = result.data;
        this.save(language);
      }
    });
  }

  save(language: Language) {
    this.languageService.save(language).subscribe(
      response => {
        this.translate.get("SAVE_SUCCESS").subscribe(trad => {
          this.loggerService.success(trad);
          this.init();
        });
      },
      error => {
        this.translate.get("SAVE_ERROR").subscribe(trad => {
          this.loggerService.error(trad);
        });
      }
    );
  }

  delete(language: Language) {
    this.dialogValidationRef = this.dialog.open(ValidationDialogComponent, {
      data: {
        title: "DELETE_TITLE",
        message: "DELETE_MESSAGE"
      },
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    this.dialogValidationRef.afterClosed().subscribe((result: any) => {
      if (result && result.data && result.data === "validated") {
        this.languageService.delete(language.id).subscribe(
          response => {
            this.translate.get("DELETE_SUCCESS").subscribe(trad => {
              this.loggerService.success(trad);
              this.init();
            });
          },
          error => {
            this.translate.get("DELETE_ERROR").subscribe(trad => {
              this.loggerService.error(trad);
            });
          }
        );
      }
    });
  }
}
