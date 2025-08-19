import {Component, Inject, OnInit} from '@angular/core';
import type {EChartsCoreOption} from 'echarts/core';
import {NodeService} from "../../../services/NodeService";
import {Node} from "../../../modeles/Node";
import {MAT_DIALOG_DATA, MatDialog, MatDialogRef} from "@angular/material/dialog";
import {map} from 'rxjs/operators';
import {Observable} from "rxjs";
import {ContentCodeComponent} from "../../content-node/content-code/content-code.component";
import {ContentNode} from "../../../modeles/ContentNode";
import {ContentNodeService} from "../../../services/ContentNodeService";
import {StatusEnum} from "../../../modeles/StatusEnum";
import {User} from "../../../modeles/User";
import {UserAccessService} from "../../../services/UserAccessService";

@Component({
  selector: 'app-nodes-view-dialog',
  templateUrl: './nodes-view-dialog.component.html',
  styleUrl: './nodes-view-dialog.component.css',
  standalone: false
})
export class NodesViewDialogComponent implements OnInit {

  options: Observable<EChartsCoreOption>;

  user: User;

  constructor(
    private nodeService: NodeService,
    private contentNodeService: ContentNodeService,
    private userAccessService: UserAccessService,
    @Inject(MAT_DIALOG_DATA) private node: Node,
    private dialog: MatDialog) {
  }


  ngOnInit() {
   this.user = this.userAccessService.getCurrentUser()

    this.options = this.nodeService.getNodeView(this.node.code).pipe(
      map(treeNode => this.buildChartOptions(treeNode))
    );
  }

  buildChartOptions(tree: any): EChartsCoreOption {
    const enrichedTree = this.addIconsToTree(tree);

    return {
      tooltip: {
        trigger: 'item',
        triggerOn: 'mousemove'
      },
      series: [
        {
          type: 'tree',
          data: [enrichedTree],
          top: '1%',
          left: '10%',
          bottom: '1%',
          right: '20%',
          symbolSize: 20,
          label: {
            position: 'left',
            verticalAlign: 'middle',
            align: 'right',
            fontSize: 12
          },
          leaves: {
            label: {
              position: 'right',
              verticalAlign: 'middle',
              align: 'left'
            }
          },
          expandAndCollapse: true,
          animationDuration: 550,
          animationDurationUpdate: 750
        }
      ]
    };
  }

  addIconsToTree(node: any): any {
    node.symbol = this.getIconForType(node);

    if (node.children && node.children.length > 0) {
      node.children = node.children.map((child: any) => this.addIconsToTree(child));
    }

    return node;
  }


  getIconForType(node: any): string {
    if (!node.leaf && node.type && !node.type.includes('NODIFY')) return 'image://assets/icons/node.svg';
    switch (node.type) {
      case 'XML':
        return 'image://assets/icons/xml.svg';
      case 'JSON':
        return 'image://assets/icons/json.svg';
      case 'HTML':
        return 'image://assets/icons/html.svg';
      case 'PICTURE':
        return 'image://assets/icons/picture.svg';
      case 'SCRIPT':
        return 'image://assets/icons/programming.svg';
      case 'STYLE':
        return 'image://assets/icons/css.svg';
      case 'FILE':
        return 'image://assets/icons/file.svg';
      case 'URLS':
        return 'image://assets/icons/url.svg';
      case 'NODIFY':
        return 'image://assets/icons/nodify_ai.png';

      default:
        return 'image://assets/icons/node.svg'; // Par défaut
    }
  }


  chartInstance: any;
  dialogRefCode: MatDialogRef<ContentCodeComponent>;

  onChartInit(ec: any): void {
    this.chartInstance = ec;
    this.chartInstance.on('dblclick', (params: any) => {
      const type = params.data?.type;
      const code = params.data?.code;

      if (type && code) {
        this.contentNodeService.getByCodeAndStatus(code, StatusEnum.SNAPSHOT).subscribe((content: any) => {
          this.nodeService.getNodeByCodeAndStatus(content.parentCode, StatusEnum.SNAPSHOT).subscribe((node: any) => {
            this.gotoElement(content, node);
          });
        });
      }
    });
  }

  gotoElement(content: ContentNode, node: Node) {

    this.dialogRefCode = this.dialog.open(ContentCodeComponent, {
      data: {
        node: node,
        contentNode: content,
        type: content.type,
        user: this.user
      },
      height: '100%',
      width: '90vw',
      disableClose: true
    });
    this.dialogRefCode.afterClosed()
      .subscribe((data: any) => {
        if (data.refresh) {
          //this.init();
        }
      });
  }

}
