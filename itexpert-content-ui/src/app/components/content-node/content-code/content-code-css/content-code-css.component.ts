import { Component, Input, ViewChild } from '@angular/core';
import { CodemirrorComponent } from '@ctrl/ngx-codemirror';
import {MatDialogRef} from '@angular/material/dialog';
import { ContentNode } from '../../../../modeles/ContentNode';

@Component({
  selector: 'app-content-code-css',
  templateUrl: './content-code-css.component.html',
  styleUrls: ['./content-code-css.component.css']
})
export class ContentCodeCssComponent {

  @Input() contentNode: ContentNode;
  @ViewChild(CodemirrorComponent) codeMirrorComponent: CodemirrorComponent;
  @Input() dialogRef: MatDialogRef<any>;

  isFullscreen: boolean = false;

  toggleFullscreen() {
    this.isFullscreen = !this.isFullscreen;

    setTimeout(() => {
      if (this.codeMirrorComponent?.codeMirror) {
        this.codeMirrorComponent.codeMirror.refresh();
      }
    }, 50);
  }
}
