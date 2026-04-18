import { AfterViewInit, Component, Inject, ViewChild, inject, signal, WritableSignal } from '@angular/core';
import { CodemirrorComponent } from "@ctrl/ngx-codemirror";
import { Plugin } from "../../../modeles/Plugin";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { TranslateService } from '@ngx-translate/core';
import { LoggerService } from 'src/app/services/LoggerService';

@Component({
  selector: 'app-plugin-dialog',
  templateUrl: './plugin-dialog.component.html',
  styleUrl: './plugin-dialog.component.css',
  standalone: false
})
export class PluginDialogComponent implements AfterViewInit {
  plugin: Plugin;
  isFullscreen: WritableSignal<boolean> = signal(false);

  @ViewChild(CodemirrorComponent) codeMirrorComponent!: CodemirrorComponent;

  private data = inject(MAT_DIALOG_DATA);
  public dialogRef = inject(MatDialogRef<PluginDialogComponent>);
  private translate = inject(TranslateService);
  private loggerService = inject(LoggerService);

  constructor() {
    if (this.data && this.data.plugin) {
      this.plugin = this.data.plugin;
    } else {
      this.plugin = new Plugin();
      this.plugin.editable = true;
    }
  }

  ngAfterViewInit(): void {
    if (!this.plugin.code) {
      this.plugin.code = `(function () {
  function modifyContent(body, regex, replacement) {
    alert('Hello world');
  }

  const body = document.body;
  const regexStr = body.getAttribute("data-regex");
  const replacement = body.getAttribute("data-replacement");

  if (regexStr && replacement) {
    const regex = new RegExp(regexStr, "g");
    modifyContent(body, regex, replacement);
  }
})();`;
    }

    setTimeout(() => {
      if (this.codeMirrorComponent?.codeMirror) {
        this.codeMirrorComponent.codeMirror.refresh();
      }
    }, 300);
  }

  toggleFullscreen(): void {
    this.isFullscreen.set(!this.isFullscreen());

    setTimeout(() => {
      if (this.codeMirrorComponent?.codeMirror) {
        this.codeMirrorComponent.codeMirror.refresh();
      }
    }, 100);
  }

  close(): void {
    this.dialogRef.close({ validate: false });
  }

  validate() {
    if (!this.plugin.name || this.plugin.name.trim().length === 0) {
      this.translate.get("NEED_PLUGIN_NAME").subscribe(trad => {
        this.loggerService.error(trad);
      });
    } else {
      this.dialogRef.close({ validate: true, plugin: this.plugin });
    }
  }
}