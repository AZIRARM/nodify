import {Component, EventEmitter, Input, Output} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {ContentNode} from "../../../../modeles/ContentNode";

@Component({
  selector: 'app-content-code-actions',
  templateUrl: './content-code-actions.component.html',
  styleUrl: './content-code-actions.component.css'
})
export class ContentCodeActionsComponent {
  @Output()
  @Input()
  contentNode: ContentNode;

  constructor(private translate: TranslateService) {
  }

  @Output() close = new EventEmitter<void>();
  @Output() validate = new EventEmitter<void>();

  closeFactory(): void {
    this.close.next();
  }

  validateFactory(): void {
    this.validate.next();
  }
}
