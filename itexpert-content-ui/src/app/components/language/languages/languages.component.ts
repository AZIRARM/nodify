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
  styleUrls: ['./languages.component.css']
})
export class LanguagesComponent implements OnInit {
  displayedColumns: string[] = ['Code', 'Name', 'UrlIcon', 'Description', "Actions"];
  dataSource: MatTableDataSource<Language>;

  dialogRef: MatDialogRef<LanguageDialogComponent>;

  user: any;

  dialogValidationRef: MatDialogRef<ValidationDialogComponent>;

  constructor(private translate: TranslateService,
              private toast: ToastrService,
              private languageService: LanguageService,
              public userAccessService: UserAccessService,
              private loggerService: LoggerService,
              private dialog: MatDialog) {
  }


  ngOnInit() {
   this.user = this.userAccessService.getCurrentUser()
   this.init();
  }

  init() {
    this.languageService.getAll().subscribe(
      (response: any) => {                           //next() callback
        response=response.sort((a:any, b:any) => a.code.localeCompare(b.code));
        this.dataSource = new MatTableDataSource(response);
      },
      (error) => {                              //error() callback
        this.toast.error('Request failed with error');
      });

  }

  create() {
    this.dialogRef = this.dialog.open(LanguageDialogComponent, {
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    this.dialogRef.afterClosed()
      .subscribe(result => {
        let language: Language = result.data;
        this.save(language);
      });
  }

  update(language: Language) {
    this.dialogRef = this.dialog.open(LanguageDialogComponent,
      {
        data: language,
        height: '80vh',
        width: '80vw',
        disableClose: true
      });
    this.dialogRef.afterClosed()
      .subscribe(result => {
        language = result.data;
        this.save(language);
      });

  }

  save(language: Language) {
    this.languageService.save(language).subscribe(
      response => {
        this.translate.get("SAVE_SUCCESS").subscribe(trad => {
          this.loggerService.success(trad);
          this.dialogRef.close();
          this.init();
        })

      },
      error => {
        this.translate.get("SAVE_ERROR").subscribe(trad1 => {
          this.translate.get("CHANGE_PROJECT_CODE_MESSAGE").subscribe(trad2 => {
            this.loggerService.error(trad1 + ",  " + trad2);
          })
        });
      });
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
    this.dialogValidationRef.afterClosed()
      .subscribe((result: any) => {
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
              })
            });

        }
      });
  }
}
