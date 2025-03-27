import {AfterViewInit, Component, Input, Output, ViewChild} from '@angular/core';
import {ContentNode} from "../../../../modeles/ContentNode";
import {CodemirrorComponent} from "@ctrl/ngx-codemirror";

@Component({
  selector: 'app-content-code-js',
  templateUrl: './content-code-js.component.html',
  styleUrl: './content-code-js.component.css'
})
export class ContentCodeJsComponent implements AfterViewInit{

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
