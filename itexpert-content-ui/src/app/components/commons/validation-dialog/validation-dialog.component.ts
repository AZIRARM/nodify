import {Component, Inject} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";

@Component({
  selector: 'app-validation-dialog',
  templateUrl: './validation-dialog.component.html',
  styleUrls: ['./validation-dialog.component.css']
})
export class ValidationDialogComponent {
  message: string;
  title: string;

  constructor(
    public dialogRef: MatDialogRef<ValidationDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public content: any,
  ) {
    this.message = content.message;
    this.title = content.title;
    console.log(this.message);
  }

  cancel() {
    this.dialogRef.close({data: 'canceled'});
  }

  validate() {
    this.dialogRef.close({data: 'validated'});
  }

}
