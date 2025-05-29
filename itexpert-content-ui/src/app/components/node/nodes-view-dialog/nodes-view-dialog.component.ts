import {Component, Inject, OnInit} from '@angular/core';
import type {EChartsCoreOption} from 'echarts/core';
import {NodeService} from "../../../services/NodeService";
import {Node} from "../../../modeles/Node";
import {MAT_DIALOG_DATA, MatDialog} from "@angular/material/dialog";
import {map} from 'rxjs/operators';
import {Observable} from "rxjs";

@Component({
  selector: 'app-nodes-view-dialog',
  templateUrl: './nodes-view-dialog.component.html',
  styleUrl: './nodes-view-dialog.component.css',
  standalone: false
})
export class NodesViewDialogComponent implements OnInit {

  options: Observable<EChartsCoreOption>;

  constructor(
    private nodeService: NodeService,
    @Inject(MAT_DIALOG_DATA) private node: Node,
    private dialog: MatDialog) {
  }


  ngOnInit(): void {
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
    if (!node.leaf) return 'image://assets/icons/node.svg';
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

      default:
         return 'image://assets/icons/node.svg'; // Par défaut
    }
  }

}
