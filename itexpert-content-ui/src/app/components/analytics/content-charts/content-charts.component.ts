import {Component, OnInit} from "@angular/core";
import {EChartsOption} from "echarts";
import {ChartService} from "../../../services/ChartService";
import {TranslateService} from "@ngx-translate/core";
import {forkJoin, Observable, of, switchMap} from "rxjs";
import {map} from "rxjs/operators";

@Component({
  selector: 'app-content-charts',
  templateUrl: './content-charts.component.html',
  styleUrls: ['./content-charts.component.css']
})
export class ContentChartsComponent implements OnInit {

  chartOptions: EChartsOption = {};

  constructor(
    private chartService: ChartService,
    private translateService: TranslateService) {
  }

  ngOnInit(): void {
    this.chartService.getCharts().subscribe((treeData: any) => {
      this.cleanTreeData(treeData).subscribe((cleaned:any) => {
        this.chartOptions = {
          tooltip: {
            trigger: 'item',
            triggerOn: 'mousemove'
          },
          series: [
            {
              type: 'tree',
              data: [cleaned],
              top: '1%',
              left: '7%',
              bottom: '1%',
              right: '20%',
              symbolSize: 20, // Taille suffisante pour voir les icônes
              label: {
                position: 'left',
                verticalAlign: 'middle',
                align: 'right',
                fontSize: 12,
                formatter: (params: any) => {
                  const name = params.name || '';
                  if (name.startsWith('FEEDBACK')) return `{feedback|${name}}`;
                  if (name.startsWith('CLICKED')) return `{clicked|${name}}`;
                  if (name.startsWith('DISPLAYED')) return `{displayed|${name}}`;
                  return name;
                },
                rich: {
                  feedback: {
                    color: '#ffffff',
                    backgroundColor: '#007bff',
                    borderRadius: 4,
                    padding: [2, 4],
                    fontWeight: 'bold'
                  },
                  clicked: {
                    color: '#ffffff',
                    backgroundColor: '#28a745',
                    borderRadius: 4,
                    padding: [2, 4],
                    fontWeight: 'bold'
                  },
                  displayed: {
                    color: '#ffffff',
                    backgroundColor: '#ffc107',
                    borderRadius: 4,
                    padding: [2, 4],
                    fontWeight: 'bold'
                  }
                }
              },
              leaves: {
                label: {
                  position: 'right',
                  verticalAlign: 'middle',
                  align: 'left',
                  fontSize: 12
                }
              },
              expandAndCollapse: true,
              animationDuration: 550,
              animationDurationUpdate: 750
            }
          ]
        };
      });
    });
  }

  private cleanTreeData(node: any): Observable<any> {
    const shouldTranslate = [
      'FEEDBACKS', 'FEEDBACK', 'FEEDBACK_ALL',
      'FEEDBACK_VERIFIED', 'FEEDBACK_NOT_VERIFIED',
      'DISPLAYED', 'CLICKED'
    ];

    const isLeaf = !node.children || node.children.length === 0;
    const needsTranslation = shouldTranslate.includes(node.name);
    const icon = this.getIconForType(node);

    const formatNode = (name: string) => ({
      ...node,
      name: node.value ? `${name} : ${node.value}` : name,
      symbol: icon,
      label: {
        fontWeight: node.value && node.value !== '0' ? 'bold' : 'normal'
      }
    });

    if (isLeaf) {
      if (needsTranslation) {
        return this.translateService.get(node.name).pipe(
          map(trad => formatNode(trad))
        );
      }
      return of(formatNode(node.name));
    }

    const cleanedChildren$ = forkJoin(node.children.map((child: any) => this.cleanTreeData(child)));

    if (needsTranslation) {
      return this.translateService.get(node.name).pipe(
        switchMap((trad:any) => cleanedChildren$.pipe(
          map(children => ({
            ...node,
            name: trad,
            symbol: icon,
            children
          }))
        ))
      );
    }

    return cleanedChildren$.pipe(
      map(children => ({
        ...node,
        symbol: icon,
        children
      }))
    );
  }

  private getIconForType(node: any): string {
    if (!node.leaf && !node.type.includes('FEEDBACK')) return 'image://assets/icons/node.svg';

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
      case 'FEEDBACK':
        return 'image://assets/icons/feedback.svg';
      case 'FEEDBACK_ALL':
        return 'image://assets/icons/all.svg';
      case 'FEEDBACK_VERIFIED':
        return 'image://assets/icons/verified.svg';
      case 'FEEDBACK_NOT_VERIFIED':
        return 'image://assets/icons/not_verified.svg';
      case 'DISPLAYED':
        return 'image://assets/icons/display.svg';
      case 'CLICKED':
        return 'image://assets/icons/click.svg';
      case '0':
        return 'image://assets/icons/0.svg';
      case '1':
        return 'image://assets/icons/1.svg';
      case '2':
        return 'image://assets/icons/2.svg';
      case '3':
        return 'image://assets/icons/3.svg';
      case '4':
        return 'image://assets/icons/4.svg';
      case '5':
        return 'image://assets/icons/5.svg';
      default:
        return 'image://assets/icons/node.svg';
    }
  }
}
