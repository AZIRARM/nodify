import {Component, Inject, OnDestroy, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {Rule} from "../../../modeles/Rule";
import {ContentNode} from "../../../modeles/ContentNode";
import {TranslateService} from "@ngx-translate/core";
import {LoggerService} from "../../../services/LoggerService";
import {UserAccessService} from "../../../services/UserAccessService";
import { LockService } from 'src/app/services/LockService';
import { interval, Subscription } from 'rxjs';

@Component({
  selector: 'app-node-rules-conditions-dialog',
  templateUrl: './node-rules-conditions-dialog.component.html',
  styleUrls: ['./node-rules-conditions-dialog.component.css']
})
export class NodeRulesConditionsDialogComponent implements OnInit, OnDestroy {


  node: any;

  rulesConditions: Rule[] = [];

  ruleType: string = "BOOL";
    
  private lockCheckSub: Subscription;

  constructor(private translateService: TranslateService,
              private loggerService: LoggerService,
              public userAccessService: UserAccessService,
              public dialogRef: MatDialogRef<NodeRulesConditionsDialogComponent>,
              @Inject(MAT_DIALOG_DATA) public content: any,
              private lockService: LockService
  ) {
    if (content) {
      this.node = content;
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

        
        
        // 🔄 Vérifie le lock toutes les 10s
        this.lockCheckSub = interval(10000).subscribe(() => {
          this.lockService.getLockInfo(this.node.code).subscribe((lockInfo:any) => {
            if (lockInfo.locked) {
              this.translateService.get("RESOURCE_LOCKED_BY_OTHER")
                .subscribe(translation => this.loggerService.warn(translation));
              this.dialogRef.close();
            }
          });
        });

      }
    });

  }

  ngOnDestroy(): void {
    if (this.lockCheckSub) {
      this.lockCheckSub.unsubscribe();
    }
    this.lockService.release();
  }

  init() {
    this.rulesConditions = this.node.rules;
  }

  cancel() {
    this.dialogRef.close();
  }

  validate() {
    this.node.rules = this.rulesConditions;
    this.dialogRef.close({data: this.node});
  }


  create() {
    let ruleCondition = new Rule();
    ruleCondition.type = this.ruleType;
    ruleCondition.editable = true;
    ruleCondition.erasable = true;

    if(this.ruleType == 'DATE'){
      ruleCondition.operator = '=';
    }

    ruleCondition.code = this.generateString(this.node.code.split("-")[0]+"-"+this.ruleType, 20);
    ruleCondition.name = this.generateString(this.ruleType, 5);
    ruleCondition.behavior = false;

    if (this.ruleType === "BOOL")
      ruleCondition.value = 'true';
    if (!this.rulesConditions) {
      this.rulesConditions = [];
    }
    this.rulesConditions.push(ruleCondition)
  }

  remove(element: any) {
    this.rulesConditions = this.rulesConditions.filter(function (item) {
      return item !== element
    })
  }

  generateString(template:string, length: number) {
    const characters = '012345679';
    const charactersLength = characters.length;
    const array = new Uint32Array(length);
    window.crypto.getRandomValues(array);
    return  template+ "-" + Array.from(array, (num) => characters[num % charactersLength]).join('');
  }

  protected readonly Node = Node;
  protected readonly ContentNode = ContentNode;

  canSave() {
    let check: boolean = true;
    this.rulesConditions.forEach((rule: Rule) => {
      if (check) {
        if (!this.checkRule(rule)) {
          check = false;
        }
      }
    })

    return check;
  }

  private checkRule(rule: Rule) {
    if (rule.type == 'BOOL') {
      return this.checkCommonsFields(rule);
    } else {
      return this.checkCommonsFields(rule) && this.checkTypeDateFields(rule);
    }
  }

  private checkTypeDateFields(rule: Rule) {
    return true;
  }

  private checkCommonsFields(rule: Rule) {
    if (!rule.name) {
      this.translateService.get("NEED_RULE_NAME").subscribe((message:string)=>{
        this.loggerService.warn(message);
      });
    }
    return true;
  }

  updateBehavior(selected: Rule) {
    if(!selected.value){
      selected.enable = false;
    }
  }
}
