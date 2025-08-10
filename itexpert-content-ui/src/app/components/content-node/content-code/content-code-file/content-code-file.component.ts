import {Component, EventEmitter, Input, Output} from '@angular/core';
import {ContentNode} from "../../../../modeles/ContentNode";

@Component({
  selector: 'app-content-code-file',
  templateUrl: './content-code-file.component.html',
  styleUrl: './content-code-file.component.css'
})
export class ContentCodeFileComponent {

  @Output()
  @Input()
  contentNode: ContentNode;


  @Output() onFileChange = new EventEmitter<void>();

  onFileChangeFactory($event: any): void {
    this.onFileChange.next($event);
  }
}
