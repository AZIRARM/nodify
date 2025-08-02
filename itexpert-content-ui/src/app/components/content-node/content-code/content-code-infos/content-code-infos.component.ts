import {Component, Input, Output} from '@angular/core';
import {ContentNode} from "../../../../modeles/ContentNode";

@Component({
  selector: 'app-content-code-infos',
  templateUrl: './content-code-infos.component.html',
  styleUrl: './content-code-infos.component.css'
})
export class ContentCodeInfosComponent {
  @Input()
  contentNode: ContentNode;
}
