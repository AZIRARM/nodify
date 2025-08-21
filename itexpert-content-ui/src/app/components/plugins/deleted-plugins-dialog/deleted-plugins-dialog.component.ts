import {Component, OnInit} from '@angular/core';
import {MatTableDataSource} from "@angular/material/table";
import {ContentNode} from "../../../modeles/ContentNode";
import {MatDialog, MatDialogRef} from "@angular/material/dialog";
import {ValidationDialogComponent} from "../../commons/validation-dialog/validation-dialog.component";
import {TranslateService} from "@ngx-translate/core";
import {ContentNodeService} from "../../../services/ContentNodeService";
import {UserService} from "../../../services/UserService";
import {LoggerService} from "../../../services/LoggerService";
import {PluginService} from "../../../services/PluginService";
import {Plugin} from "../../../modeles/Plugin";
import {UserAccessService} from "../../../services/UserAccessService";

@Component({
  selector: 'app-deleted-plugins-dialog',
  templateUrl: './deleted-plugins-dialog.component.html',
  styleUrl: './deleted-plugins-dialog.component.css'
})
export class DeletedPluginsDialogComponent implements OnInit {
  user: any;

  displayedColumns: string[] = ['Name',  'CreationDate', 'ModificationDate', 'ModifiedBy', 'Actions'];

  dataSource: MatTableDataSource<ContentNode>;
  dialogValidationRef: MatDialogRef<ValidationDialogComponent>;

  constructor(
    public dialogRef: MatDialogRef<DeletedPluginsDialogComponent>,
    private translate: TranslateService,
    private pluginService: PluginService,
    private userService: UserService,
    private userAccessService: UserAccessService,
    private loggerService: LoggerService,
    private dialog: MatDialog
  ) {
  }

  ngOnInit() {
   this.user = this.userAccessService.getCurrentUser()
   this.init();
  }

  init() {
    this.pluginService.getDeleted().subscribe(
      (response: any) => {
        if (response) {
          response.map((content: any) => this.setUserName(content));
          response=response.sort((a:any, b:any) => a.code.localeCompare(b.code));
          this.dataSource = new MatTableDataSource(response);
        }
      },
      error => {
        console.error(error);
      }
    );
  }


  activate(element: Plugin) {

    this.dialogValidationRef = this.dialog.open(ValidationDialogComponent, {
      data: {
        title: "ACTIVATE_DELETE_PLUGIN_TITLE",
        message: "ACTIVATE_DELETE_PLUGIN_MESSAGE"
      },
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    this.dialogValidationRef.afterClosed()
      .subscribe((result: any) => {
        if (result && result.data && result.data === "validated") {
          this.pluginService.activate(element.name, this.user!.id).subscribe(() => {
            this.translate.get("ACTIVATION_SUCCESS").subscribe(trad => {
              this.loggerService.success(trad);
              this.init();
            });
          }, (error) => {
            this.translate.get("ACTIVATION_ERROR").subscribe(trad => {
              this.loggerService.error(trad);
            });
          });
        }
      });
  }

  setUserName(param: any) {
    this.userService.setUserName(param);
  }

  delete(element: Plugin) {

    this.dialogValidationRef = this.dialog.open(ValidationDialogComponent, {
      data: {
        title: "DELETE_PlUGIN_TITLE",
        message: "DELETE_PlUGIN_MESSAGE"
      },
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    this.dialogValidationRef.afterClosed()
      .subscribe(result => {
        if (result && result.data !== 'canceled') {
          this.pluginService.deleteDefinitively(element.id, this.user!.id).subscribe(
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

  close() {
    this.dialogRef.close();
  }

}
