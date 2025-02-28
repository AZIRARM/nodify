import {Component, Input, Output} from '@angular/core';
import {ContentNode} from "../../../../modeles/ContentNode";

@Component({
  selector: 'app-content-code-xml',
  templateUrl: './content-code-xml.component.html',
  styleUrl: './content-code-xml.component.css'
})
export class ContentCodeXmlComponent {

  @Output()
  @Input()
  contentNode: ContentNode;
}
