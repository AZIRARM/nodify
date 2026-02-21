import {Component, Inject, OnInit} from '@angular/core';
import {MatTableDataSource} from "@angular/material/table";
import {MAT_DIALOG_DATA, MatDialog, MatDialogRef} from "@angular/material/dialog";
import {TranslateService} from "@ngx-translate/core";
import {ValidationDialogComponent} from "../../commons/validation-dialog/validation-dialog.component";
import {LoggerService} from "../../../services/LoggerService";
import {UserAccessService} from "../../../services/UserAccessService";
import {StatusEnum} from "../../../modeles/StatusEnum";
import {Observable} from "rxjs";

// Interface pour les services de publication
export interface PublicationService {
  getPublicationHistory(itemCode: string): Observable<any>;
  revertToVersion(version: string): Observable<any>;
  deployToVersion(version: string): Observable<any>;
  deleteVersionDefinitively(version: string): Observable<any>;
}

// Interface pour les éléments publiés
export interface PublicationItem {
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
  styleUrls: ['./published-items-dialog.component.css']
})
export class PublishedItemsDialogComponent implements OnInit {

  // Propriétés configurables
  titleKey: string = 'PUBLICATION_HISTORY';
  icon: string = 'history';
  displayTypeColumn: boolean = true;
  itemName: string = '';
  itemCode: string = '';
  publicationService: PublicationService;

  displayedColumns: string[] = ['Status', 'Version', 'Last Modification', 'Modified by', 'Actions'];
  dataSource: MatTableDataSource<PublicationItem> = new MatTableDataSource<PublicationItem>([]);
  dialogValidationRef: MatDialogRef<ValidationDialogComponent>;

  itemIcon: string = 'description'; // Icône par défaut

  constructor(
    public dialogRef: MatDialogRef<PublishedItemsDialogComponent>,
    private translate: TranslateService,
    private loggerService: LoggerService,
    private userAccessService: UserAccessService,
    private dialog: MatDialog,
    @Inject(MAT_DIALOG_DATA) private data: any
  ) {
    // Récupérer les données passées
    this.itemName = data?.itemName || '';
    this.itemCode = data?.itemCode || '';
    this.itemIcon = data?.itemIcon || 'description'; // Récupérer l'icône
    this.publicationService = data?.publicationService;

    // Surcharger les propriétés si fournies
    if (data?.titleKey) this.titleKey = data.titleKey;
    if (data?.icon) this.icon = data.icon;
    if (data?.displayTypeColumn !== undefined) this.displayTypeColumn = data.displayTypeColumn;
  }

  ngOnInit() {
    // Ajouter la colonne Type si nécessaire
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

    this.publicationService.getPublicationHistory(this.itemCode).subscribe(
      (response: any) => {
        if (response) {
          let filtred: any[] = response.filter((element: any) => (element.status === StatusEnum.ARCHIVE || element.status === StatusEnum.PUBLISHED))
                     .sort((a: any, b: any) => Number(b.version) - Number(a.version));

          this.dataSource.data = filtred;
        }
      },
      (error: any) => {
        console.error('Erreur chargement historique', error);
      }
    );
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
    this.dialogValidationRef = this.dialog.open(ValidationDialogComponent, {
      data: {
        title: "REVERT_VERSION_TITLE",
        message: "REVERT_VERSION_MESSAGE"
      },
      height: '80vh',
      width: '80vw',
      disableClose: true
    });

    this.dialogValidationRef.afterClosed().subscribe((result: any) => {
      if (result && result.data && result.data === "validated") {
        this.publicationService.revertToVersion(element.version).subscribe(
          () => {
            this.translate.get("REVERT_SUCCESS").subscribe((trad: string) => {
              this.loggerService.success(trad);
              this.loadHistory();
            });
          },
          (error: any) => {
            this.translate.get("REVERT_ERROR").subscribe((trad: string) => {
              this.loggerService.error(trad);
            });
          }
        );
      }
    });
  }

  deploy(element: PublicationItem) {
    this.dialogValidationRef = this.dialog.open(ValidationDialogComponent, {
      data: {
        title: "DEPLOY_VERSION_TITLE",
        message: "DEPLOY_VERSION_MESSAGE"
      },
      height: '80vh',
      width: '80vw',
      disableClose: true
    });

    this.dialogValidationRef.afterClosed().subscribe((result: any) => {
      if (result && result.data && result.data === "validated") {
        this.publicationService.deployToVersion(element.version).subscribe(
          () => {
            this.translate.get("DEPLOY_SUCCESS").subscribe((trad: string) => {
              this.loggerService.success(trad);
            });
          },
          (error: any) => {
            this.translate.get("DEPLOY_ERROR").subscribe((trad: string) => {
              this.loggerService.error(trad);
            });
          }
        );
      }
    });
  }

  delete(element: PublicationItem) {
    this.dialogValidationRef = this.dialog.open(ValidationDialogComponent, {
      data: {
        title: "DELETE_VERSION_TITLE",
        message: "DELETE_VERSION_MESSAGE"
      },
      height: '80vh',
      width: '80vw',
      disableClose: true
    });

    this.dialogValidationRef.afterClosed().subscribe((result: any) => {
      if (result && result.data && result.data === "validated") {
        this.publicationService.deleteVersionDefinitively(element.version).subscribe(
          () => {
            this.translate.get("DELETE_SUCCESS").subscribe((trad: string) => {
              this.loggerService.success(trad);
              this.loadHistory();
            });
          },
          (error: any) => {
            this.translate.get("DELETE_ERROR").subscribe((trad: string) => {
              this.loggerService.error(trad);
            });
          }
        );
      }
    });
  }

  cancel() {
    this.dialogRef.close();
  }
}
