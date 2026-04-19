import { Component, OnInit, inject, signal } from '@angular/core';
import { MatTableDataSource } from "@angular/material/table";
import { TranslateService } from "@ngx-translate/core";
import { ToastrService } from "ngx-toastr";
import { LanguageService } from "../../../services/LanguageService";
import { Language } from "../../../modeles/Language";
import { LanguageDialogComponent } from "../language-dialog/language-dialog.component";
import { MatDialog } from "@angular/material/dialog";
import { LoggerService } from "../../../services/LoggerService";
import { ValidationDialogComponent } from "../../commons/validation-dialog/validation-dialog.component";
import { UserAccessService } from "../../../services/UserAccessService";

@Component({
  selector: 'app-languages',
  templateUrl: './languages.component.html',
  styleUrls: ['./languages.component.css'],
  standalone: false
})
export class LanguagesComponent implements OnInit {
  private translate = inject(TranslateService);
  private toast = inject(ToastrService);
  private languageService = inject(LanguageService);
  public userAccessService = inject(UserAccessService);
  private loggerService = inject(LoggerService);
  private dialog = inject(MatDialog);

  displayedColumns: string[] = ['Code', 'Name', 'UrlIcon', 'Description', 'Actions'];
  dataSource = new MatTableDataSource<Language>([]);

  user = signal<any>(null);

  ngOnInit() {
    this.user.set(this.userAccessService.getCurrentUser());
    this.init();
  }

  init() {
    this.languageService.getAll().subscribe({
      next: (response: any) => {
        let data: Language[] = [];
        if (response && Array.isArray(response)) {
          data = response.sort((a: any, b: any) => a.code.localeCompare(b.code));
        } else if (response?.data && Array.isArray(response.data)) {
          data = response.data.sort((a: any, b: any) => a.code.localeCompare(b.code));
        }
        this.dataSource.data = data;
      },
      error: (error) => {
        console.error('Erreur chargement langues', error);
        this.toast.error(this.translate.instant('LOAD_ERROR'));
      }
    });
  }

  create() {
    const dialogRef = this.dialog.open(LanguageDialogComponent, {
      height: '80vh',
      width: '80vw',
      disableClose: true
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result?.data) {
        this.save(result.data);
      }
    });
  }

  update(language: Language) {
    const dialogRef = this.dialog.open(LanguageDialogComponent, {
      data: language,
      height: '80vh',
      width: '80vw',
      disableClose: true
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result?.data) {
        this.save(result.data);
      }
    });
  }

  save(language: Language) {
    this.languageService.save(language).subscribe({
      next: () => {
        this.translate.get("SAVE_SUCCESS").subscribe(trad => {
          this.loggerService.success(trad);
          this.init();
        });
      },
      error: () => {
        this.translate.get("SAVE_ERROR").subscribe(trad => {
          this.loggerService.error(trad);
        });
      }
    });
  }

  delete(language: Language) {
    const dialogValidationRef = this.dialog.open(ValidationDialogComponent, {
      data: {
        title: "DELETE_TITLE",
        message: "DELETE_MESSAGE"
      },
      height: '80vh',
      width: '80vw',
      disableClose: true
    });

    dialogValidationRef.afterClosed().subscribe((result: any) => {
      if (result?.data === "validated") {
        this.languageService.delete(language.id).subscribe({
          next: () => {
            this.translate.get("DELETE_SUCCESS").subscribe(trad => {
              this.loggerService.success(trad);
              this.init();
            });
          },
          error: () => {
            this.translate.get("DELETE_ERROR").subscribe(trad => {
              this.loggerService.error(trad);
            });
          }
        });
      }
    });
  }
}