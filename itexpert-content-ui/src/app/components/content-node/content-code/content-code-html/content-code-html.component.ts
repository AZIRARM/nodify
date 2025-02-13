import {Component, EventEmitter, Input, Output} from '@angular/core';
import {ContentNode} from "../../../../modeles/ContentNode";

@Component({
  selector: 'app-content-code-html',
  templateUrl: './content-code-html.component.html',
  styleUrl: './content-code-html.component.css'
})
export class ContentCodeHtmlComponent {

  @Output()
  @Input()
  currentContent!: ContentNode;

  @Output() close = new EventEmitter<void>();
  @Output() validate = new EventEmitter<void>();
  code: boolean = true;

  closeFactory(): void {
    this.close.next();
  }

  validateFactory(): void {
    this.validate.next();
  }

  setCodeEdition(codeEdition: boolean) {
    this.code = codeEdition;
  }
}
