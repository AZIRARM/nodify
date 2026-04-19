import { Component, EventEmitter, Input, Output } from '@angular/core';
import {MatDialogRef} from '@angular/material/dialog';
import { ContentNode } from '../../../../modeles/ContentNode';

@Component({
    selector: 'app-content-code-file',
    templateUrl: './content-code-file.component.html',
    styleUrl: './content-code-file.component.css',
    standalone: false
})
export class ContentCodeFileComponent {

  @Input() contentNode: ContentNode;

  @Input() dialogRef: MatDialogRef<any>;

  @Output() onFileChange = new EventEmitter<any>();

  onFileChangeFactory($event: any): void {
    this.onFileChange.emit($event);
  }

}
