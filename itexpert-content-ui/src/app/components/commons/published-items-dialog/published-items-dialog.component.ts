import { Component, Inject, OnInit, inject, signal, WritableSignal } from '@angular/core';
import { MatTableDataSource } from "@angular/material/table";
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from "@angular/material/dialog";
import { TranslateService } from "@ngx-translate/core";
import { ValidationDialogComponent } from "../../commons/validation-dialog/validation-dialog.component";
import { LoggerService } from "../../../services/LoggerService";
import { UserAccessService } from "../../../services/UserAccessService";
import { StatusEnum } from "../../../modeles/StatusEnum";
import { Observable, of } from "rxjs";
import { catchError, finalize, switchMap } from "rxjs/operators";

export interface PublicationService {
  getPublicationHistory(code: string): Observable<any>;
  revertToVersion(code: string, version: string): Observable<any>;
  deployToVersion(code: string, version: string): Observable<any>;
  deleteVersionDefinitively(code: string, version: string): Observable<any>;
}

export interface PublicationItem {
  code: string;
  version: string;
  modificationDate: string;
  modifiedBy: string;
  type?: string;
  status: 'PUBLISHED' | 'ARCHIVE' | 'SNAPSHOT';
  IsGranted?: boolean;
  [key: string]: any;
}

@Component({
  selector: 'app-published-items-dialog',
  templateUrl: './published-items-dialog.component.html',
  styleUrls: ['./published-items-dialog.component.css'],
  standalone: false
})
export class PublishedItemsDialogComponent implements OnInit {

  titleKey: string = 'PUBLICATION_HISTORY';
  icon: string = 'history';
  displayTypeColumn: boolean = true;
  itemName: string = '';
  itemCode: string = '';
  publicationService: PublicationService;
  itemIcon: string = 'description';


  displayedColumns: string[] = ['Status', 'Version', 'Last Modification', 'Modified by', 'Actions'];
  dataSource: WritableSignal<MatTableDataSource<PublicationItem>> = signal(new MatTableDataSource<PublicationItem>([]));
  dialogValidationRef: MatDialogRef<ValidationDialogComponent>;

  private dialogRef = inject(MatDialogRef<PublishedItemsDialogComponent>);
  private translate = inject(TranslateService);
  private loggerService = inject(LoggerService);
  private userAccessService = inject(UserAccessService);
  private dialog = inject(MatDialog);
  private data = inject(MAT_DIALOG_DATA);

  constructor() {
    this.itemName = this.data?.itemName || '';
    this.itemCode = this.data?.itemCode || '';
    this.itemIcon = this.data?.itemIcon || 'description';
    this.publicationService = this.data?.publicationService;

    if (this.data?.titleKey) this.titleKey = this.data.titleKey;
    if (this.data?.icon) this.icon = this.data.icon;
    if (this.data?.displayTypeColumn !== undefined) this.displayTypeColumn = this.data.displayTypeColumn;
  }

  ngOnInit() {
    if (this.displayTypeColumn && !this.displayedColumns.includes('Type')) {
      this.displayedColumns.splice(2, 0, 'Type');
    }

    this.loadHistory();
  }

  loadHistory() {
    if (!this.publicationService) {
      console.error('PublicationService non fourni');
      return;
    }


    this.publicationService.getPublicationHistory(this.itemCode).pipe(
      catchError((error: any) => {
        console.error('Erreur chargement historique', error);
        return of([]);
      }),

    ).subscribe((response: any) => {
      if (response) {
        let filtred: any[] = response
          .filter((element: any) => (element.status === StatusEnum.ARCHIVE || element.status === StatusEnum.PUBLISHED))
          .sort((a: any, b: any) => Number(b.version) - Number(a.version));

        this.dataSource.set(new MatTableDataSource(filtred));
      }
    });
  }

  isPublished(element: PublicationItem): boolean {
    return element.status === 'PUBLISHED';
  }

  isArchived(element: PublicationItem): boolean {
    return element.status === 'ARCHIVE';
  }

  isSnapshot(element: PublicationItem): boolean {
    return element.status === 'SNAPSHOT';
  }

  revert(element: PublicationItem) {

    const dialogValidationRef = this.dialog.open(ValidationDialogComponent, {
      data: {
        title: "REVERT_VERSION_TITLE",
        message: "REVERT_VERSION_MESSAGE"
      },
      height: '80vh',
      width: '80vw',
      disableClose: true
    });

    dialogValidationRef.afterClosed().pipe(
      switchMap((result: any) => {
        if (result && result.data && result.data === "validated") {
          return this.publicationService.revertToVersion(element.code, element.version).pipe(
            switchMap(() => this.translate.get("REVERT_SUCCESS")),
            catchError((error: any) => {
              return this.translate.get("REVERT_ERROR").pipe(
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
        this.loadHistory();
      }
    });
  }

  deploy(element: PublicationItem) {

    const dialogValidationRef = this.dialog.open(ValidationDialogComponent, {
      data: {
        title: "DEPLOY_VERSION_TITLE",
        message: "DEPLOY_VERSION_MESSAGE"
      },
      height: '80vh',
      width: '80vw',
      disableClose: true
    });

    dialogValidationRef.afterClosed().pipe(
      switchMap((result: any) => {
        if (result && result.data && result.data === "validated") {
          return this.publicationService.deployToVersion(element.code, element.version).pipe(
            switchMap(() => this.translate.get("DEPLOY_SUCCESS")),
            catchError((error: any) => {
              return this.translate.get("DEPLOY_ERROR").pipe(
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
      }
    });
  }

  delete(element: PublicationItem) {

    const dialogValidationRef = this.dialog.open(ValidationDialogComponent, {
      data: {
        title: "DELETE_VERSION_TITLE",
        message: "DELETE_VERSION_MESSAGE"
      },
      height: '80vh',
      width: '80vw',
      disableClose: true
    });

    dialogValidationRef.afterClosed().pipe(
      switchMap((result: any) => {
        if (result && result.data && result.data === "validated") {
          return this.publicationService.deleteVersionDefinitively(element.code, element.version).pipe(
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
        this.loadHistory();
      }
    });
  }

  cancel() {
    this.dialogRef.close();
  }
}