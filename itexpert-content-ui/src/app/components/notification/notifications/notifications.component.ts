import { AfterViewInit, Component, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { MatTableDataSource } from "@angular/material/table";
import { MatDialog, MatDialogRef } from "@angular/material/dialog";
import { MatPaginator } from "@angular/material/paginator";
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { ValidationDialogComponent } from "../../commons/validation-dialog/validation-dialog.component";
import { NotificationService } from "../../../services/NotificationService";
import { Notification } from "../../../modeles/Notification";
import { UserAccessService } from "../../../services/UserAccessService";
import { AuthenticationService } from 'src/app/services/AuthenticationService';
import { Subscription } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-notifications',
  templateUrl: './notifications.component.html',
  styleUrls: ['./notifications.component.css']
})
export class NotificationsComponent implements OnInit, OnDestroy {

  // Propriétés utilisateur
  user: any;

  // Configuration du tableau
  displayedColumns: string[] = ['Date', 'Type', 'Code', 'Message', 'Version', 'By', 'Actions'];
  dataSource: MatTableDataSource<Notification> = new MatTableDataSource<Notification>([]);

  // Références
  dialogRef: MatDialogRef<ValidationDialogComponent>;

  @ViewChild(MatPaginator)
  paginator!: MatPaginator;

  // Pagination
  total: number = 0;
  pageSize: number = 10;
  pageIndex: number = 0;
  pageSizeOptions: number[] = [5, 10, 20, 50];

  // Statistiques
  unreadCount: number = 0;

  // Filtres
  filterType: string = 'all';
  searchText: string = '';

  // WebSocket
  private wsSubscription: Subscription | null = null;

  @ViewChild('markReadToggle') markReadToggle: MatSlideToggle;

  constructor(
    private notificationService: NotificationService,
    private userAccessService: UserAccessService,
    private authenticationService: AuthenticationService,
    private translate: TranslateService,
    private dialog: MatDialog
  ) {}

  ngOnInit() {
    this.user = this.userAccessService.getCurrentUser();
    this.loadNotifications();
  }

  ngOnDestroy() {
    this.closeWebSocket();
  }

  /**
   * Ferme la connexion WebSocket
   */
  closeWebSocket() {
    if (this.wsSubscription) {
      this.wsSubscription.unsubscribe();
      this.wsSubscription = null;
    }
  }

  /**
   * Charge les notifications depuis le WebSocket
   */
  loadNotifications() {
    if (!this.authenticationService.isAuthenticated()) {
      return;
    }

    // Fermer l'ancienne connexion WebSocket
    this.closeWebSocket();

    console.log(`Chargement notifications - page: ${this.pageIndex}, size: ${this.pageSize}, filter: ${this.filterType}`);

    this.wsSubscription = this.notificationService.connectWebSocket(
      this.authenticationService.getAccessToken(),
      this.pageIndex,
      this.pageSize
    ).subscribe({
      next: (data: any) => {
        console.log('Données reçues:', data);
        this.total = data.count || 0;
        this.dataSource.data = data.unread || [];

        // Note: Si la propriété 'read' n'existe pas, on utilise une autre logique
        // ou on considère que toutes les notifications sont non lues par défaut
        this.unreadCount = this.dataSource.data.length;

        // Mettre à jour le paginator après réception des données
        if (this.paginator) {
          this.paginator.length = this.total;
          this.paginator.pageIndex = this.pageIndex;
          this.paginator.pageSize = this.pageSize;
        }

        // Appliquer le filtre si nécessaire
        if (this.filterType !== 'all') {
          this.applyFilter();
        }
      },
      error: (err: any) => {
        console.error("Erreur WebSocket:", err);
        this.dataSource.data = [];
        this.total = 0;
        this.unreadCount = 0;
      },
      complete: () => {
        console.log("WebSocket fermé");
        this.wsSubscription = null;
      }
    });
  }

  /**
   * Gère le changement de page
   */
  onPageChange(event: any) {
    console.log('Changement de page:', event);
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadNotifications();
  }

  /**
   * Marque une notification comme lue
   */
  markAsReaded(element: Notification) {
    this.notificationService.markAsReaded(element.id).subscribe({
      next: () => {
        console.log('Notification marquée comme lue');
        // Retirer l'élément du tableau ou le marquer comme lu
        this.dataSource.data = this.dataSource.data.filter(n => n.id !== element.id);
        this.unreadCount = this.dataSource.data.length;
        this.loadNotifications(); // Recharger après modification
      },
      error: (error: any) => {
        console.error('Erreur marquage lu:', error);
        this.showErrorMessage('ERROR_MARK_READ');
      }
    });
  }

  /**
   * Marque toutes les notifications comme lues
   */
  markAllAsReaded(event: any) {
    event.source.checked = false;

    if (this.dataSource.data.length === 0) {
      return;
    }

    this.translate.get("MARK_ALL_READ_CONFIRM_TITLE").subscribe(title => {
      this.translate.get("MARK_ALL_READ_CONFIRM_MESSAGE").subscribe(message => {
        const dialogRef = this.dialog.open(ValidationDialogComponent, {
          data: {
            title: title,
            message: message,
          },
          disableClose: true
        });

        dialogRef.afterClosed().subscribe(result => {
          if (result?.data === 'validated') {
            this.notificationService.markAllReaded().subscribe({
              next: () => {
                this.dataSource.data = [];
                this.unreadCount = 0;
                this.loadNotifications();
                this.showSuccessMessage('ALL_MARKED_READ_SUCCESS');
                setTimeout(() => {
                  event.source.checked = true;
                });
              },
              error: (error: any) => {
                console.error('Erreur marquage tout lu:', error);
                this.showErrorMessage('ERROR_MARK_ALL_READ');
                setTimeout(() => {
                  event.source.checked = false;
                });
              }
            });
          } else {
            setTimeout(() => {
              event.source.checked = false;
            });
          }
        });
      });
    });
  }

  /**
   * Recharge les notifications (première page)
   */
  allreadyReaded() {
    this.pageIndex = 0;
    this.pageSize = 10;
    this.filterType = 'all';
    this.searchText = '';
    this.loadNotifications();
  }

  /**
   * Rafraîchit les notifications
   */
  refreshNotifications() {
    this.loadNotifications();
  }

  /**
   * Filtre les notifications par type
   */
  filterBy(type: string) {
    this.filterType = type;
    this.pageIndex = 0; // Retour à la première page
    this.applyFilter();
  }

  /**
   * Applique le filtre actuel
   */
  applyFilter() {
    if (this.filterType === 'unread') {
      // Si 'read' n'existe pas, on garde toutes les notifications
      // ou on implémente une autre logique
      console.log('Filtre unread appliqué');
    } else if (this.filterType !== 'all') {
      this.dataSource.data = this.dataSource.data.filter(n =>
        n.type?.toLowerCase() === this.filterType.toLowerCase()
      );
    }

    // Appliquer aussi la recherche si elle existe
    if (this.searchText) {
      this.dataSource.filter = this.searchText.trim().toLowerCase();
    }
  }

  /**
   * Recherche dans les notifications
   */
  onSearch(event: any) {
    this.searchText = event.target.value;
    this.applyFilter();
  }

  /**
   * Affiche les détails d'une notification
   */
    viewDetails(element: Notification) {
      this.translate.get("NOTIFICATION_DETAILS_TITLE").subscribe(title => {
        // Traduire les valeurs si ce sont des clés
        const typeValue = element.type ? this.translate.instant(element.type) : '';
        const codeValue = element.code ? this.translate.instant(element.code) : '';

        let message = `
          <strong>${this.translate.instant('TYPE')}:</strong> ${typeValue}<br>
          <strong>${this.translate.instant('CODE')}:</strong> ${element.typeCode || ''}<br>
          <strong>${this.translate.instant('DESCRIPTION')}:</strong> ${codeValue}<br>
          <strong>${this.translate.instant('VERSION')}:</strong> ${element.typeVersion || ''}<br>
          <strong>${this.translate.instant('DATE')}:</strong> ${element.date ? new Date(element.date).toLocaleString() : ''}<br>
        `;

        const dialogRef = this.dialog.open(ValidationDialogComponent, {
          data: {
            title: title,
            message: message,
            isHtml: true
          },
          width: '500px',
          disableClose: false
        });


        dialogRef.afterClosed().subscribe(result => {
          if (result?.data === 'validated') {
            this.markAsReaded(element);
          }
        });

      });
    }

  /**
   * Supprime une notification
   */
  deleteNotification(element: Notification) {
    this.translate.get("DELETE_NOTIFICATION_TITLE").subscribe(title => {
      this.translate.get("DELETE_NOTIFICATION_CONFIRM").subscribe(message => {
        const dialogRef = this.dialog.open(ValidationDialogComponent, {
          data: {
            title: title,
            message: message,
          },
          disableClose: true
        });

        dialogRef.afterClosed().subscribe(result => {
          if (result?.data === 'validated') {
            // Note: Si la méthode deleteNotification n'existe pas, utiliser markAsReaded ou une autre méthode
            this.notificationService.markAsReaded(element.id).subscribe({
              next: () => {
                console.log('Notification supprimée/marquée comme lue');
                this.loadNotifications();
                this.showSuccessMessage('NOTIFICATION_DELETED_SUCCESS');
              },
              error: (error: any) => {
                console.error('Erreur suppression:', error);
                this.showErrorMessage('ERROR_DELETE_NOTIFICATION');
              }
            });
          }
        });
      });
    });
  }

  /**
   * Affiche un message de succès
   */
  private showSuccessMessage(key: string) {
    this.translate.get(key).subscribe(message => {
      // Vous pouvez implémenter votre propre système de toast ici
      console.log('Success:', message);
    });
  }

  /**
   * Affiche un message d'erreur
   */
  private showErrorMessage(key: string) {
    this.translate.get(key).subscribe(message => {
      // Vous pouvez implémenter votre propre système de toast ici
      console.error('Error:', message);
    });
  }

  // ========== MÉTHODES POUR LE REDESIGN ==========

  /**
   * Retourne la classe CSS pour le type de notification
   */
  getTypeClass(type: string): string {
    const typeMap: any = {
      'INFO': 'info',
      'WARNING': 'warning',
      'SUCCESS': 'success',
      'ERROR': 'error'
    };
    return typeMap[type] || 'info';
  }

  /**
   * Retourne l'icône pour le type de notification
   */
  getTypeIcon(type: string): string {
    const iconMap: any = {
      'INFO': 'info',
      'WARNING': 'warning',
      'SUCCESS': 'check_circle',
      'ERROR': 'error'
    };
    return iconMap[type] || 'notifications';
  }

  /**
   * Retourne les initiales d'un nom
   */
  getInitials(name: string): string {
    if (!name) return '?';
    return name.split(' ')
      .map((n: string) => n[0])
      .join('')
      .toUpperCase()
      .substring(0, 2);
  }

  /**
   * Obtient la couleur de fond pour le type
   */
  getTypeColor(type: string): string {
    const colorMap: any = {
      'INFO': '#2196f3',
      'SUCCESS': '#4caf50',
      'WARNING': '#ff9800',
      'ERROR': '#f44336'
    };
    return colorMap[type] || '#9e9e9e';
  }
}
