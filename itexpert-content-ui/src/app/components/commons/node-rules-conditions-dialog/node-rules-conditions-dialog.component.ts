import {Component, Inject} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {ToastrService} from "ngx-toastr";
import {MAT_DIALOG_DATA, MatDialog, MatDialogRef} from "@angular/material/dialog";
import {Rule} from "../../../modeles/Rule";
import {ValidationDialogComponent} from "../validation-dialog/validation-dialog.component";
import {ContentNode} from "../../../modeles/ContentNode";

@Component({
  selector: 'app-node-rules-conditions-dialog',
  templateUrl: './node-rules-conditions-dialog.component.html',
  styleUrls: ['./node-rules-conditions-dialog.component.css']
})
export class NodeRulesConditionsDialogComponent {


  node: any;

  rulesConditions: Rule[] = [];

  ruleType: string;


  dialogRefCheck: MatDialogRef<ValidationDialogComponent>;

  constructor(private translate: TranslateService,
              private toast: ToastrService,
              public dialogRef: MatDialogRef<NodeRulesConditionsDialogComponent>,
              @Inject(MAT_DIALOG_DATA) public content: any,
              private dialog: MatDialog
  ) {
    if (content) {
      this.node = content;
    }
  }

  ngOnInit() {
    this.init();
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
    if(this.ruleType === "BOOL")
      ruleCondition.value = 'true';

    if(!this.rulesConditions) {
      this.rulesConditions = [];
    }
    this.rulesConditions.push(ruleCondition)
  }
  remove(element:any) {
    this.rulesConditions = this.rulesConditions.filter(function(item) {
      return item !== element
    })
  }
  generateCode(selected:any) {
    if(selected){
      selected.code = selected.name.replace(/[\W_]+/g,"_").toUpperCase()+'-'+(new Date()).getTime();
    }
  }

  protected readonly Node = Node;
  protected readonly ContentNode = ContentNode;
}
