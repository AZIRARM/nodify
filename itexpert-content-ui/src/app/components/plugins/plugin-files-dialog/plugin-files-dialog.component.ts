import { Component, Inject, OnInit, inject, signal, WritableSignal } from '@angular/core';
import { User } from "../../../modeles/User";
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from "@angular/material/dialog";
import { ValidationDialogComponent } from "../../commons/validation-dialog/validation-dialog.component";
import { MatTableDataSource } from "@angular/material/table";
import { Plugin } from "../../../modeles/Plugin";
import { TranslateService } from "@ngx-translate/core";
import { ToastrService } from "ngx-toastr";
import { LoggerService } from "../../../services/LoggerService";
import { UserAccessService } from "../../../services/UserAccessService";
import { PluginFile } from "../../../modeles/PluginFile";
import { PluginFileService } from "../../../services/PluginFileService";
import { of } from "rxjs";
import { catchError, finalize, switchMap } from "rxjs/operators";

@Component({
  selector: 'app-plugin-files-dialog',
  templateUrl: './plugin-files-dialog.component.html',
  styleUrl: './plugin-files-dialog.component.css',
  standalone: false
})
export class PluginFilesDialogComponent implements OnInit {
  user: WritableSignal<User> = signal<User>({} as User);
  plugin: Plugin;
  displayedColumns: string[] = ['PluginName', 'FileName', 'Description', 'Actions'];
  dataSource: WritableSignal<MatTableDataSource<PluginFile>> = signal(new MatTableDataSource<PluginFile>([]));
  dialogRefValidation: MatDialogRef<ValidationDialogComponent>;
  currentPluginFile: WritableSignal<PluginFile> = signal<PluginFile>(new PluginFile());
  selectedFileName: WritableSignal<string> = signal<string>('');
  isLoading: WritableSignal<boolean> = signal(false);

  private data = inject(MAT_DIALOG_DATA);
  public dialogRef = inject(MatDialogRef<PluginFilesDialogComponent>);
  private translate = inject(TranslateService);
  private toast = inject(ToastrService);
  private loggerService = inject(LoggerService);
  public userAccessService = inject(UserAccessService);
  public pluginFileService = inject(PluginFileService);
  private dialog = inject(MatDialog);

  constructor() {
    if (this.data) {
      this.plugin = this.data;
    }
  }

  ngOnInit() {
    this.user.set(this.userAccessService.getCurrentUser());
    this.resetCurrentPluginFile();
    this.init();
  }

  resetCurrentPluginFile() {
    const newPluginFile = new PluginFile();
    newPluginFile.pluginId = this.plugin?.id;
    newPluginFile.isEditable = true;
    this.currentPluginFile.set(newPluginFile);
    this.selectedFileName.set('');
  }

  init() {
    if (!this.plugin?.id) return;

    this.isLoading.set(true);
    this.pluginFileService.getPluginAssets(this.plugin.id).pipe(
      catchError((error) => {
        console.error('Erreur chargement assets', error);
        this.toast.error('Request failed with error');
        return of([]);
      }),
      finalize(() => this.isLoading.set(false))
    ).subscribe((response: any) => {
      this.dataSource.set(new MatTableDataSource(response || []));
    });
  }

  canSaveAsset(): boolean {
    const current = this.currentPluginFile();
    if (current?.id) {
      return true;
    }
    return !!(current?.fileName && current?.data);
  }

  delete(pluginFile: PluginFile) {
    this.isLoading.set(true);
    this.dialogRefValidation = this.dialog.open(ValidationDialogComponent, {
      data: {
        title: "DELETE_PlUGIN_FILE_TITLE",
        message: "DELETE_PlUGIN_FILE_MESSAGE"
      },
      height: '80vh',
      width: '80vw',
      disableClose: true
    });

    this.dialogRefValidation.afterClosed().pipe(
      switchMap((result: any) => {
        if (result && result.data !== 'canceled') {
          return this.pluginFileService.delete(pluginFile.id).pipe(
            switchMap(() => this.translate.get("DELETE_SUCCESS")),
            catchError((error: any) => {
              return this.translate.get("DELETE_ERROR").pipe(
                switchMap((trad: string) => {
                  this.loggerService.error(trad);
                  throw error;
                })
              );
            })
          );
        }
        return of(null);
      }),
      finalize(() => this.isLoading.set(false))
    ).subscribe((trad: string | null) => {
      if (trad) {
        this.loggerService.success(trad);
        this.init();
      }
    });
  }

  save() {
    this.isLoading.set(true);
    this.pluginFileService.save(this.currentPluginFile()).pipe(
      switchMap(() => this.translate.get("SAVE_SUCCESS")),
      catchError((error: any) => {
        return this.translate.get("SAVE_ERROR").pipe(
          switchMap((trad: string) => {
            this.loggerService.error(trad);
            throw error;
          })
        );
      }),
      finalize(() => this.isLoading.set(false))
    ).subscribe((trad: string) => {
      this.loggerService.success(trad);
      this.resetCurrentPluginFile();
      this.init();
    });
  }

  close(): void {
    this.dialogRef.close({ validate: false });
  }

  validate() {
    this.dialogRef.close({ validate: true, plugin: this.plugin });
  }

  onFileChange(event: any) {
    const file = event.target.files[0];
    if (!file) return;

    const reader = new FileReader();

    reader.onload = () => {
      const base64Content: string = reader.result as string;
      const current = this.currentPluginFile();
      current.fileName = file.name;
      current.data = base64Content;
      this.currentPluginFile.set(current);
      this.selectedFileName.set(file.name);
    };

    reader.readAsDataURL(file);
  }

  edit(element: PluginFile) {
    this.currentPluginFile.set({ ...element });
    const current = this.currentPluginFile();
    current.isEditable = true;
    this.currentPluginFile.set(current);
    this.selectedFileName.set(element.fileName);
  }
}