import {Component, EventEmitter, Input, Output} from '@angular/core';
import {ContentNode} from "../../../../modeles/ContentNode";

@Component({
  selector: 'app-content-code-js',
  templateUrl: './content-code-js.component.html',
  styleUrl: './content-code-js.component.css'
})
export class ContentCodeJsComponent {

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
