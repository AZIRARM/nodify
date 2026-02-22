import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatTableDataSource } from '@angular/material/table';
import { TranslateService } from '@ngx-translate/core';
import { ToastrService } from 'ngx-toastr';
import { LockService } from 'src/app/services/LockService';
import { LoggerService } from 'src/app/services/LoggerService';
import { UserAccessService } from 'src/app/services/UserAccessService';
import { NodeService } from 'src/app/services/NodeService';
import { ContentNodeService } from 'src/app/services/ContentNodeService';

@Component({
  selector: 'app-release-locks',
  templateUrl: './release-locks.component.html',
  styleUrls: ['./release-locks.component.css']
})
export class ReleaseLocksComponent implements OnInit {

  displayedColumns: string[] = ["Code", "Owner", "Locked", "Actions"];
  dataSource = new MatTableDataSource<any>([]);

  constructor(
    private translate: TranslateService,
    private toast: ToastrService,
    private loggerService: LoggerService,
    public userAccessService: UserAccessService,
    private nodeService: NodeService,
    private contentNodeService: ContentNodeService,
    public lockService: LockService,
    private dialog: MatDialog,
    private cdRef: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadLocks();
  }

  loadLocks(): void {
    console.log("loadLocks appelé");

    this.lockService.handleAllLocks().subscribe({
      next: (locks: any) => {
        console.log("Données reçues:", locks);

        if (Array.isArray(locks)) {
          this.dataSource.data = locks;
          this.cdRef.detectChanges();
          console.log("dataSource mis à jour:", this.dataSource.data);
        } else if (locks && Array.isArray(locks.data)) {
          // Si la réponse est un objet avec une propriété 'data'
          this.dataSource.data = locks.data;
          this.cdRef.detectChanges();
        }
      },
      error: (err: any) => {
        console.error("Erreur WebSocket:", err);
        this.toast.error(this.translate.instant("LOCKS_LOAD_FAIL"));
      }
    });
  }

  unlock(element: any) {
    this.lockService.adminRelease(element.resourceCode).subscribe({
      next: (success: boolean) => {
        if (success) {
          element.lockInfo = { owner: null, isOwner: false, locked: false };
          this.toast.success(this.translate.instant("LOCK_RELEASED_SUCCESS"));
          this.loadLocks();
        } else {
          this.toast.error(this.translate.instant("LOCK_RELEASED_FAIL"));
        }
      },
      error: (err: any) => {
        console.error("Erreur unlock:", err);
        this.toast.error(this.translate.instant("LOCK_RELEASED_FAIL"));
      }
    });
  }
}
