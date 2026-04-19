import { Component, Inject, OnDestroy, OnInit, inject, signal, WritableSignal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { Rule } from "../../../modeles/Rule";
import { ContentNode } from "../../../modeles/ContentNode";
import { TranslateService } from "@ngx-translate/core";
import { LoggerService } from "../../../services/LoggerService";
import { UserAccessService } from "../../../services/UserAccessService";
import { LockService } from 'src/app/services/LockService';
import { interval, Subscription } from 'rxjs';
import { AuthenticationService } from "../../../services/AuthenticationService";

@Component({
  selector: 'app-node-rules-conditions-dialog',
  templateUrl: './node-rules-conditions-dialog.component.html',
  styleUrls: ['./node-rules-conditions-dialog.component.css'],
  standalone: false
})
export class NodeRulesConditionsDialogComponent implements OnInit, OnDestroy {

  node: any;
  rulesConditions: WritableSignal<Rule[]> = signal<Rule[]>([]);
  ruleType: WritableSignal<string> = signal("BOOL");
  private lockCheckSub?: Subscription;

  private translateService = inject(TranslateService);
  private loggerService = inject(LoggerService);
  public userAccessService = inject(UserAccessService);
  private authenticationService = inject(AuthenticationService);
  public dialogRef = inject(MatDialogRef<NodeRulesConditionsDialogComponent>);
  private lockService = inject(LockService);
  private content = inject(MAT_DIALOG_DATA);

  constructor() {
    if (this.content) {
      this.node = this.content;
    }
  }

  ngOnInit() {
    this.init();

    this.lockService.acquire(this.node.code).subscribe(acquired => {
      if (!acquired) {
        this.translateService.get("RESOURCE_LOCKED")
          .subscribe(translation => {
            this.loggerService.warn(translation);
          });
        this.dialogRef.close();
      } else {
        this.lockService.startInactivityWatcher(30 * 60 * 1000, () => {
          this.translateService.get("RESOURCE_RELEASED")
            .subscribe(translation => {
              this.loggerService.warn(translation);
            });
          this.dialogRef.close();
        });

        this.lockService.getLockInfoSocket(this.node.code, this.authenticationService.getAccessToken()).subscribe((lockInfo: any) => {
          if (lockInfo.locked) {
            this.translateService.get("RESOURCE_LOCKED_BY_OTHER")
              .subscribe(translation => this.loggerService.warn(translation));
            this.dialogRef.close();
          }
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
    this.rulesConditions.set(this.node.rules);
  }

  cancel() {
    this.dialogRef.close();
  }

  validate() {
    this.node.rules = this.rulesConditions();
    this.dialogRef.close({ data: this.node });
  }

  create() {
    let ruleCondition = new Rule();
    ruleCondition.type = this.ruleType();
    ruleCondition.editable = true;
    ruleCondition.erasable = true;

    if (this.ruleType() == 'DATE') {
      ruleCondition.operator = '=';
    }

    ruleCondition.code = this.generateString(this.node.code.split("-")[0] + "-" + this.ruleType(), 20);
    ruleCondition.name = this.generateString(this.ruleType(), 5);
    ruleCondition.behavior = false;

    if (this.ruleType() === "BOOL")
      ruleCondition.value = 'true';

    const currentRules = this.rulesConditions();
    if (!currentRules) {
      this.rulesConditions.set([ruleCondition]);
    } else {
      this.rulesConditions.set([...currentRules, ruleCondition]);
    }
  }

  remove(element: any) {
    const currentRules = this.rulesConditions();
    this.rulesConditions.set(currentRules.filter(item => item !== element));
  }

  generateString(template: string, length: number) {
    const characters = '012345679';
    const charactersLength = characters.length;
    const array = new Uint32Array(length);
    window.crypto.getRandomValues(array);
    return template + "-" + Array.from(array, (num) => characters[num % charactersLength]).join('');
  }

  protected readonly Node = Node;
  protected readonly ContentNode = ContentNode;

  canSave() {
    let check: boolean = true;
    const currentRules = this.rulesConditions();
    for (const rule of currentRules) {
      if (check && !this.checkRule(rule)) {
        check = false;
      }
    }
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
      this.translateService.get("NEED_RULE_NAME").subscribe((message: string) => {
        this.loggerService.warn(message);
      });
    }
    return true;
  }

  updateBehavior(selected: Rule) {
    if (!selected.value) {
      selected.enable = false;
    }
  }
}