import { Component, OnDestroy, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatTableDataSource } from '@angular/material/table';
import { TranslateService } from '@ngx-translate/core';
import { ToastrService } from 'ngx-toastr';
import { LockService } from 'src/app/services/LockService';
import { LoggerService } from 'src/app/services/LoggerService';
import { UserAccessService } from 'src/app/services/UserAccessService';
import { NodeService } from 'src/app/services/NodeService';
import { ContentNodeService } from 'src/app/services/ContentNodeService';
import { interval, Subscription } from 'rxjs';

@Component({
  selector: 'app-release-locks',
  templateUrl: './release-locks.component.html',
  styleUrls: ['./release-locks.component.css']
})
export class ReleaseLocksComponent implements OnInit, OnDestroy {

  displayedColumns: string[] = ["Code", "Owner", "Locked", "Actions"];
  dataSource = new MatTableDataSource<any>([]);

  private refreshSub?: Subscription;


  constructor(
    private translate: TranslateService,
    private toast: ToastrService,
    private loggerService: LoggerService,
    public userAccessService: UserAccessService,
    private nodeService: NodeService,
    private contentNodeService: ContentNodeService,
    public lockService: LockService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    // 🔹 Charger une première fois
    this.loadLocks();

    // 🔹 Relancer toutes les 10 secondes
    this.refreshSub = interval(10000).subscribe(() => {
      this.loadLocks();
    });
  }

  ngOnDestroy(): void {
    // 🔹 Évite les fuites mémoire
    if (this.refreshSub) {
      this.refreshSub.unsubscribe();
    }
  }

  private loadLocks(): void {
    this.lockService.getAll().subscribe({
      next: (locks: any[]) => {
        // ⚠️ à adapter selon ce que ton backend renvoie (LockInfo + nodeId)
        this.dataSource.data = locks;
      },
      error: err => {
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
      error: (err:any) => {
        this.toast.error(this.translate.instant("LOCK_RELEASED_FAIL"));
      }
    });
  }
}
