import {Component, Input, Output} from '@angular/core';
import {ContentNode} from "../../../../modeles/ContentNode";

@Component({
  selector: 'app-content-code-css',
  templateUrl: './content-code-css.component.html',
  styleUrl: './content-code-css.component.css'
})
export class ContentCodeCssComponent {
  @Output()
  @Input()
  contentNode: ContentNode;
}
