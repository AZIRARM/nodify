import {AfterViewInit, Component, Input, Output, ViewChild} from '@angular/core';
import {MatDialogRef} from '@angular/material/dialog';
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
