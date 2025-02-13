import {Component, EventEmitter, Input, Output} from '@angular/core';
import {ContentNode} from "../../../../modeles/ContentNode";

@Component({
  selector: 'app-content-code-css',
  templateUrl: './content-code-css.component.html',
  styleUrl: './content-code-css.component.css'
})
export class ContentCodeCssComponent {
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
