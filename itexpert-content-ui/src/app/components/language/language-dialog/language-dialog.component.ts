import {Component, Inject} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {Language} from "../../../modeles/Language";

@Component({
  selector: 'app-language-dialog',
  templateUrl: './language-dialog.component.html',
  styleUrls: ['./language-dialog.component.css']
})
export class LanguageDialogComponent {

  language: Language = new Language();

  constructor(
    public dialogRef: MatDialogRef<LanguageDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public content: Language
  ) {
    if (content) {
      this.language = content;
    }
  }

  cancel() {
    this.dialogRef.close();
  }

  validate() {
    this.dialogRef.close({data: this.language});
  }

}
