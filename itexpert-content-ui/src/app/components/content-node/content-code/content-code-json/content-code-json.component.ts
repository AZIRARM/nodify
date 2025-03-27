import {AfterViewInit, Component, Input, Output, ViewChild} from '@angular/core';
import {ContentNode} from "../../../../modeles/ContentNode";
import {CodemirrorComponent} from "@ctrl/ngx-codemirror";

@Component({
  selector: 'app-content-code-json',
  templateUrl: './content-code-json.component.html',
  styleUrl: './content-code-json.component.css'
})
export class ContentCodeJsonComponent implements AfterViewInit{

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
