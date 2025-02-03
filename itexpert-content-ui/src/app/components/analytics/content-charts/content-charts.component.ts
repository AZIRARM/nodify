import {Component, OnInit} from '@angular/core';
import {FeedbackService} from "../../../services/FeedbackService";
import {ContentClickService} from "../../../services/ContentClickService";
import {ContentDisplayService} from "../../../services/ContentDisplayService";
import {NodeService} from "../../../services/NodeService";
import {StatusEnum} from "../../../modeles/StatusEnum";
import {ContentNodeService} from "../../../services/ContentNodeService";

@Component({
  selector: 'app-content-charts',
  templateUrl: './content-charts.component.html',
  styleUrls: ['./content-charts.component.css']
})
export class ContentChartsComponent implements OnInit {

  contentCodeFilter: string = "all";

  viewFeedbacks: any = [400, 400];
  viewDisplays: any = [600, 400];
  viewClicks: any = [600, 400];

  // options
  gradient: boolean = true;
  showLegend: boolean = false;
  showLabels: boolean = true;
  isDoughnut: boolean = false;
  legendPosition: any = "below";


  colorScheme: any = {
    domain: ['#23d914', '#5462ef', 'rgba(229,187,58,0.56)', '#ec0a0a']
  };


  feedbackCharts: any = [];
  clickCharts: any = [];
  displayCharts: any = [];

  content: any;
  projects: any[] = [];
  nodes: any[] = [];
  contents: any[] = [];

  displayeds: any[] = [];

  selectedProject: any;
  selectedNode: any;
  selectedContent: any;

  constructor(
    private feedbackService: FeedbackService,
    private nodeService: NodeService,
    private contentNodeService: ContentNodeService,
    private contentClickService: ContentClickService,
    private contentDisplayService: ContentDisplayService
  ) {
  }

  ngOnInit(): void {
    this.init();
  }

  private init() {
    this.initProjects();
    this.showCharts();
  }

  onSelect(data: any): void {
    console.log('Item clicked', JSON.parse(JSON.stringify(data)));
    if (data.name && isNaN(data.name)) {
      this.contentCodeFilter = data.name;
    }
  }

  onActivate(data: any): void {
    console.log('Activate', JSON.parse(JSON.stringify(data)));
  }

  onDeactivate(data: any): void {
    console.log('Deactivate', JSON.parse(JSON.stringify(data)));
  }

  private initProjects() {
    this.nodeService.getParentsNodes(StatusEnum.SNAPSHOT).subscribe((nodes: any) => {
      this.projects = nodes;
    });
  }


  changeProject() {

    this.selectedNode = null;
    this.selectedContent = null;

    if (this.selectedProject) {
      this.nodeService.getAllDescendantsByParentCode(this.selectedProject.code).subscribe((nodes: any) => {
        this.nodes = nodes.filter((node:any)=>node.code !== this.selectedProject.code);
        this.showProjectCharts();
      });
    }
  }

  changeNode() {

    this.selectedContent = null;

    if (this.selectedNode) {
      this.contentNodeService.getAllByParentCodeAndStatus(this.selectedNode.code, StatusEnum.SNAPSHOT).subscribe((contents: any) => {
        this.contents = contents;
        this.showNodeCharts();
      });
    } else {
      this.showProjectCharts();
    }
  }

  changeContent() {
    if (this.selectedContent) {
      this.showContentCharts();
    } else {
      this.showNodeCharts();
    }
  }


  private showCharts() {
    this.feedbacks();
    this.clicks();
    this.displays();
  }


  private showProjectCharts() {
    this.feedbacksProject();
    this.clicksProject();
    this.displaysProject();
  }

  private showNodeCharts() {
    this.feedbacksNode();
    this.clicksNode();
    this.displaysNode();
  }

  private showContentCharts() {
    this.feedbacksContent();
    this.clicksContent();
    this.displaysContent();
  }


  //charts
  displays() {
    this.displayCharts = [];
    this.contentDisplayService.getCharts().subscribe((charts: any) => {
      this.displayCharts.push({name: 'ALL', data: charts});
    });
  }
  feedbacks() {
    this.feedbackCharts = [];
    this.feedbackService.getCharts().subscribe((charts: any) => {
      this.feedbackCharts.push({name: 'ALL', data: charts});
    });
  }
  clicks() {
    this.clickCharts = [];
    this.contentClickService.getCharts().subscribe((charts: any) => {
      this.clickCharts.push({name: 'ALL', data: charts});
    });
  }


  displaysProject() {
    this.displayCharts = [];
    this.contentDisplayService.getChartsNode(this.selectedProject.code).subscribe((charts: any) => {
      this.displayCharts.push({name: this.selectedProject.name+"("+this.selectedProject.code+")", data: charts});
    });
  }
  feedbacksProject() {
    this.feedbackCharts = [];
    this.feedbackService.getChartsNode(this.selectedProject.code).subscribe((charts: any) => {
      this.feedbackCharts.push({name: this.selectedProject.name+"("+this.selectedProject.code+")", data: charts});
    });
  }
  clicksProject() {
    this.clickCharts = [];
    this.contentClickService.getChartsNode(this.selectedProject.code).subscribe((charts: any) => {
      this.clickCharts.push({name: this.selectedProject.name+"("+this.selectedProject.code+")", data: charts});
    });
  }

  displaysNode() {
    this.displayCharts = [];
    this.contentDisplayService.getChartsNode(this.selectedNode.code).subscribe((charts: any) => {
      this.displayCharts.push({name: this.selectedNode.name+"("+this.selectedNode.code+")", data: charts});
    });
  }
  feedbacksNode() {
    this.feedbackCharts = [];
    this.feedbackService.getChartsNode(this.selectedNode.code).subscribe((charts: any) => {
      this.feedbackCharts.push({name: this.selectedNode.name+"("+this.selectedNode.code+")", data: charts});
    });
  }
  clicksNode() {
    this.clickCharts = [];
    this.contentClickService.getChartsNode(this.selectedNode.code).subscribe((charts: any) => {
      this.clickCharts.push({name: this.selectedNode.name+"("+this.selectedNode.code+")", data: charts});
    });
  }


  displaysContent() {
    this.displayCharts = [];
    this.contentDisplayService.getChartsContent(this.selectedContent.code).subscribe((charts: any) => {
      this.displayCharts.push({name: this.selectedContent.description+"("+this.selectedContent.code+")", data: charts});
    });
  }
  feedbacksContent() {
    this.feedbackCharts = [];
    this.feedbackService.getChartsContent(this.selectedContent.code).subscribe((charts: any) => {
      this.feedbackCharts.push({name: this.selectedContent.description+"("+this.selectedContent.code+")", data: charts});
    });
  }
  clicksContent() {
    this.clickCharts = [];
    this.contentClickService.getChartsContent(this.selectedContent.code).subscribe((charts: any) => {
      this.clickCharts.push({name: this.selectedContent.description+"("+this.selectedContent.code+")", data: charts});
    });
  }

  initFilter() {
    this.selectedContent = null;
    this.selectedNode = null;
    this.selectedProject = null;
    this.displayCharts = null;
    this.clickCharts = null;
    this.feedbackCharts = null;

    this.showCharts()
  }

  onSelectClick(clickChart: any, $event: any) {

  }

  onSelectDisplay($event: any) {
    this.selectedContent = {code: $event.label};
    this.selectedNode = null;
    this.selectedProject = null;
    this.displayCharts = null;
    this.clickCharts = null;
    this.feedbackCharts = null;
    this.showContentCharts();
  }

  onSelectFeedback($event: any) {

  }
}
