import {
  Component,
  OnInit,
  ViewChild,
  inject,
  signal,
  DestroyRef
} from '@angular/core';

import { MatTableDataSource } from "@angular/material/table";
import { MatDialog } from "@angular/material/dialog";
import { MatPaginator } from "@angular/material/paginator";
import { MatSlideToggle } from '@angular/material/slide-toggle';

import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ValidationDialogComponent } from "../../commons/validation-dialog/validation-dialog.component";
import { NotificationService } from "../../../services/NotificationService";
import { Notification } from "../../../modeles/Notification";
import { UserAccessService } from "../../../services/UserAccessService";
import { AuthenticationService } from 'src/app/services/AuthenticationService';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-notifications',
  templateUrl: './notifications.component.html',
  styleUrls: ['./notifications.component.css'],
  standalone: false
})
export class NotificationsComponent implements OnInit {

  private notificationService = inject(NotificationService);
  private userAccessService = inject(UserAccessService);
  private authenticationService = inject(AuthenticationService);
  private translate = inject(TranslateService);
  private dialog = inject(MatDialog);
  private destroyRef = inject(DestroyRef);

  user = signal<any>(null);

  displayedColumns: string[] = [
    'Date',
    'Type',
    'Code',
    'Message',
    'Version',
    'By',
    'Actions'
  ];

  dataSource = new MatTableDataSource<Notification>([]);

  @ViewChild(MatPaginator)
  paginator!: MatPaginator;

  @ViewChild('markReadToggle')
  markReadToggle!: MatSlideToggle;

  total = signal(0);
  pageSize = signal(10);
  pageIndex = signal(0);

  pageSizeOptions = [5, 10, 20, 50];

  unreadCount = signal(0);

  filterType = signal('all');
  searchText = signal('');

  ngOnInit() {
    this.user.set(this.userAccessService.getCurrentUser());
    this.loadNotifications();
  }

  loadNotifications() {

    if (!this.authenticationService.isAuthenticated()) {
      return;
    }

    this.notificationService.connectWebSocket(
      this.authenticationService.getAccessToken(),
      this.pageIndex(),
      this.pageSize()
    )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({

        next: (data: any) => {

          this.total.set(data.count || 0);
          this.dataSource.data = data.unread || [];
          this.unreadCount.set(this.dataSource.data.length);

          if (this.paginator) {
            this.paginator.length = this.total();
            this.paginator.pageIndex = this.pageIndex();
            this.paginator.pageSize = this.pageSize();
          }

          if (this.filterType() !== 'all') {
            this.applyFilter();
          }

        },

        error: () => {
          this.dataSource.data = [];
          this.total.set(0);
          this.unreadCount.set(0);
        }

      });

  }

  onPageChange(event: any) {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadNotifications();
  }

  markAsReaded(element: Notification) {

    this.notificationService.markAsReaded(element.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({

        next: () => {

          this.dataSource.data =
            this.dataSource.data.filter(n => n.id !== element.id);

          this.unreadCount.set(this.dataSource.data.length);

          this.loadNotifications();

        }

      });

  }

  markAllAsReaded(event: any) {

    event.source.checked = false;

    if (!this.dataSource.data.length) return;

    this.translate.get("MARK_ALL_READ_CONFIRM_TITLE")
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(title => {

        this.translate.get("MARK_ALL_READ_CONFIRM_MESSAGE")
          .subscribe(message => {

            const dialogRef = this.dialog.open(ValidationDialogComponent, {
              data: { title, message },
              disableClose: true
            });

            dialogRef.afterClosed()
              .subscribe(result => {

                if (result?.data === 'validated') {

                  this.notificationService.markAllReaded()
                    .subscribe(() => {

                      this.dataSource.data = [];
                      this.unreadCount.set(0);

                      this.loadNotifications();

                    });

                }

              });

          });

      });

  }

  allreadyReaded() {
    this.pageIndex.set(0);
    this.pageSize.set(10);
    this.filterType.set('all');
    this.searchText.set('');
    this.loadNotifications();
  }

  refreshNotifications() {
    this.loadNotifications();
  }

  filterBy(type: string) {
    this.filterType.set(type);
    this.pageIndex.set(0);
    this.applyFilter();
  }

  applyFilter() {

    if (this.filterType() !== 'all') {

      this.dataSource.data =
        this.dataSource.data.filter(n =>
          n.type?.toLowerCase() === this.filterType().toLowerCase()
        );

    }

    if (this.searchText()) {
      this.dataSource.filter =
        this.searchText().trim().toLowerCase();
    }

  }

  onSearch(event: any) {
    this.searchText.set(event.target.value);
    this.applyFilter();
  }

  viewDetails(element: Notification) {

    this.translate.get("NOTIFICATION_DETAILS_TITLE")
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(title => {

        const typeValue = element.type ? this.translate.instant(element.type) : '';
        const codeValue = element.code ? this.translate.instant(element.code) : '';

        const message = `
          <strong>${this.translate.instant('TYPE')}:</strong> ${typeValue}<br>
          <strong>${this.translate.instant('CODE')}:</strong> ${element.typeCode || ''}<br>
          <strong>${this.translate.instant('DESCRIPTION')}:</strong> ${codeValue}<br>
          <strong>${this.translate.instant('VERSION')}:</strong> ${element.typeVersion || ''}<br>
          <strong>${this.translate.instant('DATE')}:</strong> ${element.date ? new Date(element.date).toLocaleString() : ''}<br>
        `;

        const dialogRef = this.dialog.open(ValidationDialogComponent, {
          data: {
            title,
            message,
            isHtml: true
          },
          width: '500px'
        });

        dialogRef.afterClosed()
          .subscribe(result => {

            if (result?.data === 'validated') {
              this.markAsReaded(element);
            }

          });

      });

  }

  deleteNotification(element: Notification) {

    this.translate.get("DELETE_NOTIFICATION_TITLE")
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(title => {

        this.translate.get("DELETE_NOTIFICATION_CONFIRM")
          .subscribe(message => {

            const dialogRef = this.dialog.open(ValidationDialogComponent, {
              data: { title, message },
              disableClose: true
            });

            dialogRef.afterClosed()
              .subscribe(result => {

                if (result?.data === 'validated') {

                  this.notificationService.markAsReaded(element.id)
                    .subscribe(() => {

                      this.loadNotifications();

                    });

                }

              });

          });

      });

  }

  getTypeClass(type: string): string {

    const typeMap: any = {
      INFO: 'info',
      WARNING: 'warning',
      SUCCESS: 'success',
      ERROR: 'error'
    };

    return typeMap[type] || 'info';

  }

  getTypeIcon(type: string): string {

    const iconMap: any = {
      INFO: 'info',
      WARNING: 'warning',
      SUCCESS: 'check_circle',
      ERROR: 'error'
    };

    return iconMap[type] || 'notifications';

  }

  getInitials(name: string): string {

    if (!name) return '?';

    return name.split(' ')
      .map((n: string) => n[0])
      .join('')
      .toUpperCase()
      .substring(0, 2);

  }

  getTypeColor(type: string): string {

    const colorMap: any = {
      INFO: '#2196f3',
      SUCCESS: '#4caf50',
      WARNING: '#ff9800',
      ERROR: '#f44336'
    };

    return colorMap[type] || '#9e9e9e';

  }

}