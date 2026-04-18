import {Component, Inject, OnInit} from '@angular/core';
import {User} from "../../../modeles/User";
import {MAT_DIALOG_DATA, MatDialog, MatDialogRef} from "@angular/material/dialog";
import {ValidationDialogComponent} from "../../commons/validation-dialog/validation-dialog.component";
import {MatTableDataSource} from "@angular/material/table";
import {Plugin} from "../../../modeles/Plugin";
import {TranslateService} from "@ngx-translate/core";
import {ToastrService} from "ngx-toastr";
import {LoggerService} from "../../../services/LoggerService";
import {UserAccessService} from "../../../services/UserAccessService";
import {PluginFile} from "../../../modeles/PluginFile";
import {PluginFileService} from "../../../services/PluginFileService";

@Component({
    selector: 'app-plugin-files-dialog',
    templateUrl: './plugin-files-dialog.component.html',
    styleUrl: './plugin-files-dialog.component.css',
    standalone: false
})
export class PluginFilesDialogComponent implements OnInit {
  user: User;
  plugin: Plugin;
  displayedColumns: string[] = ['PluginName', 'FileName', 'Description', 'Actions'];
  dataSource: MatTableDataSource<PluginFile> = new MatTableDataSource<PluginFile>([]);
  dialogRefValidation: MatDialogRef<ValidationDialogComponent>;
  currentPluginFile: PluginFile;
  selectedFileName: string = '';

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: any,
    public dialogRef: MatDialogRef<PluginFilesDialogComponent>,
    private translate: TranslateService,
    private toast: ToastrService,
    private loggerService: LoggerService,
    public userAccessService: UserAccessService,
    public pluginFileService: PluginFileService,
    private dialog: MatDialog
  ) {
    if (data) {
      this.plugin = data;
    }
  }

  ngOnInit() {
    this.user = this.userAccessService.getCurrentUser();
    this.resetCurrentPluginFile();
    this.init();
  }

  resetCurrentPluginFile() {
    this.currentPluginFile = new PluginFile();
    this.currentPluginFile.pluginId = this.plugin?.id;
    this.currentPluginFile.isEditable = true;
    this.selectedFileName = '';
  }

  init() {
    if (!this.plugin?.id) return;

    this.pluginFileService.getPluginAssets(this.plugin.id).subscribe(
      (response: any) => {
        this.dataSource.data = response || [];
      },
      (error) => {
        console.error('Erreur chargement assets', error);
        this.toast.error('Request failed with error');
      });
  }

  canSaveAsset(): boolean {
    if (this.currentPluginFile?.id) {
      // En mode édition, seul la description peut être modifiée
      return true;
    }
    // En mode création, il faut un nom de fichier et des données
    return !!(this.currentPluginFile?.fileName && this.currentPluginFile?.data);
  }

  delete(pluginFile: PluginFile) {
    this.dialogRefValidation = this.dialog.open(ValidationDialogComponent, {
      data: {
        title: "DELETE_PlUGIN_FILE_TITLE",
        message: "DELETE_PlUGIN_FILE_MESSAGE"
      },
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    this.dialogRefValidation.afterClosed()
      .subscribe(result => {
        if (result && result.data !== 'canceled') {
          this.pluginFileService.delete(pluginFile.id).subscribe(
            response => {
              this.translate.get("DELETE_SUCCESS").subscribe(trad => {
                this.loggerService.success(trad);
                this.init();
              });
            },
            error => {
              this.translate.get("DELETE_ERROR").subscribe(trad => {
                this.loggerService.error(trad);
              });
            });
        }
      });
  }

  save() {
    this.pluginFileService.save(this.currentPluginFile).subscribe(
      (response: any) => {
        this.translate.get("SAVE_SUCCESS").subscribe(trad => {
          this.loggerService.success(trad);
          this.resetCurrentPluginFile();
          this.init();
        });
      },
      error => {
        this.translate.get("SAVE_ERROR").subscribe(trad => {
          this.loggerService.error(trad);
        });
      });
  }

  close(): void {
    this.dialogRef.close({validate: false});
  }

  validate() {
    this.dialogRef.close({validate: true, plugin: this.plugin});
  }

  onFileChange(event: any) {
    const file = event.target.files[0];
    if (!file) return;

    const reader = new FileReader();

    reader.onload = () => {
      const base64Content: string = reader.result as string;
      this.currentPluginFile.fileName = file.name;
      this.currentPluginFile.data = base64Content;
      this.selectedFileName = file.name;
    };

    reader.readAsDataURL(file);
  }

  edit(element: PluginFile) {
    // Créer une copie pour l'édition
    this.currentPluginFile = {...element};
    this.currentPluginFile.isEditable = true;
    this.selectedFileName = element.fileName;
  }
}
