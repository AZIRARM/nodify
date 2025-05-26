import {Component, Inject, OnInit} from '@angular/core';
import type { EChartsCoreOption } from 'echarts/core';
import {NodeService} from "../../../services/NodeService";
import {Node} from "../../../modeles/Node";
import {MAT_DIALOG_DATA, MatDialog} from "@angular/material/dialog";

@Component({
  selector: 'app-nodes-view-dialog',
  templateUrl: './nodes-view-dialog.component.html',
  styleUrl: './nodes-view-dialog.component.css',
  standalone: false
})
export class NodesViewDialogComponent implements OnInit {

  options: EChartsCoreOption;

  constructor(
    private nodeService: NodeService,
    @Inject(MAT_DIALOG_DATA) private node: Node,
    private dialog: MatDialog) {
  }


  ngOnInit(): void {
    this.nodeService.getNodeView(this.node.code).subscribe(treeData => {
      this.options = {
        series: [
          {
            type: 'tree',
            data: [treeData], // arbre racine
            top: '5%',
            left: '20%',
            bottom: '5%',
            right: '20%',
            symbolSize: 10,
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
            initialTreeDepth: 3,
            animationDuration: 550,
            animationDurationUpdate: 750
          }
        ]
      };
    });
  }
}
