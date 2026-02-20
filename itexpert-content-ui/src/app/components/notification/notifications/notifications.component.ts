import {AfterViewInit, Component, OnInit, OnDestroy, ViewChild} from '@angular/core';
import {MatTableDataSource} from "@angular/material/table";
import {MatDialogRef} from "@angular/material/dialog";
import {ValidationDialogComponent} from "../../commons/validation-dialog/validation-dialog.component";
import {NotificationService} from "../../../services/NotificationService";
import {Notification} from "../../../modeles/Notification";
import {MatPaginator} from "@angular/material/paginator";
import {UserAccessService} from "../../../services/UserAccessService";
import { AuthenticationService } from 'src/app/services/AuthenticationService';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-notifications',
  templateUrl: './notifications.component.html',
  styleUrls: ['./notifications.component.css']
})
export class NotificationsComponent implements OnInit, OnDestroy {

  user: any;
  displayedColumns: string[] = ['Date', 'Type', 'Code', 'Message', 'Version', 'By', 'Actions'];

  dialogRef: MatDialogRef<ValidationDialogComponent>;

  @ViewChild(MatPaginator)
  paginator!: MatPaginator;

  dataSource: MatTableDataSource<Notification> = new MatTableDataSource<Notification>([]);

  total: number = 0;
  pageSize: number = 10;
  pageIndex: number = 0;
  pageSizeOptions: number[] = [5, 10, 20, 50];

  private wsSubscription: Subscription | null = null;

  constructor(
    private notificationService: NotificationService,
    private userAccessService: UserAccessService,
    private authenticationService: AuthenticationService
  ) {}

  ngOnInit() {
    this.user = this.userAccessService.getCurrentUser();
    this.loadNotifications();
  }

  ngOnDestroy() {
    this.closeWebSocket();
  }

  closeWebSocket() {
    if (this.wsSubscription) {
      this.wsSubscription.unsubscribe();
      this.wsSubscription = null;
    }
  }

  loadNotifications() {
    if (!this.authenticationService.isAuthenticated()) {
      return;
    }

    // Fermer l'ancienne connexion WebSocket
    this.closeWebSocket();

    console.log(`Chargement notifications - page: ${this.pageIndex}, size: ${this.pageSize}`);

    this.wsSubscription = this.notificationService.connectWebSocket(
      this.authenticationService.getAccessToken(),
      this.pageIndex,
      this.pageSize
    ).subscribe({
      next: (data: any) => {
        console.log('Données reçues:', data);
        this.total = data.count || 0;
        this.dataSource.data = data.unread || [];

        // Mettre à jour le paginator après réception des données
        if (this.paginator) {
          this.paginator.length = this.total;
          this.paginator.pageIndex = this.pageIndex;
          this.paginator.pageSize = this.pageSize;
        }
      },
      error: (err: any) => {
        console.error("Erreur WebSocket:", err);
        this.dataSource.data = [];
      },
      complete: () => {
        console.log("WebSocket fermé");
        this.wsSubscription = null;
      }
    });
  }

  onPageChange(event: any) {
    console.log('Changement de page:', event);
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadNotifications();
  }

  markAsReaded(element: Notification) {
    this.notificationService.markAsReaded(element.id).subscribe({
      next: () => {
        console.log('Notification marquée comme lue');
        this.loadNotifications(); // Recharger après modification
      },
      error: (error: any) => {
        console.error('Erreur marquage lu:', error);
      }
    });
  }

  markAllAsReaded() {
    this.notificationService.markAllReaded().subscribe({
      next: () => {
        console.log('Toutes les notifications marquées comme lues');
        this.loadNotifications();
      },
      error: (error: any) => {
        console.error('Erreur marquage tout lu:', error);
      }
    });
  }

  allreadyReaded() {
    this.pageIndex = 0;
    this.pageSize = 10;
    this.loadNotifications();
  }
}
