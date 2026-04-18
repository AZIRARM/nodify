import {AfterViewInit, Component, Input, Output, ViewChild} from '@angular/core';
import {MatDialogRef} from '@angular/material/dialog';
import {ContentNode} from "../../../../modeles/ContentNode";
import {CodemirrorComponent} from "@ctrl/ngx-codemirror";

@Component({
    selector: 'app-content-code-xml',
    templateUrl: './content-code-xml.component.html',
    styleUrl: './content-code-xml.component.css',
    standalone: false
})
export class ContentCodeXmlComponent implements AfterViewInit{

  @Output()
  @Input()
  contentNode: ContentNode;

  @Input() dialogRef: MatDialogRef<any>;

  @ViewChild(CodemirrorComponent) codeMirrorComponent!: CodemirrorComponent;

  isFullscreen: boolean = false;

  ngAfterViewInit(): void {
    setTimeout(() => {
      if (this.codeMirrorComponent?.codeMirror) {
        this.codeMirrorComponent.codeMirror.refresh();
      }
    }, 300);
  }

  toggleFullscreen() {
    this.isFullscreen = !this.isFullscreen;

    setTimeout(() => {
      if (this.codeMirrorComponent?.codeMirror) {
        this.codeMirrorComponent.codeMirror.refresh();
      }
    }, 50);
  }
}
