import {Component, Input} from '@angular/core';
import {MatDialogRef} from '@angular/material/dialog';

@Component({
    selector: 'app-content-code-title',
    templateUrl: './content-code-title.component.html',
    styleUrl: './content-code-title.component.css',
    standalone: false
})
export class ContentCodeTitleComponent {
  @Input()
  title: string;

  @Input()
  dialogRef: MatDialogRef<any>;

  constructor() {
  }

  closeDialog(): void {
    if (this.dialogRef) {
      this.dialogRef.close();
    }
  }
}
