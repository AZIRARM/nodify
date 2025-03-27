import {Component, Inject, OnDestroy, OnInit} from '@angular/core';
import {Node} from "../../../modeles/Node";
import {TranslateService} from "@ngx-translate/core";
import {ToastrService} from "ngx-toastr";
import {MAT_DIALOG_DATA, MatDialog, MatDialogRef} from "@angular/material/dialog";
import {ContentNodeService} from "../../../services/ContentNodeService";
import {ContentNode} from "../../../modeles/ContentNode";
import {ValidationDialogComponent} from "../../commons/validation-dialog/validation-dialog.component";
import {MatTableDataSource} from "@angular/material/table";
import {User} from "../../../modeles/User";
import {LoggerService} from "../../../services/LoggerService";
import {ContentUrl} from "../../../modeles/ContentUrl";
import {
  NodeRulesConditionsDialogComponent
} from "../../commons/node-rules-conditions-dialog/node-rules-conditions-dialog.component";
import {ValuesDialogComponent} from "../../commons/values-dialog/values-dialog.component";
import {NodeAccessRolesDialogComponent} from "../../node/node-access-roles-dialg/node-access-roles-dialog.component";
import {StatusEnum} from "../../../modeles/StatusEnum";
import {
  PublishedContentsNodesDialogComponent
} from "../published-contents-nodes-dialog/published-contents-nodes-dialog.component";
import {TranslationsDialogComponent} from "../../commons/translations-dialog/translations-dialog.component";
import {UserService} from "../../../services/UserService";
import {UserAccessService} from "../../../services/UserAccessService";
import {Env} from "../../../../assets/configurations/environment";
import {
  DeletedContentsNodesDialogComponent
} from "../deleted-contents-nodes-dialog/deleted-contents-nodes-dialog.component";
import {NodeService} from "../../../services/NodeService";
import {ContentDatasComponent} from "../content-datas/content-datas.component";
import {DataService} from "../../../services/DataService";
import {ContentCodeComponent} from "../content-code/content-code.component";

@Component({
  selector: 'app-node-content-dialog',
  templateUrl: './content-node-dialog.component.html',
  styleUrls: ['./content-node-dialog.component.css']
})
export class ContentNodeDialogComponent implements OnInit, OnDestroy {
  user: User;

  node: Node;

  type: string;
  currentContent: ContentNode;

  environments: Node[];


  mapDatas: Map<String, boolean> = new Map();

  dialogRefPublish: MatDialogRef<ValidationDialogComponent>;

  displayedColumns: string[] = ['Status', 'Type', 'Version', 'Last Modification', 'Modified by', 'Translations', 'Rules', 'Values', 'Publication', 'Actions'];
  dataSource: MatTableDataSource<ContentNode>;

  dialogRefValues: MatDialogRef<ValuesDialogComponent>;
  dialogRefRules: MatDialogRef<NodeRulesConditionsDialogComponent>;
  dialogRefAccessRoles: MatDialogRef<NodeAccessRolesDialogComponent>;
  dialogRefDeleteds: MatDialogRef<DeletedContentsNodesDialogComponent>;
  dialogRefPublished: MatDialogRef<PublishedContentsNodesDialogComponent>;
  dialogRefTranslations: MatDialogRef<TranslationsDialogComponent>;
  dialogRefDelete: MatDialogRef<ValidationDialogComponent>;
  dialogRefDatas: MatDialogRef<ContentDatasComponent>;
  dialogRefCode: MatDialogRef<ContentCodeComponent>;


  constructor(private translate: TranslateService,
              private toast: ToastrService,
              private loggerService: LoggerService,
              private contentNodeService: ContentNodeService,
              private dataService: DataService,
              public userAccessService: UserAccessService,
              public nodeService: NodeService,
              public dialogRef: MatDialogRef<ContentNodeDialogComponent>,
              @Inject(MAT_DIALOG_DATA) public content: Node,
              private userService: UserService,
              private dialog: MatDialog
  ) {
    if (content) {
      this.node = content;
    }
  }

  ngOnInit() {
    this.user = JSON.parse(
      JSON.parse(
        JSON.stringify((window.localStorage.getItem('userInfo')))
      )
    );
    this.init();
  }

  ngOnDestroy(): void {
  }

  init() {
    this.currentContent = null as any;
    this.mapDatas.clear();

    this.contentNodeService.getAllByParentCodeAndStatus(this.node.code, StatusEnum.SNAPSHOT).subscribe(
      (response: any) => {
        //next() callback
        response.map((node: any) => this.setUserName(node)).map((node: any) => this.setUserName(node));
        this.dataSource = new MatTableDataSource(response);
        this.initEnvironments();
      },
      (error) => {                              //error() callback
        this.toast.error('Request failed with error');
      });
  }

  private initEnvironments() {
    this.nodeService.getAllParentOrigin().subscribe(
      (response: any) => {
        this.environments = response.filter((env: Node) => this.user.roles.includes("ADMIN") || this.user.projects.includes(env.code));
      },
      (error) => {
        console.error('Request failed with error');
      });
  }

  validate() {
    this.save(this.currentContent)
  }


  create() {

    if (this.currentContent) {
      this.dialogRefPublish = this.dialog.open(ValidationDialogComponent, {
        data: {
          title: "CANCEL_MODIFICATIONS_TITLE",
          message: "CANCEL_MODIFICATIONS_MESSAGE"
        },
        height: '80vh',
        width: '80vw',
        disableClose: true
      });
      this.dialogRefPublish.afterClosed()
        .subscribe(result => {
          if (result.data === 'validated') {
            this.createNewContent();
          }
        });
    } else {
      this.createNewContent();
    }
  }


  publish(content: ContentNode) {
    this.dialogRefPublish = this.dialog.open(ValidationDialogComponent, {
      data: {
        title: "PUBLISH_PROJECT_TITLE",
        message: "PUBLISH_PROJECT_MESSAGE"
      },
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    this.dialogRefPublish.afterClosed()
      .subscribe(result => {

        if (result && result.data !== 'canceled') {
          this.contentNodeService.publish(content.id, true, this.user.id).subscribe(
            response => {
              this.translate.get("SAVE_SUCCESS").subscribe(trad => {
                this.loggerService.success(trad);
                this.init();
              })

            },
            error => {
              this.translate.get("SAVE_ERROR").subscribe(trad1 => {
                this.translate.get("CHANGE_PROJECT_CODE_MESSAGE").subscribe(trad2 => {
                  this.loggerService.error(trad1 + ",  " + trad2);
                })
              })
            });
        }
      });
  }

  delete(node: Node) {

    this.dialogRefDelete = this.dialog.open(ValidationDialogComponent, {
      data: {
        title: "DELETE_CONTENT_NODE_TITLE",
        message: "DELETE_CONTENT_NODE_MESSAGE"
      },
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    this.dialogRefDelete.afterClosed()
      .subscribe(result => {
        if (result && result.data !== 'canceled') {
          this.contentNodeService.delete(node.code, this.user.id).subscribe(
            response => {
              this.translate.get("DELETE_SUCCESS").subscribe(trad => {
                this.loggerService.success(trad);
                this.init();
              })

            },
            error => {
              this.translate.get("DELETE_ERROR").subscribe(trad => {
                this.loggerService.error(trad);
              })
            });
        }
      });
  }


  update(content: ContentNode) {
    this.dialogRefCode = this.dialog.open(ContentCodeComponent, {
      data: {
        node: this.node,
        contentNode: content,
        type: this.type,
        user: this.user
      },
      height: '100%',
      width: '90vw',
      disableClose: true
    });
    this.dialogRefCode.afterClosed()
      .subscribe((data:any) => {
        if(data.refresh) {
          this.init();
        }
      });
  }

  private createNewContent() {
    this.currentContent = new ContentNode();
    this.currentContent.type = this.type;
    this.currentContent.snapshot = true;
    this.currentContent.parentCode = this.node.code;
    if (this.currentContent && !this.currentContent.id) {
      this.currentContent.code = this.type.replace(/[\W_]+/g, "_").toUpperCase() + '-'
        + (this.node.parentCode ? this.node.parentCode.split("-")[0] + '-' : this.node.code.split("-")[0] + '-')
        + (new Date()).getTime();
    }
    this.update(this.currentContent);
  }

  addUrl() {
    if (this.type === 'URLS') {
      if (!this.currentContent.urls)
        this.currentContent.urls = [];

      let url: ContentUrl = new ContentUrl();
      url.url = "";
      url.type = "";
      url.description = "";
      this.currentContent.urls.push(url);
    }
  }


  rules(content: ContentNode) {

    this.dialogRefRules = this.dialog.open(NodeRulesConditionsDialogComponent, {
      data: content,
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    this.dialogRefRules.afterClosed()
      .subscribe(result => {
        this.save(result.data);
      });
  }

  values(content: ContentNode) {
    this.dialogRefValues = this.dialog.open(ValuesDialogComponent, {
      data: content,
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    this.dialogRefValues.afterClosed()
      .subscribe(result => {
        this.save(result.data);
      });
  }

  roles(content: ContentNode) {
    this.dialogRefAccessRoles = this.dialog.open(NodeAccessRolesDialogComponent, {
      data: content,
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    this.dialogRefAccessRoles.afterClosed()
      .subscribe(result => {
        this.save(result.data);
      });
  }

  private save(content: ContentNode) {

    if (this.content) {
      this.content.modifiedBy = this.user.id;
    }

    content.modifiedBy = this.user.id;
    content.parentCode = this.node.code;
    content.parentCodeOrigin = this.node.parentCodeOrigin;

    this.contentNodeService.save(content).subscribe(
      (response: any) => {

        this.currentContent = response;
        this.translate.get("SAVE_SUCCESS").subscribe(trad => {
          this.loggerService.success(trad);
          this.init();
        })

      },
      error => {
        this.translate.get("SAVE_ERROR").subscribe(trad1 => {
          this.translate.get("CHANGE_PROJECT_CODE_MESSAGE").subscribe(trad2 => {
            this.loggerService.error(trad1 + ",  " + trad2);
          })
        })
      });
  }

  getPublishedIcon(element: any) {
    if (element.publicationStatus === 'PUBLISHED')
      return "primary";
    else if (element.publicationStatus === 'SNAPSHOT')
      return "warn";
    else
      return "danger";
  }


  gotoPublished(content: ContentNode) {
    this.dialogRefPublished = this.dialog.open(PublishedContentsNodesDialogComponent, {
      data: content,
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    this.dialogRefPublished.afterClosed()
      .subscribe(result => {
        if (result) {
          this.init();
        }
      });
  }

  translations(contentNode: ContentNode) {
    this.dialogRefTranslations = this.dialog.open(TranslationsDialogComponent, {
      data: contentNode,
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    this.dialogRefTranslations.afterClosed()
      .subscribe(result => {
        if (result) {
          this.save(contentNode);
        }
      });
  }

  deleteUrl(url: ContentUrl) {

    if (url) {
      this.currentContent.urls = this.currentContent.urls.filter((v: ContentUrl) => (v.url !== url.url && v.type !== url.type && v.description !== url.description));
    }
  }

  setUserName(param: any) {
    this.userService.setUserName(param);
  }

  export(element: ContentNode, environmentCode: string) {

    this.contentNodeService.export(element.code, environmentCode).subscribe((data: any) => {
      let blob: Blob = new Blob([JSON.stringify(data)], {type: 'application/json'});

      const a = document.createElement('a');
      const objectUrl = URL.createObjectURL(blob);
      a.href = objectUrl;
      let fileName: string;
      fileName = element.code + (environmentCode && environmentCode.length > 0 ? +"_" + environmentCode : "") + ".json";
      a.download = fileName;
      a.click();
      URL.revokeObjectURL(objectUrl);
    });
  }


  import(fileList: any) {
    if (fileList.files.length < 1) {
      return;
    }

    let file: File = fileList.files[0];
    const reader: any = new FileReader()
    reader.readAsText(file, "UTF-8");
    let content: any;
    let service: any = this.contentNodeService;
    let callback = this;
    reader.onload = ((evt: any) => {
      content = evt.target.result;
      service.import(this.node.code, JSON.parse(content)).subscribe((data: any) => {
        this.translate.get("IMPORT_SUCCESS").subscribe((translation: string) => {
          this.toast.success(translation);
          callback.init();
        });
      });
    }).bind(this);
  }

  view(element: ContentNode) {
    window.open(Env.EXPERT_CONTENT_API_URL + "/contents/code/"
      + element.code
      + ((element.type && (element.type === 'PICTURE' || element.type === 'FILE')) ? '/file?status=' + StatusEnum.SNAPSHOT : '?payloadOnly=true&status=' + StatusEnum.SNAPSHOT), element.code);
  }


  deleteds() {
    this.dialogRefDeleteds = this.dialog.open(DeletedContentsNodesDialogComponent, {
        height: '80vh',
        width: '80vw',
        disableClose: true
      }
    );
    this.dialogRefDeleteds.afterClosed()
      .subscribe();
  }

  deploy(element: any, environmentCode: string) {
    this.contentNodeService.deploy(element.code, environmentCode).subscribe((data: any) => {
      this.translate.get("DEPLOY_SUCCESS").subscribe((translation: string) => {
        this.toast.success(translation);
      });
    });
  }

  getEnvironments() {
    try {
      return this.environments.filter((env: Node) =>
        this.node.parentCodeOrigin ? env.code !== this.node.parentCodeOrigin : env.code !== this.node.code
      );
    } catch (error: any) {
      return null;
    }
  }

  close() {
    this.currentContent = null as any;
  }

  datas(contentNode: ContentNode) {
    this.dialogRefDatas = this.dialog.open(ContentDatasComponent, {
      data: contentNode,
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    this.dialogRefDatas.afterClosed()
      .subscribe(result => {
        this.init();
      });
  }

  haveDatas(code: string) {
    if (this.mapDatas.has(code)) {
      return this.mapDatas.get(code);
    } else {
      return this.dataService.countDatasByContentNodeCode(code).subscribe(
        (count: any) => {
          this.mapDatas.set(code, count > 0);
          return this.mapDatas.get(code);
        });
    }
  }

  favorite(element:ContentNode) {
    element.favorite = !element.favorite;
    this.save(element);
  }
}
