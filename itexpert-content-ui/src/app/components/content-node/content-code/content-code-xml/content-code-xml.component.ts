import {AfterViewInit, Component, Input, Output, ViewChild} from '@angular/core';
import {ContentNode} from "../../../../modeles/ContentNode";
import {CodemirrorComponent} from "@ctrl/ngx-codemirror";

@Component({
  selector: 'app-content-code-xml',
  templateUrl: './content-code-xml.component.html',
  styleUrl: './content-code-xml.component.css'
})
export class ContentCodeXmlComponent implements AfterViewInit{

  @Output()
  @Input()
  contentNode: ContentNode;

  @ViewChild(CodemirrorComponent) codeMirrorComponent!: CodemirrorComponent;

  ngAfterViewInit(): void {
    setTimeout(() => {
      if (this.codeMirrorComponent?.codeMirror) {
        this.codeMirrorComponent.codeMirror.refresh();
      }
    }, 300);
  }
}
