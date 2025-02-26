import {Component, Input, Output} from '@angular/core';
import {ContentNode} from "../../../../modeles/ContentNode";

@Component({
  selector: 'app-content-code-js',
  templateUrl: './content-code-js.component.html',
  styleUrl: './content-code-js.component.css'
})
export class ContentCodeJsComponent {

  @Output()
  @Input()
  contentNode: ContentNode;
}
