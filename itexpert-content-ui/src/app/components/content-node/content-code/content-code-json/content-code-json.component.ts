import {Component, EventEmitter, Input, Output} from '@angular/core';
import {ContentNode} from "../../../../modeles/ContentNode";

@Component({
  selector: 'app-content-code-json',
  templateUrl: './content-code-json.component.html',
  styleUrl: './content-code-json.component.css'
})
export class ContentCodeJsonComponent {

  @Output()
  @Input()
  currentContent: ContentNode;


  @Output() close = new EventEmitter<void>();
  @Output() validate = new EventEmitter<void>();


  closeFactory(): void {
    this.close.next();
  }

  validateFactory(): void {
    this.validate.next();
  }
}
