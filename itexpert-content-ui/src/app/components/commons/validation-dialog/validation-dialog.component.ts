import { Component, Inject, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";

@Component({
  selector: 'app-validation-dialog',
  templateUrl: './validation-dialog.component.html',
  styleUrls: ['./validation-dialog.component.css'],
  standalone: false
})
export class ValidationDialogComponent {
  message: string;
  title: string;
  isHtml: boolean = false;

  private dialogRef = inject(MatDialogRef<ValidationDialogComponent>);
  private content = inject(MAT_DIALOG_DATA);

  constructor() {
    this.message = this.content.message;
    this.title = this.content.title;
    this.isHtml = this.content.title ? this.content.title : false;
    console.log(this.message);
  }

  cancel() {
    this.dialogRef.close({ data: 'canceled' });
  }

  validate() {
    this.dialogRef.close({ data: 'validated' });
  }
}