import { Component, inject, signal, computed } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { Language } from "../../../modeles/Language";

@Component({
  selector: 'app-language-dialog',
  templateUrl: './language-dialog.component.html',
  styleUrls: ['./language-dialog.component.css'],
  standalone: false
})
export class LanguageDialogComponent {

  private dialogRef = inject(MatDialogRef<LanguageDialogComponent>);
  private content = inject<Language | null>(MAT_DIALOG_DATA);

  language = signal<Language>(this.content ? { ...this.content } : new Language());

  isFormValid = computed(() =>
    !!(this.language().code && this.language().name)
  );

  updateCode(code: string) {
    this.language.update(l => ({ ...l, code }));
  }

  updateName(name: string) {
    this.language.update(l => ({ ...l, name }));
  }

  updateDescription(description: string) {
    this.language.update(l => ({ ...l, description }));
  }

  updateUrlIcon(urlIcon: string) {
    this.language.update(l => ({ ...l, urlIcon }));
  }

  cancel() {
    this.dialogRef.close();
  }

  validate() {
    if (this.isFormValid()) {
      this.dialogRef.close({ data: this.language() });
    }
  }
}