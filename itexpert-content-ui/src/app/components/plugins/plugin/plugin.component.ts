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

@Component({
  selector: 'app-plugin',
  templateUrl: './plugin.component.html',
  styleUrl: './plugin.component.css'
})
export class PluginComponent implements OnInit {
  user: User;

  dialogRefPlugin: MatDialogRef<PluginDialogComponent>;
  dialogRefValidation: MatDialogRef<ValidationDialogComponent>;

  displayedColumns: string[] = ['Status', 'Name', 'Description', 'Actions'];

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
    this.user = JSON.parse(
      JSON.parse(
        JSON.stringify((window.localStorage.getItem('userInfo')))
      )
    );
    this.init();
  }


  init() {
    this.pluginService.getAll().subscribe(
      (response: any) => {
        //next() callback
        this.dataSource = new MatTableDataSource(response);
      },
      (error) => {                              //error() callback
        this.toast.error('Request failed with error');
      });
  }


  enable(plugin: Plugin) {
    plugin.enabled = true;
    this.save(plugin);
  }

  disable(plugin: Plugin) {
    plugin.enabled = false;
    this.save(plugin);
  }


  create() {
    this.dialogRefPlugin = this.dialog.open(PluginDialogComponent, {
      data: {
        title: "CANCEL_MODIFICATIONS_TITLE",
        message: "CANCEL_MODIFICATIONS_MESSAGE"
      },
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    this.dialogRefPlugin.afterClosed()
      .subscribe(result => {
        if (result.data === 'validated') {
          this.save(result.plugin);
        }
      });
  }

  update(plugin: Plugin) {
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
        if (data.refresh) {
          this.init();
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
        if (result && result.plugin && result.data !== 'canceled') {
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

}
