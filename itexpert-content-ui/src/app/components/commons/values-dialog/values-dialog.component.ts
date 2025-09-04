import {Component, Inject, OnDestroy, OnInit} from '@angular/core';
import {Node} from "../../../modeles/Node";
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {Value} from "../../../modeles/Value";
import {MatTableDataSource} from "@angular/material/table";
import {UserAccessService} from "../../../services/UserAccessService";
import { LoggerService } from 'src/app/services/LoggerService';
import { LockService } from 'src/app/services/LockService';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-values-dialog',
  templateUrl: './values-dialog.component.html',
  styleUrls: ['./values-dialog.component.css']
})
export class ValuesDialogComponent implements OnInit, OnDestroy {

  node: Node;
  key: string;
  value: string;

  displayedColumns: string[] = ['Key', 'Value', 'Actions'];
  dataSource: MatTableDataSource<Value>;

  constructor(
    public dialogRef: MatDialogRef<ValuesDialogComponent>,
    public userAccessService: UserAccessService,
    private loggerService: LoggerService,
    private lockService: LockService,
    private translateService: TranslateService,
    @Inject(MAT_DIALOG_DATA) public content: any
  ) {
    if (content) {
      this.node = content;
      if (!this.node.values) {
        this.node.values = [];
      }
    }
  }

  
  ngOnInit() {
    this.init();
    
    // 🔒 Tente d’acquérir le lock en entrant dans l’édition
    this.lockService.acquire(this.node.code).subscribe(acquired => {
      if (!acquired) {
        
         this.translateService.get("RESOURCE_LOCKED")
            .subscribe(translation => {
              this.loggerService.warn(translation);
            });
        this.dialogRef.close();
      } else {
        // Si acquis → démarre la surveillance d’inactivité à 30 min
        this.lockService.startInactivityWatcher(30 * 60 * 1000, () => {

         this.translateService.get("RESOURCE_RELEASED")
            .subscribe(translation => {
              this.loggerService.warn(translation);
            });
          this.dialogRef.close();
        });
      }
    });
  }
  

  ngOnDestroy(): void {
    // Libère le lock proprement
    this.lockService.release();
  }

  cancel() {
    this.dialogRef.close();
  }

  validate() {
    this.dialogRef.close({data: this.node});
  }

  remove(key: string) {
    console.log('<--------------' + this.node.values);
    this.node.values = this.node.values.filter(v => v.key !== key);
    console.log('------------------->' + this.node.values);
    this.init();
  }

  add() {
    let values: any = this.node.values?.filter(val => val.key !== this.key);

    let val = new Value();
    val.key = this.key;
    val.value = this.value;

    values.push(val);

    this.node.values = values;
    this.init();
  }

  edit(val: Value) {
    this.key = val.key;
    this.value = val.value;
  }

  private init() {
    this.dataSource = new MatTableDataSource(this.node.values);
  }
}
