import {Component, EventEmitter, Input, Output} from '@angular/core';
import {ContentNode} from "../../../../modeles/ContentNode";

@Component({
  selector: 'app-content-code-picture',
  templateUrl: './content-code-picture.component.html',
  styleUrl: './content-code-picture.component.css'
})
export class ContentCodePictureComponent {

  @Output()
  @Input()
  contentNode: ContentNode;


  @Output() onFileChange = new EventEmitter<void>();
  @Output() close = new EventEmitter<void>();

  onFileChangeFactory($event: any): void {
    this.onFileChange.next($event);
  }
}
