import {Component, EventEmitter, Input, Output} from '@angular/core';
import {MatDialogRef} from '@angular/material/dialog';
import {ContentNode} from "../../../../modeles/ContentNode";

@Component({
    selector: 'app-content-code-picture',
    templateUrl: './content-code-picture.component.html',
    styleUrl: './content-code-picture.component.css',
    standalone: false
})
export class ContentCodePictureComponent {

  @Output()
  @Input()
  contentNode: ContentNode;

  @Input() dialogRef: MatDialogRef<any>;


  @Output() onFileChange = new EventEmitter<void>();
  @Output() close = new EventEmitter<void>();

  onFileChangeFactory($event: any): void {
    this.onFileChange.next($event);
  }
}
