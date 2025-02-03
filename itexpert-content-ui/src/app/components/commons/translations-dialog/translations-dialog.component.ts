import {Component, Inject} from '@angular/core';
import {MatTableDataSource} from "@angular/material/table";
import {MAT_DIALOG_DATA, MatDialog, MatDialogRef} from "@angular/material/dialog";
import {TranslateService} from "@ngx-translate/core";
import {ToastrService} from "ngx-toastr";
import {ActivatedRoute, Router} from "@angular/router";
import {NodeService} from "../../../services/NodeService";
import {LoggerService} from "../../../services/LoggerService";
import {LanguageService} from "../../../services/LanguageService";
import {AccessRoleService} from "../../../services/AccessRoleService";
import {Language} from "../../../modeles/Language";
import {Translation} from "../../../modeles/Translation";
import {Value} from "../../../modeles/Value";

@Component({
  selector: 'app-translations-dialog',
  templateUrl: './translations-dialog.component.html',
  styleUrls: ['./translations-dialog.component.css']
})
export class TranslationsDialogComponent {
  data: any;

  current: any;

  languages: Language[] = [];

  displayedColumns: string[] = ['Language', 'Key', 'Value', 'Actions'];

  dataSource: MatTableDataSource<Translation>;

  constructor(
    public dialogRef: MatDialogRef<TranslationsDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public content: any,
    private translate: TranslateService,
    private toast: ToastrService,
    private route: ActivatedRoute,
    private nodeService: NodeService,
    private loggerService: LoggerService,
    private languageService: LanguageService,
    private accessRoleService: AccessRoleService,
    private router: Router,
    private dialog: MatDialog
  ) {
    if (content) {
      this.data = content;
      if (!this.data.translations) {
        this.data.translations = [];
      }
    }
  }

  ngOnInit(): void {
    this.init();
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
