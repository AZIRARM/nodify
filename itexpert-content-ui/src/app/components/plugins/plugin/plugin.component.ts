import { Component, OnInit, inject, signal, WritableSignal } from '@angular/core';
import { User } from "../../../modeles/User";
import { MatDialog, MatDialogRef } from "@angular/material/dialog";
import { ValidationDialogComponent } from "../../commons/validation-dialog/validation-dialog.component";
import { MatTableDataSource } from "@angular/material/table";
import { TranslateService } from "@ngx-translate/core";
import { ToastrService } from "ngx-toastr";
import { LoggerService } from "../../../services/LoggerService";
import { PluginDialogComponent } from "../plugin-dialog/plugin-dialog.component";
import { Plugin } from "../../../modeles/Plugin";
import { PluginService } from "../../../services/PluginService";
import { UserAccessService } from "../../../services/UserAccessService";
import { DeletedPluginsDialogComponent } from "../deleted-plugins-dialog/deleted-plugins-dialog.component";
import { PluginFilesDialogComponent } from "../plugin-files-dialog/plugin-files-dialog.component";
import { of } from "rxjs";
import { catchError, finalize, switchMap } from "rxjs/operators";

@Component({
  selector: 'app-plugin',
  templateUrl: './plugin.component.html',
  styleUrl: './plugin.component.css',
  standalone: false
})
export class PluginComponent implements OnInit {
  user: WritableSignal<User> = signal<User>({} as User);
  dialogRefPlugin: MatDialogRef<PluginDialogComponent>;
  dialogRefValidation: MatDialogRef<ValidationDialogComponent>;
  dialogRefDeleted: MatDialogRef<DeletedPluginsDialogComponent>;
  dialogRefFiles: MatDialogRef<PluginFilesDialogComponent>;
  displayedColumns: string[] = ['Status', 'Name', 'Description', 'ModifiedBy', 'CreationDate', 'ModificationDate', 'Actions'];
  dataSource: WritableSignal<MatTableDataSource<Plugin>> = signal(new MatTableDataSource<Plugin>([]));
  totalDeleteds: WritableSignal<number> = signal(0);

  private translate = inject(TranslateService);
  private toast = inject(ToastrService);
  private loggerService = inject(LoggerService);
  public userAccessService = inject(UserAccessService);
  public pluginService = inject(PluginService);
  private dialog = inject(MatDialog);

  constructor() {
    this.dataSource.set(new MatTableDataSource<Plugin>([]));
  }

  ngOnInit() {
    this.user.set(this.userAccessService.getCurrentUser());
    this.init();
  }

  init() {
    this.pluginService.getNotDeleted().pipe(
      catchError((error) => {
        this.toast.error('Request failed with error');
        return of([]);
      })
    ).subscribe((response: any) => {
      this.dataSource.set(new MatTableDataSource(response || []));
    });

    this.pluginService.getDeleted().pipe(
      catchError((error) => {
        console.error(error);
        return of([]);
      })
    ).subscribe((response: any) => {
      if (response) {
        this.totalDeleteds.set(response.length);
      }
    });
  }

  changeStatus(plugin: Plugin) {
    const action$ = !plugin.enabled
      ? this.pluginService.enable(plugin.id)
      : this.pluginService.disable(plugin.id);

    action$.pipe(
      catchError((error) => {
        this.toast.error('Request failed with error');
        return of(null);
      }),
    ).subscribe(() => {
      this.init();
      this.save(plugin);
    });
  }

  getStatusTooltip(element: Plugin): string {
    return element.enabled ? "DISABLE" : "ENABLE";
  }

  create() {
    this.update(new Plugin());
  }

  update(plugin: Plugin) {
    plugin.editable = true;
    this.dialogRefPlugin = this.dialog.open(PluginDialogComponent, {
      data: {
        plugin: plugin,
        user: this.user()
      },
      height: '100%',
      width: '90vw',
      disableClose: true
    });
    this.dialogRefPlugin.afterClosed()
      .subscribe((data: any) => {
        if (data && data.validate) {
          this.save(data.plugin);
        }
      });
  }

  delete(plugin: Plugin) {
    this.dialogRefValidation = this.dialog.open(ValidationDialogComponent, {
      data: {
        title: "DELETE_PlUGIN_TITLE",
        message: "DELETE_PlUGIN_MESSAGE"
      },
      height: '80vh',
      width: '80vw',
      disableClose: true
    });

    this.dialogRefValidation.afterClosed().pipe(
      switchMap((result: any) => {
        if (result && result.data !== 'canceled') {
          return this.pluginService.delete(plugin.id).pipe(
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
      })
    ).subscribe((trad: string | null) => {
      if (trad) {
        this.loggerService.success(trad);
        this.init();
      }
    });
  }

  private save(plugin: Plugin) {

    this.pluginService.save(plugin).pipe(
      switchMap(() => this.translate.get("SAVE_SUCCESS")),
      catchError((error: any) => {
        return this.translate.get("SAVE_ERROR").pipe(
          switchMap((trad: string) => {
            this.loggerService.error(trad);
            throw error;
          })
        );
      }),

    ).subscribe((trad: string) => {
      this.loggerService.success(trad);
      this.init();
    });
  }

  getPublishedIcon(element: Plugin) {
    if (element.enabled)
      return "primary";
    else
      return "danger";
  }

  deleteds() {
    this.dialogRefDeleted = this.dialog.open(DeletedPluginsDialogComponent, {
      height: '80vh',
      width: '80vw',
      disableClose: true
    }
    );
    this.dialogRefDeleted.afterClosed()
      .subscribe(() => {
        this.init();
      });
  }

  assets(element: Plugin) {
    this.dialogRefFiles = this.dialog.open(PluginFilesDialogComponent, {
      data: element,
      height: '80vh',
      width: '80vw',
      disableClose: true
    }
    );
    this.dialogRefFiles.afterClosed()
      .subscribe(() => {
        this.init();
      });
  }

  export(element: Plugin) {
    this.pluginService.export(element.id).subscribe((data: any) => {
      const jsonString = typeof data === 'string' ? data : JSON.stringify(data, null, 2);
      const blob: Blob = new Blob([jsonString], { type: 'application/json' });
      const a = document.createElement('a');
      const objectUrl = URL.createObjectURL(blob);
      a.href = objectUrl;
      a.download = element.name + ".json";
      a.click();
      URL.revokeObjectURL(objectUrl);
    });
  }

  import(event: Event) {
    const input = event.target as HTMLInputElement;

    if (input?.files?.length) {
      const file = input.files[0];
      const reader = new FileReader();

      reader.onload = () => {
        try {
          const plugin: Plugin = JSON.parse(reader.result as string);
          console.log('Plugin to import:', plugin);

          this.pluginService.import(plugin).subscribe({
            next: () => {
              console.log('Plugin successfully imported');
              this.init();
            },
            error: (err) => {
              console.error('Import failed:', err);
            }
          });
        } catch (error) {
          console.error('Invalid JSON file:', error);
        }
      };
      reader.readAsText(file);
    } else {
      console.warn('No file selected or input is invalid.');
    }
  }

  status(element: Plugin) {
    element.enabled = !element.enabled;
  }

}