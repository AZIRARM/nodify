import {Component, EventEmitter, Input, Output} from '@angular/core';
import {ContentNode} from "../../../../modeles/ContentNode";
import {ContentFile} from "../../../../modeles/ContentFile";

@Component({
  selector: 'app-content-code-picture',
  templateUrl: './content-code-picture.component.html',
  styleUrl: './content-code-picture.component.css'
})
export class ContentCodePictureComponent {

  @Output()
  @Input()
  currentContent: ContentNode;


  @Output() onFileChange = new EventEmitter<void>();
  @Output() close = new EventEmitter<void>();
  @Output() validate = new EventEmitter<void>();

  closeFactory(): void {
    this.close.next();
  }

  validateFactory(): void {
    this.validate.next();
  }

  onFileChangeFactory($event:any): void {
    this.onFileChange.next($event);
  }
}
