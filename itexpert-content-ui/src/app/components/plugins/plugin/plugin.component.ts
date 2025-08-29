import {Component, OnInit} from '@angular/core';
import {User} from "../../../modeles/User";
import {MatDialog, MatDialogRef} from "@angular/material/dialog";
import {ValidationDialogComponent} from "../../commons/validation-dialog/validation-dialog.component";
import {MatTableDataSource} from "@angular/material/table";
import {TranslateService} from "@ngx-translate/core";
import {ToastrService} from "ngx-toastr";
import {LoggerService} from "../../../services/LoggerService";
import {PluginDialogComponent} from "../plugin-dialog/plugin-dialog.component";
import {Plugin} from "../../../modeles/Plugin";
import {PluginService} from "../../../services/PluginService";
import {UserAccessService} from "../../../services/UserAccessService";
import {DeletedPluginsDialogComponent} from "../deleted-plugins-dialog/deleted-plugins-dialog.component";
import {PluginFilesDialogComponent} from "../plugin-files-dialog/plugin-files-dialog.component";

@Component({
  selector: 'app-plugin',
  templateUrl: './plugin.component.html',
  styleUrl: './plugin.component.css'
})
export class PluginComponent implements OnInit {
  user: User;

  dialogRefPlugin: MatDialogRef<PluginDialogComponent>;
  dialogRefValidation: MatDialogRef<ValidationDialogComponent>;
  dialogRefDeleted: MatDialogRef<DeletedPluginsDialogComponent>;
  dialogRefFiles: MatDialogRef<PluginFilesDialogComponent>;

  displayedColumns: string[] = ['Status', 'Name', 'Description', 'ModifiedBy', 'CreationDate', 'ModificationDate', 'Actions'];

  dataSource: MatTableDataSource<Plugin>;

  constructor(private translate: TranslateService,
              private toast: ToastrService,
              private loggerService: LoggerService,
              public userAccessService: UserAccessService,
              public pluginService: PluginService,
              private dialog: MatDialog
  ) {
  }

  ngOnInit() {
   this.user = this.userAccessService.getCurrentUser()
    this.init();
  }


  init() {
    this.pluginService.getNotDeleted().subscribe(
      (response: any) => {
        //next() callback
        this.dataSource = new MatTableDataSource(response);
      },
      (error) => {                              //error() callback
        this.toast.error('Request failed with error');
      });
  }


  status(plugin: Plugin) {
    if (!plugin.enabled) {
      this.pluginService.enable(plugin.id).subscribe(
        (response: any) => {
          //next() callback
          this.init();
        },
        (error) => {                              //error() callback
          this.toast.error('Request failed with error');
        });
    } else {
      this.pluginService.disable(plugin.id).subscribe(
        (response: any) => {
          //next() callback
          this.init();
        },
        (error) => {                              //error() callback
          this.toast.error('Request failed with error');
        });
    }
    this.save(plugin);
  }


  create() {
    this.update(new Plugin());
  }

  update(plugin: Plugin) {
    plugin.editable = true;
    this.dialogRefPlugin = this.dialog.open(PluginDialogComponent, {
      data: {
        plugin: plugin,
        user: this.user
      },
      height: '100%',
      width: '90vw',
      disableClose: true
    });
    this.dialogRefPlugin.afterClosed()
      .subscribe((data: any) => {
        if (data.validate) {
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
    this.dialogRefValidation.afterClosed()
      .subscribe(result => {
        if (result && result.data !== 'canceled') {

          this.pluginService.delete(plugin.id).subscribe(
            response => {
              this.translate.get("DELETE_SUCCESS").subscribe(trad => {
                this.loggerService.success(trad);
                this.init();
              })

            },
            error => {
              this.translate.get("DELETE_ERROR").subscribe(trad => {
                this.loggerService.error(trad);
              })
            });
        }
      });
  }

  private save(plugin: Plugin) {
    this.pluginService.save(plugin).subscribe(
      (response: any) => {

        this.translate.get("SAVE_SUCCESS").subscribe(trad => {
          this.loggerService.success(trad);
          this.init();
        })

      },
      error => {
        this.translate.get("SAVE_ERROR").subscribe(trad => {
          this.loggerService.error(trad);
        })
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
      .subscribe(result => {
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
      .subscribe(result => {
        this.init();
      });
  }

  export(element: Plugin) {
    this.pluginService.export(element.id).subscribe((data: any) => {
      const jsonString = typeof data === 'string' ? data : JSON.stringify(data, null, 2); // Beautifié avec indentation
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
            next: (res) => {
              console.log('Plugin successfully imported:', res);
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
}
