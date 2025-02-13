import {Component, EventEmitter, Input, Output} from '@angular/core';
import {ContentNode} from "../../../../modeles/ContentNode";
import {ContentFile} from "../../../../modeles/ContentFile";

@Component({
  selector: 'app-content-code-file',
  templateUrl: './content-code-file.component.html',
  styleUrl: './content-code-file.component.css'
})
export class ContentCodeFileComponent {

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
