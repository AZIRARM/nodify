import { Component, OnInit, inject, signal, WritableSignal } from '@angular/core';
import { MatTableDataSource } from "@angular/material/table";
import { ContentNode } from "../../../modeles/ContentNode";
import { MatDialog, MatDialogRef } from "@angular/material/dialog";
import { ValidationDialogComponent } from "../../commons/validation-dialog/validation-dialog.component";
import { TranslateService } from "@ngx-translate/core";
import { ContentNodeService } from "../../../services/ContentNodeService";
import { UserService } from "../../../services/UserService";
import { LoggerService } from "../../../services/LoggerService";
import { PluginService } from "../../../services/PluginService";
import { Plugin } from "../../../modeles/Plugin";
import { UserAccessService } from "../../../services/UserAccessService";
import { of } from "rxjs";
import { catchError, finalize, switchMap } from "rxjs/operators";

@Component({
  selector: 'app-deleted-plugins-dialog',
  templateUrl: './deleted-plugins-dialog.component.html',
  styleUrl: './deleted-plugins-dialog.component.css',
  standalone: false
})
export class DeletedPluginsDialogComponent implements OnInit {
  user: WritableSignal<any> = signal(null);
  displayedColumns: string[] = ['Name', 'CreationDate', 'ModificationDate', 'ModifiedBy', 'Actions'];
  dataSource: WritableSignal<MatTableDataSource<Plugin>> = signal(new MatTableDataSource<Plugin>([]));
  dialogValidationRef: MatDialogRef<ValidationDialogComponent>;


  public dialogRef = inject(MatDialogRef<DeletedPluginsDialogComponent>);
  private translate = inject(TranslateService);
  private pluginService = inject(PluginService);
  private userService = inject(UserService);
  public userAccessService = inject(UserAccessService);
  private loggerService = inject(LoggerService);
  private dialog = inject(MatDialog);

  ngOnInit() {
    this.user.set(this.userAccessService.getCurrentUser());
    this.init();
  }

  init() {

    this.pluginService.getDeleted().pipe(
      catchError(error => {
        console.error(error);
        return of([]);
      }),

    ).subscribe((response: any) => {
      if (response) {
        response = response.sort((a: any, b: any) => a.code.localeCompare(b.code));
        this.dataSource.set(new MatTableDataSource(response));
      }
    });
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

    this.dialogValidationRef.afterClosed().pipe(
      switchMap((result: any) => {
        if (result && result.data && result.data === "validated") {
          return this.pluginService.activate(element.id).pipe(
            switchMap(() => this.translate.get("ACTIVATION_SUCCESS")),
            catchError((error: any) => {
              return this.translate.get("ACTIVATION_ERROR").pipe(
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

    ).subscribe((trad: string | null) => {
      if (trad) {
        this.loggerService.success(trad);
        this.init();
      }
    });
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

    this.dialogValidationRef.afterClosed().pipe(
      switchMap((result: any) => {
        if (result && result.data !== 'canceled') {
          return this.pluginService.deleteDefinitively(element.id).pipe(
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

    ).subscribe((trad: string | null) => {
      if (trad) {
        this.loggerService.success(trad);
        this.init();
      }
    });
  }

  close() {
    this.dialogRef.close();
  }
}