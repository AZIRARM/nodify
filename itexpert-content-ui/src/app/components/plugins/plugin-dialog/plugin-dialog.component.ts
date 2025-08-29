import {AfterViewInit, Component, Inject, ViewChild} from '@angular/core';
import {CodemirrorComponent} from "@ctrl/ngx-codemirror";
import {Plugin} from "../../../modeles/Plugin";
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import { TranslateService } from '@ngx-translate/core';
import { ToastrService } from 'ngx-toastr';
import { LoggerService } from 'src/app/services/LoggerService';

@Component({
  selector: 'app-plugin-dialog',
  templateUrl: './plugin-dialog.component.html',
  styleUrl: './plugin-dialog.component.css'
})
export class PluginDialogComponent implements AfterViewInit {
  plugin: Plugin;

  @ViewChild(CodemirrorComponent) codeMirrorComponent!: CodemirrorComponent;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: any,
    public dialogRef: MatDialogRef<PluginDialogComponent>,
    private translate: TranslateService,
    private loggerService: LoggerService
  ) {
    if (data && data.plugin) {
      this.plugin = data.plugin;
    }
  }

  ngAfterViewInit(): void {
    if (!this.plugin ) {
      this.plugin = new Plugin();
      this.plugin.editable = true;
    }
    if(!this.plugin.code){
      this.plugin.code = "(function () { \n" +
        "function modifyContent(body, regex, replacement) {\n" +
        "        alert('Hello world');\n" +
        "}\n\n" +
        "     const body = document.body;\n" +
        "     const regexStr = body.getAttribute(\"data-regex\");\n" +
        "     const replacement = body.getAttribute(\"data-replacement\");\n" +
        "\n" +
        "if (regexStr && replacement) {\n" +
        "     const regex = new RegExp(regexStr, \"g\");\n" +
        "     modifyContent(body, regex, replacement);\n" +
        "   }\n" +
        "})();";
    }

    setTimeout(() => {
      if (this.codeMirrorComponent?.codeMirror) {
        this.codeMirrorComponent.codeMirror.refresh();
      }
    }, 300);
  }


  close(): void {
    this.dialogRef.close({validate: false});
  }

  validate() {
    if(!this.plugin.name || this.plugin.name.trim().length <= 0) {
      this.translate.get("NEED_PLUGIN_NAME").subscribe(trad => {
                this.loggerService.error(trad);
              })
    } else{
      this.dialogRef.close({validate: true, plugin: this.plugin});
    }
  }
}
